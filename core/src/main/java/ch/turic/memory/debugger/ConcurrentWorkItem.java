package ch.turic.memory.debugger;

import java.util.concurrent.CountDownLatch;

public class ConcurrentWorkItem<T> {
    private T payload;
    private CountDownLatch done;
    private Throwable error = null;

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
        done = new CountDownLatch(1);
        done.await();
        if (error != null) throw error;
        return payload;
    }
}