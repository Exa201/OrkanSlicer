package android.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** Minimal sorted JVM test double for Android's SparseArray. */
public class SparseArray<E> {
    private final TreeMap<Integer, E> values = new TreeMap<>();

    public SparseArray() {}

    public SparseArray(int initialCapacity) {}

    public E get(int key) {
        return values.get(key);
    }

    public E get(int key, E defaultValue) {
        E value = values.get(key);
        return value != null ? value : defaultValue;
    }

    public void put(int key, E value) {
        values.put(key, value);
    }

    public void append(int key, E value) {
        values.put(key, value);
    }

    public void remove(int key) {
        values.remove(key);
    }

    public void delete(int key) {
        values.remove(key);
    }

    public int size() {
        return values.size();
    }

    public int keyAt(int index) {
        return new ArrayList<>(values.keySet()).get(index);
    }

    public E valueAt(int index) {
        return values.get(keyAt(index));
    }

    public void setValueAt(int index, E value) {
        values.put(keyAt(index), value);
    }

    public int indexOfKey(int key) {
        int index = 0;
        for (Integer candidate : values.keySet()) {
            if (candidate == key) return index;
            index++;
        }
        return -1;
    }

    public int indexOfValue(E value) {
        int index = 0;
        for (Map.Entry<Integer, E> entry : values.entrySet()) {
            if (entry.getValue() == value || (value != null && value.equals(entry.getValue()))) return index;
            index++;
        }
        return -1;
    }

    public void clear() {
        values.clear();
    }
}
