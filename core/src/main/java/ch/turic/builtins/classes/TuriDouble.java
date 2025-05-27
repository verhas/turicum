package ch.turic.builtins.classes;

import ch.turic.TuriClass;
import ch.turic.LngCallable;

import java.util.Objects;

public class TuriDouble implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Double.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        return switch (identifier) {
            case "to_string" -> new TuriMethod<>((args)-> String.format("%s", Objects.requireNonNullElse(target,"none")));
            default -> null;
        };
    }
}
