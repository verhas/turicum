package ch.turic.lsp;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrency gate implementing <em>debounce</em> + <em>latest-wins</em> + <em>single-flight</em>
 * semantics for expensive operations.
 *
 * <h2>Concept</h2>
 * Multiple threads may signal that expensive work should run (e.g., re-index, re-parse, re-compute).
 * This class ensures:
 * <ul>
 *   <li><b>Debounce:</b> work starts only after a configured quiet period has elapsed without a newer request.</li>
 *   <li><b>Latest-wins:</b> if a newer request arrives while you are waiting, your attempt is cancelled.</li>
 *   <li><b>Single-flight:</b> at most one caller may run the expensive section at any time.</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * Call {@link #open()} when you want to run the expensive work. If it returns a {@link Lease},
 * you own the single-flight slot and may proceed. Always release the lease.
 *
 * <pre>{@code
 * try (var lease = gate.open()) {
 *     if (lease == null) {
 *         return; // superseded by a newer request, or gate has been closed
 *     }
 *     // ... expensive work ...
 * }
 * }</pre>
 *
 * <h2>Cancellation model</h2>
 * Cancellation is cooperative: a caller learns it has lost by receiving {@code null} from
 * {@link #open()}, or (optionally) by checking {@link Lease#isStillLatest()} during computation.
 *
 * <h2>Thread interruption</h2>
 * {@link #open()} is interruptible. If the current thread is interrupted while waiting,
 * it throws {@link InterruptedException} and does not acquire a lease.
 */
public final class SlowStart implements AutoCloseable {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();

    private final long quietPeriodNanos;


    /**
     * {@code true} once this gate has been permanently closed.
     * <p>
     * <b>Guarded by {@link #lock}:</b> all reads/writes must hold {@code lock} (no {@code volatile} needed).
     */
    private boolean closed = false;

    /**
     * Monotonically increasing "request id". Newer request supersedes older ones.
     */
    private long generation = 0;

    /**
     * {@code true} while expensive work is currently running under a lease.
     */
    private boolean running = false;

    /**
     * Creates a new gate with the given debounce window.
     *
     * @param quietPeriod the minimum time that must pass without a newer request before work may start
     * @throws NullPointerException if {@code quietPeriod} is {@code null}
     * @throws IllegalArgumentException if {@code quietPeriod} is zero or negative
     */
    public SlowStart(Duration quietPeriod) {
        Objects.requireNonNull(quietPeriod, "quietPeriod");
        if (quietPeriod.isZero() || quietPeriod.isNegative()) {
            throw new IllegalArgumentException("quietPeriod must be > 0");
        }
        this.quietPeriodNanos = quietPeriod.toNanos();
    }

    /**
     * Announces interest in running the expensive operation and waits until it is allowed to start.
     *
     * <p>This method implements two sequential waiting phases:</p>
     * <ol>
     *   <li><b>Debounce phase:</b> wait until the quiet period elapses with no newer request.</li>
     *   <li><b>Single-flight phase:</b> wait until no other thread is currently running under a lease.</li>
     * </ol>
     *
     * <p><b>Latest-wins:</b> If a newer request arrives while you are waiting in either phase,
     * this call returns {@code null}.</p>
     *
     * @return a {@link Lease} if the caller is permitted to run; {@code null} if superseded by a newer
     *         request or if the gate was closed
     * @throws InterruptedException if the current thread is interrupted while waiting
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

    /**
     * Permanently closes this gate.
     *
     * <p>After closing:</p>
     * <ul>
     *   <li>All current and future {@link #open()} calls return {@code null} (or may be unblocked to do so).</li>
     *   <li>Any threads waiting inside {@link #open()} are awakened so they can observe the closed state.</li>
     * </ul>
     *
     * <p>Closing the gate does not forcibly stop already-running work; it only prevents new leases from being
     * granted and allows cooperative cancellation checks via {@link Lease#isStillLatest()}.</p>
     */
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

    /**
     * A lease representing permission to run the expensive operation.
     *
     * <p>Only one lease may be active at a time (single-flight). Closing the lease releases the slot and wakes
     * up waiting callers.</p>
     *
     * <p>Leases are intended to be used with try-with-resources.</p>
     */
    public final class Lease implements AutoCloseable {
        private final long myGen;
        private boolean released = false;

        private Lease(long myGen) {
            this.myGen = myGen;
        }

        /**
         * Releases the single-flight slot held by this lease.
         *
         * <p>This method is idempotent: calling it multiple times has no additional effect.</p>
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
         * Returns whether this lease still corresponds to the latest request generation and the gate is open.
         *
         * <p>This is an optional cooperative cancellation hook: long-running computations can periodically
         * check this and abort early if a newer request arrived or the gate was closed.</p>
         *
         * @return {@code true} if the gate is not closed, and no newer request has superseded this lease;
         *         {@code false} otherwise
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