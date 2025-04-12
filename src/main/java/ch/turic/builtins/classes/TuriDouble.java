package ch.turic.builtins.classes;

import ch.turic.TuriClass;
import ch.turic.LngCallable;

public class TuriDouble implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Double.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        return switch (identifier) {
            default -> null;
        };
    }
}
