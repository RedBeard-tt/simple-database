package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * CLI entry point for the key-value store.
 * Accepts SET, GET, and EXIT commands on STDIN and writes results to STDOUT.
 */
public class Main {

    public static void main(String[] args) {
        KeyValueIndex index = new KeyValueIndex();
        Storage storage = new Storage();

        // Rebuild in-memory index from the append-only log on disk.
        try {
            storage.replay(index);
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(System.out, true);

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = clean(line);
                if (line.isEmpty()) {
                    continue;
                }
                String upper = line.toUpperCase();

                if (upper.equals("EXIT")) {
                    break;
                }

                if (upper.startsWith("SET ")) {
                    handleSet(line.substring(4).trim(), index, storage, out);
                    continue;
                }

                if (upper.startsWith("GET ")) {
                    handleGet(clean(line.substring(4)), storage, index, out);
                    continue;
                }
                // Unknown command: produce no output so GET response lines stay in sync.
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                storage.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void handleSet(String rest, KeyValueIndex index,
                                   Storage storage, PrintWriter out) {
        int sp = rest.indexOf(' ');
        String key, value;
        if (sp < 0) {
            key = rest.trim();
            value = "";
        } else {
            key = rest.substring(0, sp).trim();
            value = rest.substring(sp + 1).trim();
        }
        index.set(key, value);
        try {
            storage.appendSet(key, value);
        } catch (IOException e) {
            out.println("ERROR: " + e.getMessage());
        }
    }

    private static void handleGet(String key, Storage storage,
                                   KeyValueIndex index, PrintWriter out) {
        String val = null;

        // Always read from disk first so we see writes from other processes.
        try {
            val = storage.getLatest(key);
        } catch (IOException ignored) {
            // Fall back to in-memory index on file error.
        }

        // If the file scan missed it, try the in-memory index.
        if (val == null) {
            val = index.get(key);
        }

        if (val != null) {
            out.println(val);
        } else {
            out.println();
        }
        out.flush();
    }

    /** Strip carriage returns, BOM, and surrounding whitespace. */
    private static String clean(String s) {
        return s.replace("\r", "").replace("\uFEFF", "").trim();
    }
}
