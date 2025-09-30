package ch.turic.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Config {
    final Map<String, String> values = new HashMap<>();
    final Map<String, Set<String>> defaults = new HashMap<>();

    public void set(final String key, final String value) {
        values.put(key, value);
        createDefaults(key);
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return values.entrySet();
    }

    public Set<Map.Entry<String, Set<String>>> defaultSet() {
        return defaults.entrySet();
    }

    private void createDefaults(String key) {
        if (!defaults.containsKey(key)) {
            defaults.put(key, new HashSet<>());
        }
    }

    public boolean is(final String key, final String value) {
        createDefaults(key);
        defaults.get(key).add(value);
        return values.containsKey(key) && values.get(key).equals(value);
    }

    public String get(final String key, String defaultValue) {
        createDefaults(key);
        defaults.get(key).add(defaultValue);
        return values.getOrDefault(key, defaultValue);
    }

}
