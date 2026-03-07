package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * CLI entry point for the key-value store.
 *
 * <p>Reads commands from STDIN, one per line:
 * <ul>
 *   <li>{@code SET <key> <value>} – persist key to value and update in-memory index</li>
 *   <li>{@code GET <key>} – print the latest value for key, or a blank line if absent</li>
 *   <li>{@code EXIT} – flush and exit</li>
 * </ul>
 * Results are written to STDOUT.
 */
public class Main {

    /** Command tokens (compared case-insensitively). */
    private static final String CMD_SET  = "SET";
    private static final String CMD_GET  = "GET";
    private static final String CMD_EXIT = "EXIT";

    public static void main(String[] args) {
        KeyValueIndex index = new KeyValueIndex();
        Storage storage = new Storage();

        // Replay the append-only log to rebuild the in-memory index on startup.
        try {
            storage.replay(index);
        } catch (IOException e) {
            System.err.println("Warning: could not load data.db – " + e.getMessage());
        }

        BufferedReader in  = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter   out  = new PrintWriter(System.out, true);

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = clean(line);
                if (line.isEmpty()) {
                    continue;
                }

                String upper = line.toUpperCase();

                if (upper.equals(CMD_EXIT)) {
                    break;
                }

                if (upper.startsWith(CMD_SET + " ")) {
                    handleSet(line.substring(CMD_SET.length() + 1).trim(),
                              index, storage, out);
                    continue;
                }

                if (upper.startsWith(CMD_GET + " ")) {
                    handleGet(clean(line.substring(CMD_GET.length() + 1)),
                              storage, index, out);
                    continue;
                }
                // Unknown command: produce no output so GET response lines stay aligned.
            }
        } catch (IOException e) {
            System.err.println("Fatal read error: " + e.getMessage());
        } finally {
            storage.close();
        }
    }

    /**
     * Handle a SET command.
     *
     * @param rest    everything after "SET " on the input line
     * @param index   in-memory index to update
     * @param storage persistent storage to append to
     * @param out     STDOUT writer (used for error responses)
     */
    private static void handleSet(String rest, KeyValueIndex index,
                                   Storage storage, PrintWriter out) {
        int sp = rest.indexOf(' ');
        String key   = sp < 0 ? rest.trim()                    : rest.substring(0, sp).trim();
        String value = sp < 0 ? ""                             : rest.substring(sp + 1).trim();

        index.set(key, value);
        try {
            storage.appendSet(key, value);
        } catch (IOException e) {
            System.err.println("Error persisting SET – " + e.getMessage());
        }
    }

    /**
     * Handle a GET command.
     *
     * <p>Reads the latest value from disk so writes by other concurrent processes
     * are always visible. Falls back to the in-memory index if the file read fails.
     *
     * @param key     the key to look up
     * @param storage persistent storage (used for authoritative on-disk lookup)
     * @param index   in-memory index (fallback)
     * @param out     STDOUT writer for the response line
     */
    private static void handleGet(String key, Storage storage,
                                   KeyValueIndex index, PrintWriter out) {
        String val = null;

        // Authoritative: scan data.db for the last written value.
        try {
            val = storage.getLatest(key);
        } catch (IOException e) {
            System.err.println("Warning: file read error for GET, using in-memory index – "
                    + e.getMessage());
        }

        // Fallback: use the in-memory index if the file scan found nothing.
        if (val == null) {
            val = index.get(key);
        }

        out.println(val != null ? val : "");
        out.flush();
    }

    /**
     * Strip carriage-return characters, BOM, and leading/trailing whitespace.
     *
     * @param s raw input string
     * @return cleaned string
     */
    private static String clean(String s) {
        return s.replace("\r", "").replace("\uFEFF", "").trim();
    }
}
