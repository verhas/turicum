package ch.turic.memory;

/**
 * A yielder can accommodate data transfer between two coroutines/threads.
 * <p>
 * The model is that one thread -- let's call it a parent thread -- creates an asynchronous thread -- the child thread.
 * They can communicate by sending each other messages.
 * Messages are arbitrary objects (wrapped into a {@link ch.turic.memory.Channel.Message} object).
 * A yielder contains two channels, and it can provide these to channels to whoever wants to read it.
 * Typically, the parent thread and the child thread.
 */
public interface Yielder {
    Channel toChild();

    Channel toParent();
}
