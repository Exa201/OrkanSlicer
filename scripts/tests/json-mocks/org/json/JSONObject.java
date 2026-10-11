package org.json;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal ordered JSON object model for deterministic profile-import regression cases. */
public class JSONObject {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JSONObject() { }

    public JSONObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public JSONObject put(String key, double value) { return put(key, Double.valueOf(value)); }
    public JSONObject put(String key, int value) { return put(key, Integer.valueOf(value)); }
    public JSONObject put(String key, long value) { return put(key, Long.valueOf(value)); }
    public JSONObject put(String key, boolean value) { return put(key, Boolean.valueOf(value)); }

    public String optString(String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    public String optString(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : value.toString();
    }

    public String getString(String key) throws JSONException {
        if (!values.containsKey(key) || values.get(key) == null) throw new JSONException("Missing string: " + key);
        return values.get(key).toString();
    }

    public Object get(String key) throws JSONException {
        if (!values.containsKey(key)) throw new JSONException("Missing value: " + key);
        return values.get(key);
    }

    public Iterator<String> keys() { return values.keySet().iterator(); }
}
