package kvstore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Append-only persistence to data.db.
 * Replays log on startup to rebuild the in-memory index.
 * For GET, also provides a direct file scan so the latest value is always returned
 * even if another process appended to data.db after this process started.
 */
public class Storage {

    private static final String DATA_FILE = "data.db";
    private static final String SET_CMD = "SET";

    private final Path path;
    private BufferedWriter writer;

    public Storage() {
        this.path = Paths.get(DATA_FILE);
    }

    /**
     * Read all SET entries from data.db and apply them to the index.
     * Called once at startup to rebuild in-memory state.
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
     * Scan data.db for the last value written for the given key.
     * Reads the file on every call so it always reflects the latest on-disk state,
     * even if other processes have appended entries since startup.
     *
     * @return the most recently written value, or null if the key has never been set
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
                if (line.startsWith(SET_CMD + " ")) {
                    String rest = line.substring(SET_CMD.length() + 1).trim();
                    int sp = rest.indexOf(' ');
                    String k = sp < 0 ? rest.trim() : rest.substring(0, sp).trim();
                    String v = sp < 0 ? "" : rest.substring(sp + 1).trim();
                    if (k.equals(key)) {
                        result = v;
                    }
                }
            }
        }
        return result;
    }

    /** Append a SET entry to data.db and flush immediately for durability. */
    public void appendSet(String key, String value) throws IOException {
        if (writer == null) {
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        writer.write(SET_CMD + " " + key + " " + value);
        writer.newLine();
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    // ---- helpers ----

    private void parseLine(String raw, KeyValueIndex index) {
        String line = clean(raw);
        if (!line.startsWith(SET_CMD + " ")) {
            return;
        }
        String rest = line.substring(SET_CMD.length() + 1).trim();
        int sp = rest.indexOf(' ');
        String key = sp < 0 ? rest.trim() : rest.substring(0, sp).trim();
        String value = sp < 0 ? "" : rest.substring(sp + 1).trim();
        index.set(key, value);
    }

    /** Strip carriage returns and BOM, then trim whitespace. */
    private String clean(String s) {
        return s.replace("\r", "").replace("\uFEFF", "").trim();
    }
}
