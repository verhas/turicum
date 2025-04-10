package javax0.turicum.builtins.classes;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;

import java.util.function.BiFunction;

public record TuriMethodCallBuilder(Object target, BiFunction<Object, Object[], Object> method) implements LngCallable {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        return method.apply(target, arguments);
    }
}
