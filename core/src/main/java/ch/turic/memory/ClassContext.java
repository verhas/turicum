package ch.turic.memory;

import java.util.ArrayList;
import java.util.List;

public class ClassContext extends Context {
    private final LngClass[] parents;

    public ClassContext(Context context, LngClass[] parents) {
        super(context.globalContext, context.threadContext);
        this.parents = parents;
    }

    @Override
    public Object get(String key) {
        final var value = super.getLocal(key);
        if (value != null) return value;
        for (final var parent : parents) {
            final var inherited = parent.context().get(key);
            if (inherited != null) {
                return inherited;
            }
        }
        final var variable = globalContext.heap.get(key);
        if (variable != null) {
            return variable.get();
        } else {
            return null;
        }
    }

    /**
     * Retrieves a list of wrapping contexts, including those from the current context 
     * and recursively from the contexts of the parent classes.
     *
     * The method uses recursion instead of a simple loop because parent contexts can be 
     * different implementations of Context (like ClassContext), each with their own 
     * wrapping behavior. A loop would not correctly handle these varying context types 
     * and their specific implementations of wrappingContexts().
     *
     * @return a list of {@link Context} instances, comprising the wrapping contexts 
     *         of this instance and all parent contexts.
     */
    public List<Context> wrappingContexts() {
        final var ctxList = new ArrayList<>(super.wrappingContexts());
        for (final var parent : parents) {
            ctxList.addAll(parent.context().wrappingContexts());
        }
        return ctxList;
    }

    @Override
    public boolean contains(String key) {
        final var thisContains = super.contains(key);
        if (thisContains) return true;
        for (final var parent : parents) {
            if (parent.context().contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsLocal(String key) {
        if (super.containsLocal(key)) {
            return true;
        }
        for (final var parent : parents) {
            if (parent.context().containsLocal(key)) {
                return true;
            }
        }
        return false;
    }

    public LngClass[] parents() {
        return parents;
    }
}
