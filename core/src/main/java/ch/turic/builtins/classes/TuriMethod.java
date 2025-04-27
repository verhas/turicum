package ch.turic.builtins.classes;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.LngCallable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class TuriMethod<T> implements LngCallable.LngCallableClosure {
    private final T target;
    private final BiFunction<T, Object[], T> method;

    public T target() {
        return target;
    }

    public BiFunction<T, Object[], T> method() {
        return method;
    }

    public TuriMethod(T target, BiFunction<T, Object[], T> method) {
        this.target = target;
        this.method = method;
    }

    public TuriMethod(Function<Object[], T> method) {
        this(null, (T target, Object[] args) -> method.apply(args));
    }

    public TuriMethod(Supplier<T> method) {
        this(null, (T target, Object[] args) -> {
                    if (args != null && args.length > 0) {
                        throw new ExecutionException("Too many arguments");
                    }
                    return method.get();
                }
        );
    }

    public static <T> TuriMethod<T> of(Supplier<T> method) {
        return new TuriMethod<>(method);
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        return method.apply(target, arguments);
    }
}
