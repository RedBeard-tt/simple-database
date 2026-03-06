package kvstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * CLI for the key-value store: SET, GET, EXIT via STDIN/STDOUT.
 */
public class Main {

    public static void main(String[] args) {
        KeyValueIndex index = new KeyValueIndex();
        Storage storage = new Storage();
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
                line = line.replace("\r", "").replace("\uFEFF", "").trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("EXIT")) {
                    break;
                }
                if (line.startsWith("SET ")) {
                    String rest = line.substring(4).trim();
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
                    try {
                        storage.appendSet(key, value);
                    } catch (IOException e) {
                        out.println("ERROR: " + e.getMessage());
                    }
                    continue;
                }
                if (line.startsWith("GET ")) {
                    String key = line.substring(4).trim();
                    String val = index.get(key);
                    if (val != null) {
                        out.println(val);
                    } else {
                        out.println();
                    }
                    continue;
                }
                // Unknown command - produce no output so GET response lines stay in sync
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
}
