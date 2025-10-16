package ch.turic.testjar;

import ch.turic.exceptions.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;
import ch.turic.builtins.classes.TuriMethod;

public class TuriShort implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Short.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) throws ExecutionException {
        if( !(target instanceof Short num)){
            throw new ExecutionException("Wrong target type, need Long, probably internal error");
        }
        return switch (identifier) {
            case "to_string" -> new TuriMethod<>((args)-> String.format("%s",num));
            case "hex" -> new TuriMethod<>((args) -> String.format("0x%X", num));
            default -> null;
        };
    }
}
