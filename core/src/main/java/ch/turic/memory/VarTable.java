package ch.turic.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class VarTable {
    private Map<String, Variable> map = new HashMap<>();
    public final AtomicBoolean isMultiThreading = new AtomicBoolean(false);
    private final boolean volatileVariables;

    /**
     * Creates a variable table holding plain {@link Variable}s. This is the table used for the
     * local frames, which are thread confined.
     */
    public VarTable() {
        this(false);
    }

    /**
     * Creates a variable table.
     *
     * @param volatileVariables when {@code true} the table creates {@link VolatileVariable}s,
     *                          and plain variables {@link #put(String, Variable) put} into the
     *                          table are converted. The global heap uses this mode, because
     *                          globals can be written and read by different threads without any
     *                          intervening synchronization point.
     */
    public VarTable(final boolean volatileVariables) {
        this.volatileVariables = volatileVariables;
    }

    public Variable get(final String name) {
        return map.get(name);
    }

    public void set(final String name, Object value){
        map.computeIfAbsent(name, this::newVariable).set(value);
    }

    /**
     * Creates a new variable of the kind matching this table, stores and returns it.
     * The caller may set the types and the value on the returned instance.
     *
     * @param name the name of the variable
     * @return the newly created variable, already stored in the table
     */
    public Variable define(final String name) {
        final var v = newVariable(name);
        map.put(name, v);
        return v;
    }

    private Variable newVariable(final String name) {
        return volatileVariables ? new VolatileVariable(name) : new Variable(name);
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
        return map.put(name, adapt(value));
    }

    /**
     * Converts a plain variable to a {@link VolatileVariable} when this table requires volatile
     * variables (e.g. importing a module's variables into the global heap). The raw
     * {@link Variable#assign(Object)} is used for the copy: the value was already validated
     * against the types when it was set on the source variable, and a not-yet-initialized
     * typed variable must not be re-validated against {@code null}.
     *
     * @param variable the variable to store
     * @return the variable itself, or its volatile copy
     */
    private Variable adapt(final Variable variable) {
        if (!volatileVariables || variable instanceof VolatileVariable) {
            return variable;
        }
        final var vv = new VolatileVariable(variable.name);
        vv.types = variable.types;
        vv.assign(variable.get());
        return vv;
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
