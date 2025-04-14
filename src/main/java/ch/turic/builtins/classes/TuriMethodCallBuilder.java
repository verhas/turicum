package ch.turic.builtins.classes;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.LngCallable;

import java.util.function.BiFunction;

public record TuriMethodCallBuilder<T>(T target, BiFunction<T, Object[], Object> method) implements LngCallable.LngCallableClosure {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        return method.apply(target, arguments);
    }
}
