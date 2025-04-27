package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.TuriClass;
import ch.turic.commands.operators.Cast;
import ch.turic.LngCallable;

public class TuriString implements TuriClass {
    @Override
    public Class<?> forClass() {
        return String.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if( !(target instanceof String string)){
            throw new ExecutionException("Target object is not a String, this is an internal error");
        }

        return switch (identifier) {
            case "times" -> new TuriMethod<>((args) -> string.repeat(Cast.toLong(args[0]).intValue()));
            case "charAt" -> new TuriMethod<>( (args) -> ""+string.charAt(Cast.toLong(args[0]).intValue()));
            case "indexOf" -> new TuriMethod<>( ( args) -> (long) string.indexOf(args[0].toString()));
            default -> null;
        };
    }
}
