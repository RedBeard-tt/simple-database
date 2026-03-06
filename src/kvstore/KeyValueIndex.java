package kvstore;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory key-value index implemented without built-in Map.
 * Uses a list of entries with linear scan. Last-write-wins semantics.
 */
public class KeyValueIndex {

    /** Single key-value entry. */
    public static final class Entry {
        private final String key;
        private String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    private final List<Entry> entries = new ArrayList<>();

    /** Set key to value (last-write-wins). */
    public void set(String key, String value) {
        for (Entry e : entries) {
            if (e.getKey().equals(key)) {
                e.setValue(value);
                return;
            }
        }
        entries.add(new Entry(key, value));
    }

    /** Return value for key, or null if not found. */
    public String get(String key) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getKey().equals(key)) {
                return entries.get(i).getValue();
            }
        }
        return null;
    }
}
