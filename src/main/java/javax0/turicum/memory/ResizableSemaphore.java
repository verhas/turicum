package javax0.turicum.memory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ResizableSemaphore extends Semaphore {

    private final AtomicInteger inUse = new AtomicInteger(0);
    private volatile int maxPermits;

    public ResizableSemaphore(int initialPermits, boolean fair) {
        super(initialPermits, fair);
        this.maxPermits = initialPermits;
        super.release(initialPermits);
    }

    protected void reducePermits(int permits) {
        super.reducePermits(permits);
    }

    public synchronized void setLimit(int newLimit) {
        int delta = newLimit - maxPermits;
        maxPermits = newLimit;

        if (delta > 0) {
            super.release(delta);
        } else if (delta < 0) {
            reducePermits(-delta);
        }
    }

    public Permit acquirePermit() throws InterruptedException {
        super.acquire();
        inUse.incrementAndGet();
        return new Permit();
    }

    public int getInUse() {
        return inUse.get();
    }

    public int getLimit() {
        return maxPermits;
    }

    public class Permit implements AutoCloseable {
        private boolean released = false;

        @Override
        public void close() {
            if (!released) {
                released = true;
                inUse.decrementAndGet();
                ResizableSemaphore.super.release(1);
            }
        }
    }

}
