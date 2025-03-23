package javax0.genai.pl.memory;

public class ClassContext extends Context {
    private final LngClass[] parents;

    public ClassContext(LngClass[] parents) {
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
        return null;
    }

    @Override
    public boolean contains(String key) {
        final var thisContains = super.contains(key);
        if (thisContains ) return true;
        for (final var parent : parents) {
            final var inheritedContains = parent.context().contains(key);
            if (inheritedContains ) {
                return inheritedContains;
            }
        }
        return false;
    }

    public LngClass[] parents() {
        return parents;
    }
}
