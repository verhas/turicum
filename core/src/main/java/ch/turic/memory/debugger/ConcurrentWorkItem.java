package ch.turic.memory.debugger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ConcurrentWorkItem<T> {
    private T payload = null;
    private final CountDownLatch done = new CountDownLatch(1);
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
    public void complete(T result) {
        this.payload = result;
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
     * Called by the sender thread to wait for result
     */
    public T await() throws InterruptedException, Throwable {
        done.await();
        if (error != null) throw error;
        return payload;
    }

    /**
     * Optional: bounded wait
     */
    public T await(long timeout, TimeUnit unit) throws InterruptedException, Throwable {
        if (!done.await(timeout, unit)) {
            throw new RuntimeException("Timeout waiting for result");
        }
        if (error != null) throw error;
        return payload;
    }
}