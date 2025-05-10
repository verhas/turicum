package ch.turic;

import ch.turic.memory.LngException;

import java.util.function.Supplier;

public class ExecutionException extends RuntimeException {
    public LngException embedded() {
        return embedded;
    }

    private LngException embedded = null;

    public ExecutionException(String message, Exception cause) {
        super(message, cause);
    }

    public ExecutionException(String s, Object... params) {
        super(String.format(s, params));
    }
    public ExecutionException(Throwable t, String s, Object... params) {
        super(String.format(s, params),t);
    }

    public ExecutionException(Throwable throwable) {
        super(throwable.getMessage(), throwable);
    }

    public ExecutionException(Throwable throwable, LngException embedded) {
        super(throwable.getMessage(), throwable);
        this.embedded = embedded;
    }

    public static void when(final boolean b, String msg, Object... parameters) throws ExecutionException {
        if (b) {
            throw new ExecutionException(String.format(msg, parameters));
        }
    }

    public static void when(final boolean b, Supplier<String> msg) throws ExecutionException {
        if (b) {
            throw new ExecutionException(msg.get());
        }
    }
}
