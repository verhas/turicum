package ch.turic;


public interface LngCallable {
    @FunctionalInterface
    interface LngCallableClosure extends LngCallable {
    }

    @FunctionalInterface
    interface LngCallableMacro extends LngCallable {
    }

    Object call(Context ctx, Object[] args) throws ExecutionException;
}
