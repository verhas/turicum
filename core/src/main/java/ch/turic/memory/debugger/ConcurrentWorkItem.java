package ch.turic.memory.debugger;

import java.util.concurrent.CountDownLatch;

/**
 * A single rendezvous between the debugged thread and the debugger controller.
 * <p>
 * The debugged thread sends the item through a channel and then calls {@link #await()}; the
 * controller picks the item up, fills in the command, and calls {@link #complete()} (or
 * {@link #fail(Throwable)}) to release the debugged thread.
 * <p>
 * The latch is created in the constructor, before the item becomes visible to the other
 * thread. It must not be created lazily in {@code await()}: the controller may call
 * {@code complete()} in the window between the channel send and the debugged thread reaching
 * {@code await()} — with a lazily created latch that is a data race that either throws
 * {@code NullPointerException} in the controller or silently loses the wake-up, leaving the
 * debugged thread parked forever. A GUI or scripted debugger responds fast enough to hit
 * that window; a human typing at a REPL almost never does, which is why the CLI proof of
 * concept appeared to work.
 */
public class ConcurrentWorkItem<T> {
    private final T payload;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile Throwable error = null;

    public ConcurrentWorkItem(T payload) {
        this.payload = payload;
    }

    public static <K> ConcurrentWorkItem<K> of(K payload) {
        return new ConcurrentWorkItem<>(payload);
    }

    /**
     * Called by the worker to access the payload
     */
    public T payload() {
        return payload;
    }

    /**
     * Called by the worker to mark processing done
     */
    public void complete() {
        done.countDown();
    }

    /**
     * Called by the worker to mark an error
     */
    public void fail(Throwable t) {
        this.error = t;
        done.countDown();
    }

    /**
     * Called by the sender thread to wait for the result
     */
    public T await() throws InterruptedException, Throwable {
        done.await();
        if (error != null) throw error;
        return payload;
    }
}
