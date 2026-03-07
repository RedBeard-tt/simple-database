package kvstore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
// BufferedWriter used locally inside appendSet via try-with-resources
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Append-only file storage backed by {@code data.db}.
 *
 * <p>Each write appends one {@code SET key value} line and flushes immediately
 * so data survives an unexpected process exit. On startup the log is replayed
 * in order to rebuild the in-memory index (last-write-wins).
 *
 * <p>The {@link #getLatest(String)} method always reads from disk, which means
 * writes made by other processes after this one started are visible to GETs.
 */
public class Storage {

    /** Name of the append-only log file. */
    private static final String DATA_FILE  = "data.db";

    /** Command token written at the start of every log entry. */
    private static final String SET_CMD    = "SET";

    /** Pre-computed prefix used when matching log lines. */
    private static final String SET_PREFIX = SET_CMD + " ";

    private final Path path;

    /** Constructs a Storage backed by {@value #DATA_FILE} in the working directory. */
    public Storage() {
        this.path = Paths.get(DATA_FILE);
    }

    /**
     * Replay all SET entries from {@code data.db} and apply them to {@code index}.
     * Called once at startup; later writes by other processes are not included here
     * but are picked up by {@link #getLatest(String)}.
     *
     * @param index the in-memory index to populate
     * @throws IOException if the file cannot be read
     */
    public void replay(KeyValueIndex index) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                parseLine(line, index);
            }
        }
    }

    /**
     * Scan {@code data.db} for the most recently written value for {@code key}.
     *
     * <p>Re-reads the file on every invocation so entries appended by other
     * processes after startup are always visible.
     *
     * @param key the key to look up
     * @return the last value written for {@code key}, or {@code null} if none exists
     * @throws IOException if the file cannot be read
     */
    public String getLatest(String key) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        String result = null;
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = clean(line);
                if (!line.startsWith(SET_PREFIX)) {
                    continue;
                }
                String rest = line.substring(SET_PREFIX.length()).trim();
                int    sp   = rest.indexOf(' ');
                String k    = sp < 0 ? rest.trim()                  : rest.substring(0, sp).trim();
                String v    = sp < 0 ? ""                           : rest.substring(sp + 1).trim();
                if (k.equals(key)) {
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * Append a {@code SET key value} entry to {@code data.db} and flush immediately.
     *
     * <p>The writer is opened in {@code CREATE | APPEND} mode so a new file is
     * created when data.db does not yet exist, and existing content is never
     * overwritten. The writer is closed after each append to ensure all bytes
     * are visible to other processes and no file descriptor is left dangling.
     *
     * @param key   the key to store (must not be {@code null} or contain newlines)
     * @param value the value to associate with {@code key}
     * @throws IOException if opening, writing, or closing the file fails
     */
    public void appendSet(String key, String value) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(SET_PREFIX + key + " " + value);
            w.newLine();
        }
    }

    /**
     * No-op: retained for API compatibility. The writer is now opened and closed
     * within each {@link #appendSet} call, so there is no long-lived resource to
     * release here.
     */
    public void close() {
        // Writer is managed locally inside appendSet; nothing to close here.
    }

    // ---- private helpers ----

    /**
     * Parse one line from the log and apply it to {@code index} if it is a SET entry.
     *
     * @param raw   raw line from the file (may contain CR or BOM)
     * @param index in-memory index to update
     */
    private void parseLine(String raw, KeyValueIndex index) {
        String line = clean(raw);
        if (!line.startsWith(SET_PREFIX)) {
            return;
        }
        String rest  = line.substring(SET_PREFIX.length()).trim();
        int    sp    = rest.indexOf(' ');
        String key   = sp < 0 ? rest.trim()                  : rest.substring(0, sp).trim();
        String value = sp < 0 ? ""                           : rest.substring(sp + 1).trim();
        index.set(key, value);
    }

    /**
     * Strip carriage-return characters and BOM, then trim whitespace.
     *
     * @param s raw string
     * @return cleaned string
     */
    private String clean(String s) {
        return s.replace("\r", "").replace("\uFEFF", "").trim();
    }
}
