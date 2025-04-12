package ch.turic;


public interface LngCallable {
    Object call(Context ctx, Object[] arguments)throws ExecutionException;
}
