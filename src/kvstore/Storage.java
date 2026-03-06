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
 * Append-only persistence to data.db. Replays log on load to rebuild index.
 */
public class Storage {

    private static final String DATA_FILE = "data.db";
    private static final String SET_PREFIX = "SET ";

    private final Path path;
    private BufferedWriter writer;

    public Storage() {
        this.path = Paths.get(DATA_FILE);
    }

    /**
     * Load all SET entries from data.db and apply them to the index (last-write-wins).
     */
    public void replay(KeyValueIndex index) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.replace("\r", "").trim();
                if (line.startsWith(SET_PREFIX)) {
                    String rest = line.substring(SET_PREFIX.length()).trim();
                    int firstSpace = rest.indexOf(' ');
                    String key, value;
                    if (firstSpace < 0) {
                        key = rest.trim();
                        value = "";
                    } else {
                        key = rest.substring(0, firstSpace).trim();
                        value = rest.substring(firstSpace + 1).trim();
                    }
                    index.set(key, value);
                }
            }
        }
    }

    /** Append a SET line to data.db and flush immediately for durability. */
    public void appendSet(String key, String value) throws IOException {
        if (writer == null) {
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        writer.write(SET_PREFIX + key + " " + value);
        writer.newLine();
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
