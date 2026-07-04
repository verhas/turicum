package ch.turic.memory;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A mutual exclusion lock for Turicum programs, created by the {@code mutex()} built-in function
 * and used by the {@code sync} command:
 *
 * <pre>{@code
 * let l = mutex()
 * sync l {
 *     // executed while holding the mutex
 * }
 * }</pre>
 * <p>
 * The {@code sync} command acquires and releases through the Java-level {@link #acquire()} and
 * {@link #release()} methods, with the release in a Java {@code finally} — so the mutex is
 * released even when the executing thread is aborted (timeout, step limit) while holding it.
 * Acquisition is interruptible, so a thread blocked waiting for the mutex can be unblocked by
 * {@link ThreadContext#abort()}.
 * <p>
 * The methods {@code lock()}, {@code unlock()}, {@code try_lock([seconds])}, {@code is_locked()}
 * and {@code is_held()} are also available on the object for advanced use. When using them
 * directly, the program is responsible for releasing what it acquired; {@code sync} is the
 * recommended form.
 * <p>
 * The lock is reentrant: nested {@code sync} on the same mutex is allowed.
 */
public class LngMutex implements HasFields {
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Acquires the mutex, waiting if necessary. The wait is interruptible so that
     * {@link ThreadContext#abort()} can unblock a waiting thread.
     *
     * @throws ExecutionException if the thread is interrupted while waiting
     */
    public void acquire() throws ExecutionException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Interrupted while waiting for a mutex");
        }
    }

    /**
     * Releases the mutex.
     *
     * @throws ExecutionException if the current thread does not hold the mutex
     */
    public void release() throws ExecutionException {
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            throw new ExecutionException("unlock() called on a mutex that the thread does not hold");
        }
    }

    private boolean tryAcquire(Object[] args) throws ExecutionException {
        if (args.length == 0) {
            return lock.tryLock();
        }
        final var seconds = Cast.toDouble(args[0]);
        try {
            return lock.tryLock((long) (seconds * 1_000_000_000L), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Interrupted while waiting for a mutex");
        }
    }

    private volatile Map<String, Object> fieldMap = null;

    private Map<String, Object> getFieldMap() {
        if (fieldMap == null) {
            synchronized (this) {
                if (fieldMap == null) {
                    fieldMap = Map.of(
                            "lock", new TuriMethod<>((args) -> {
                                acquire();
                                return null;
                            }),
                            "unlock", new TuriMethod<>((args) -> {
                                release();
                                return null;
                            }),
                            "try_lock", new TuriMethod<>(this::tryAcquire),
                            "is_locked", new TuriMethod<>(lock::isLocked),
                            "is_held", new TuriMethod<>(lock::isHeldByCurrentThread)
                    );
                }
            }
        }
        return fieldMap;
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on a mutex");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var field = getFieldMap().get(name);
        if (field == null) {
            throw new ExecutionException("Unknown mutex field: " + name);
        }
        return field;
    }

    @Override
    public Set<String> fields() {
        return getFieldMap().keySet();
    }

    @Override
    public String toString() {
        return lock.isLocked() ? "mutex[locked]" : "mutex[unlocked]";
    }
}
