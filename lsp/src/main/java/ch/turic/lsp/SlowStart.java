package ch.turic.lsp;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Debounce + latest-wins + single-flight gate with a one-step open().
 * <p>
 * Usage:
 * <pre>
 *   try (var lease = gate.open()) {
 *     if (lease == null) return; // superseded or closed
 *     // ... expensive work ...
 *   }
 * </pre>
 */
public final class SlowStart implements AutoCloseable {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();

    private final long quietPeriodNanos;

    private boolean closed = false;

    /**
     * Monotonically increasing "request id". Newer request supersedes older ones.
     */
    private long generation = 0;

    /**
     * True, while expensive work is currently running under a lease.
     */
    private boolean running = false;

    public SlowStart(Duration quietPeriod) {
        Objects.requireNonNull(quietPeriod, "quietPeriod");
        if (quietPeriod.isZero() || quietPeriod.isNegative()) {
            throw new IllegalArgumentException("quietPeriod must be > 0");
        }
        this.quietPeriodNanos = quietPeriod.toNanos();
    }

    /**
     * Announces interest in doing expensive work.
     * <p>
     * This method blocks for the debounce window and for single-flight availability.
     *
     * @return a Lease if the caller is allowed to run, or null if the caller was superseded or gate closed.
     */
    public Lease open() throws InterruptedException {
        final long myGen;
        final long deadline;

        lock.lockInterruptibly();
        try {
            if (closed) return null;

            generation++;
            myGen = generation;
            deadline = System.nanoTime() + quietPeriodNanos;

            // Wake any waiters so they can notice supersession and/or reset their debounce.
            changed.signalAll();
        } finally {
            lock.unlock();
        }

        lock.lockInterruptibly();
        try {
            // ---- Debounce phase: wait until quiet period elapses without a newer request.
            while (true) {
                if (closed) return null;
                if (myGen != generation) return null; // newer request arrived -> I lose

                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) break;

                changed.awaitNanos(remaining);
            }

            // ---- Single-flight phase: wait until no expensive work is running.
            while (true) {
                if (closed) return null;
                if (myGen != generation) return null; // newer request arrived while waiting -> I lose

                if (!running) {
                    running = true;
                    return new Lease(myGen);
                }
                // Avoid permanent sleep; periodically re-check supersession/closed.
                changed.await(50, TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public final class Lease implements AutoCloseable {
        private final long myGen;
        private boolean released = false;

        private Lease(long myGen) {
            this.myGen = myGen;
        }

        /**
         * Release the single-flight lock.
         * Always call via try-with-resources.
         */
        @Override
        public void close() {
            lock.lock();
            try {
                if (released) return;
                released = true;

                // Only release if we still hold the running slot.
                // (We should, but this makes it robust.)
                if (running) {
                    running = false;
                }
                changed.signalAll();
            } finally {
                lock.unlock();
            }
        }

        /**
         * Optional: exposes whether this lease still corresponds to the latest generation.
         * Useful if you want to bail out mid-computation.
         */
        public boolean isStillLatest() {
            lock.lock();
            try {
                return !closed && myGen == generation;
            } finally {
                lock.unlock();
            }
        }
    }
}