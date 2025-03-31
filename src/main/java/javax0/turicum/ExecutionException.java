package javax0.turicum;

import java.util.function.Supplier;

public class ExecutionException extends RuntimeException {

    public ExecutionException(String message, Exception cause) {
        super(message, cause);
    }

    public ExecutionException(String s, Object... params) {
        super(String.format(s, params));
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
