package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;

import java.util.concurrent.CompletableFuture;

public class TuriFuture implements TuriClass {
    @Override
    public Class<?> forClass() {
        return CompletableFuture.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if (!(target instanceof CompletableFuture<?> future)) {
            throw new ExecutionException("Target must be of type CompletableFuture, this is an internal error");
        }
        return switch (identifier) {
            case "is_done" -> new TuriMethodCallBuilder<>(future, (fut, args) -> fut.isDone());
            case "is_cancelled" -> new TuriMethodCallBuilder<>(future, (fut, args) -> fut.isCancelled());
            case "get" -> new TuriMethodCallBuilder<>(future, (fut, args) -> {
                try {
                    return fut.get();
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException("Execution exception while waiting for thread %s", e.getMessage());
                }
            });
            default -> null;
        };
    }
}
