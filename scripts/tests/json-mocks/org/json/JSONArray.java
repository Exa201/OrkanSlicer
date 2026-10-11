package org.json;

import java.util.ArrayList;
import java.util.List;

/** Minimal JSON array model used only by the headless Java/Kotlin interop contract test. */
public class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray put(Object value) {
        values.add(value);
        return this;
    }

    public JSONArray put(double value) { return put(Double.valueOf(value)); }
    public JSONArray put(int value) { return put(Integer.valueOf(value)); }
    public JSONArray put(long value) { return put(Long.valueOf(value)); }
    public JSONArray put(boolean value) { return put(Boolean.valueOf(value)); }

    public int length() { return values.size(); }

    public String getString(int index) throws JSONException {
        if (index < 0 || index >= values.size()) throw new JSONException("Array index out of bounds: " + index);
        Object value = values.get(index);
        if (value == null) throw new JSONException("Value at " + index + " is null");
        return value.toString();
    }
}
