package ch.turic.builtins.classes;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.LngCallable;

import java.util.function.Supplier;

public class TuriMethod<T> implements LngCallable.LngCallableClosure {
    private final T target;
    private final BiFunction<T, Object[], T> method;

    public interface BiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    public interface Function<T, R> {
        R apply(T t) throws Exception;
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
        try {
            return method.apply(target, arguments);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }
}
