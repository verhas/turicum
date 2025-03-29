package javax0.turicum.builtins.classes;

import javax0.turicum.TuriClass;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.LngCallable;

public class TuriLong implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Long.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        return switch (identifier) {
            case "times" ->
                    new TuriMethodCallBuilder(target, (obj, args) -> times(obj,args[0]));
            default -> null;
        };
    }

    private Object times(Object target, Object arg) {
        if (Cast.isLong(arg)) {
            return Cast.toLong(arg) * Cast.toLong(target);
        }
        if (Cast.isDouble(arg)) {
            return Cast.toDouble(arg) * Cast.toDouble(target);
        }
        return arg.toString().repeat(Cast.toLong(target).intValue());
    }
}
