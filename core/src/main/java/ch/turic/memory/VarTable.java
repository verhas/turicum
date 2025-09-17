package ch.turic.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class VarTable {
    private Map<String, Variable> map = new HashMap<>();
    public final AtomicBoolean isMultiThreading = new AtomicBoolean(false);

    public Variable get(final String name) {
        return map.get(name);
    }

    public void set(final String name, Object value){
        map.computeIfAbsent(name,Variable::new).set(value);
    }

    public Set<Map.Entry<String, Variable>> entrySet() {
        return map.entrySet();
    }

    public boolean containsKey(final String name) {
        return map.containsKey(name);
    }

    public Variable remove(final String name) {
        return map.remove(name);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Variable put(final String name, final Variable value) {
        return map.put(name, value);
    }


    public void parallel() {
        if (isMultiThreading.compareAndSet(false, true)) {
            map = new ConcurrentHashMap<>(map);
        }
    }
}
