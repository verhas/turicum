package javax0.turicum.builtins.classes;

import javax0.turicum.TuriClass;
import javax0.turicum.LngCallable;

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
