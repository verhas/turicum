package javax0.turicum.builtins.classes;

import javax0.turicum.TuriClass;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.LngCallable;

public class TuriString implements TuriClass {

    @Override
    public Class<?> forClass() {
        return String.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        return switch (identifier) {
            case "times" -> new TuriMethodCallBuilder(target, (obj, args) -> obj.toString().repeat(Cast.toLong(args[0]).intValue()));
            case "charAt" -> new TuriMethodCallBuilder(target, (obj, args) -> obj.toString().charAt(Cast.toLong(args[0]).intValue()));
            case "indexOf" -> new TuriMethodCallBuilder(target, (obj, args) -> obj.toString().indexOf(args[0].toString()));
            default -> null;
        };
    }

}
