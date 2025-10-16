package ch.turic.builtins.classes;

import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriClass;
import ch.turic.commands.operators.Cast;
import ch.turic.LngCallable;

public class TuriLong implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Long.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if( !(target instanceof Long num)){
            throw new ExecutionException("Wrong target type, need Long, probably internal error");
        }
        return switch (identifier) {
            case "to_string" -> new TuriMethod<>((args)-> String.format("%s",num));
            case "times" -> new TuriMethod<>((args) -> times(num, args[0]));
            case "hex" -> new TuriMethod<>((args) -> String.format("0x%X", num));
            default -> null;
        };
    }

    private Object times(Long num, Object arg) {
        if (Cast.isLong(arg)) {
            return Cast.toLong(arg) * Cast.toLong(num);
        }
        if (Cast.isDouble(arg)) {
            return Cast.toDouble(arg) * Cast.toDouble(num);
        }
        return arg.toString().repeat(Cast.toLong(num).intValue());
    }
}
