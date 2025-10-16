package ch.turic.exceptions;

import ch.turic.memory.LngException;

import java.util.function.Supplier;

/**
 * Runtime exception class for execution-related errors in the Turi language.
 * This class provides various constructors for different error scenarios and
 * supports embedding of LngException objects for additional context.
 */
public class ExecutionException extends RuntimeException {
    /**
     * Returns the embedded LngException if one exists.
     *
     * @return The embedded LngException object or null if none exists
     */
    public LngException embedded() {
        return embedded;
    }

    private LngException embedded = null;

    /**
     * Creates an exception with a cause and message.
     *
     * @param cause   The underlying exception
     * @param message The error message
     */
    public ExecutionException(Exception cause, String message) {
        super(message, cause);
    }

    /**
     * Creates an exception with a formatted message.
     *
     * @param s      The message format string
     * @param params The parameters to format the message
     */
    public ExecutionException(String s, Object... params) {
        super(String.format(s, params));
    }

    /**
     * Creates an exception with a cause and formatted message.
     *
     * @param t      The underlying throwable
     * @param s      The message format string
     * @param params The parameters to format the message
     */
    public ExecutionException(Throwable t, String s, Object... params) {
        super(String.format(s, params), t);
    }

    /**
     * Creates an exception from another throwable.
     *
     * @param throwable The underlying throwable
     */
    public ExecutionException(Throwable throwable) {
        super(throwable.getMessage(), throwable);
    }

    /**
     * Creates an exception from a throwable with an embedded LngException.
     *
     * @param throwable The underlying throwable
     * @param embedded  The embedded LngException
     */
    public ExecutionException(Throwable throwable, LngException embedded) {
        super(throwable.getMessage(), throwable);
        this.embedded = embedded;
    }

    /**
     * Throws an ExecutionException with a formatted message when the condition is true.
     *
     * @param b The condition to check
     * @param msg The message format string
     * @param parameters The parameters to format the message
     * @throws ExecutionException when the condition is true
     */
    public static void when(final boolean b, String msg, Object... parameters) throws ExecutionException {
        if (b) {
            throw new ExecutionException(String.format(msg, parameters));
        }
    }

    /**
     * Throws an ExecutionException with a message from a supplier when the condition is true.
     *
     * @param b   The condition to check
     * @param msg The message supplier
     * @throws ExecutionException when the condition is true
     */
    public static void when(final boolean b, Supplier<String> msg) throws ExecutionException {
        if (b) {
            throw new ExecutionException(msg.get());
        }
    }
}
