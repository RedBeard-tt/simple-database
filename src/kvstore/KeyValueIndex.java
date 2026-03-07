package kvstore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * In-memory key-value index backed by a simple list of entries.
 *
 * <p>Intentionally does not use any built-in {@code Map} or {@code Dictionary}
 * type. All lookups and updates use linear scans over an {@link ArrayList}.
 *
 * <p>Last-write-wins semantics are enforced: each call to {@link #set} removes
 * any existing entry for the key before inserting the new one, so the list
 * contains at most one entry per key at all times.
 */
public class KeyValueIndex {

    /**
     * A single immutable-key, mutable-value entry stored in the index.
     */
    public static final class Entry {

        private final String key;
        private final String value;

        /**
         * @param key   the entry key (never {@code null})
         * @param value the entry value (never {@code null})
         */
        public Entry(String key, String value) {
            this.key   = key;
            this.value = value;
        }

        /** @return the key for this entry */
        public String getKey()   { return key; }

        /** @return the value stored at this entry */
        public String getValue() { return value; }
    }

    /** Ordered list of entries; contains at most one entry per key. */
    private final List<Entry> entries = new ArrayList<>();

    /**
     * Associate {@code key} with {@code value}, replacing any previous mapping.
     *
     * <p>Any existing entry for {@code key} is removed before the new entry is
     * appended, so the most recently written value is always at the end of the
     * list and is found first by {@link #get}.
     *
     * @param key   the key to set (must not be {@code null})
     * @param value the value to associate with {@code key} (must not be {@code null})
     */
    public void set(String key, String value) {
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().getKey().equals(key)) {
                it.remove();
            }
        }
        entries.add(new Entry(key, value));
    }

    /**
     * Return the value associated with {@code key}, or {@code null} if the key
     * has not been set.
     *
     * <p>The list is scanned from the end so the most recent entry is found first,
     * which guarantees last-write-wins even if duplicate entries were somehow
     * introduced (e.g., via a hand-edited log file).
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if absent
     */
    public String get(String key) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getKey().equals(key)) {
                return entries.get(i).getValue();
            }
        }
        return null;
    }
}
