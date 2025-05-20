package ch.turic.memory;

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
