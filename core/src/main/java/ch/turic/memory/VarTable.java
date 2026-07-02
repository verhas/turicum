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


    /**
     * This method is called only from the GlobalContext to switch the VarTable from single-thread to multi-thread.
     * By the time this method is called, there is only one thread in the interpreter calling this method.
     * This ensures that the non-volatile {@code map} field is safely changed and all other threads have a safe
     * happens-before edge because they are started later.
     */
    public void parallel() {
        if (isMultiThreading.compareAndSet(false, true)) {
            map = new ConcurrentHashMap<>(map);
        }
    }
}
