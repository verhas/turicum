package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;

import java.util.Iterator;

public class TuriIterator implements TuriClass {
    @Override
    public Class<?> forClass() {
        return Iterator.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if (!(target instanceof Iterator<?> iterator)) {
            throw new ExecutionException("Target must be of type CompletableFuture, this is an internal error");
        }
        return switch (identifier) {
            case "has_next" -> new TuriMethodCallBuilder<>(iterator, (it, args) -> it.hasNext());
            case "next" -> new TuriMethodCallBuilder<>(iterator, (fut, args) -> fut.next());
            default -> null;
        };
    }
}
