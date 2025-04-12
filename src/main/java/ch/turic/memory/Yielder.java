package ch.turic.memory;

import ch.turic.ExecutionException;

/**
 * A yielder can collect data as a callback, and then it can return the collected data.
 * <p>
 * It is an abstraction for asynchronous operations.
 * The name comes from the Python asynchronous '{@code yield}' used in generators.
 * When there is a {@code Yielder} in the context hierarchy the 'yield' command of Turicum calls its
 * {@link #send(Object)} method. The implementation creates a new thread to process it and returns.
 * <p>
 * The method {@link #collect()} will return the processed objects when all the processing is done.
 * <p>
 * The nature of a {@code Yielder} does not need to be asynchronous, but that is the where it makes sense.
 */
public interface Yielder {

    void send(Object o) throws ExecutionException;
    Object[] collect();
}
