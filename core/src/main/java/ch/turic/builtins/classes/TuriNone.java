package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.NoneType;

public class TuriNone implements TuriClass {
    public static final TuriNone INSTANCE = new TuriNone();
    @Override
    public Class<?> forClass() {
        return NoneType.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        return switch (identifier) {
            case "to_string" -> new TuriMethod<>((args)-> "none");
            default -> null;
        };
    }
}
