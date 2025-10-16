package ch.turic;


import ch.turic.exceptions.ExecutionException;

public interface LngCallable {
    @FunctionalInterface
    interface LngCallableClosure extends LngCallable {
    }

    @FunctionalInterface
    interface LngCallableMacro extends LngCallable {
    }

    Object call(Context context, Object[] arguments) throws ExecutionException;
}
