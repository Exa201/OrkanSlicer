package android.util;

import java.util.TreeMap;

/** Minimal JVM test double for the startup scheduler's Android collection. */
public class SparseBooleanArray {
    private final TreeMap<Integer, Boolean> values = new TreeMap<>();

    public boolean get(int key) {
        Boolean value = values.get(key);
        return value != null && value;
    }

    public void put(int key, boolean value) {
        values.put(key, value);
    }

    public int size() {
        return values.size();
    }
}
