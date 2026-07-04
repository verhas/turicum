package ch.turic.memory;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An atomic cell holding a single Turicum value, created by the {@code atomic()} built-in
 * function. The cell is the data-centric companion of the {@code sync} command: the value is
 * reachable only through the synchronized operations of the cell, so unsynchronized access is
 * structurally impossible.
 *
 * <pre>{@code
 * let counter = atomic(0)
 * counter.incr()
 * let n = counter.get()
 * }</pre>
 * <p>
 * Operations:
 * <ul>
 *     <li>{@code get()} — the current value; lock-free volatile read</li>
 *     <li>{@code set(v)} — replace the value</li>
 *     <li>{@code update(f)} — atomically replace the value with {@code f(value)}; the function
 *     runs exactly once, under the cell's lock, and the new value is returned. Do not perform
 *     blocking operations or touch other atomics inside the function.</li>
 *     <li>{@code cas(expected, new)} — set to {@code new} if the current value equals
 *     {@code expected} (language equality); returns whether it did</li>
 *     <li>{@code incr()}, {@code decr()}, {@code add(n)} — numeric convenience over
 *     {@code update}, returning the new value</li>
 * </ul>
 * <p>
 * The cell holds <em>immutable snapshots</em>: lists and objects are pinned when they are
 * stored, so a value handed out by {@code get()} cannot be mutated behind the cell's back.
 * To change it, build a new value and {@code set()} or {@code update()} it in. The pinning is
 * shallow, the same way {@code pin} works elsewhere in the language.
 * <p>
 * All mutating operations run under the cell's own {@link ReentrantLock}; {@code get()} reads
 * the {@link AtomicReference} without locking. An aborted thread releases the lock (the release
 * is in a Java {@code finally}), and a thread blocked in {@code update}'s function unwinds
 * through it the usual way.
 */
public class LngAtomic implements HasFields {
    private final AtomicReference<Object> value = new AtomicReference<>();
    private final ReentrantLock lock = new ReentrantLock();

    public LngAtomic(Object initial) {
        value.set(pin(initial));
    }

    /**
     * Pins lists and objects stored in the cell so that the cell only ever holds and hands out
     * immutable snapshots.
     *
     * @param v the value to store
     * @return the same value, pinned when it is a list or an object
     */
    private static Object pin(Object v) {
        switch (v) {
            case LngList list -> list.pinned.set(true);
            case LngObject object -> object.pinned.set(true);
            case null, default -> {
            }
        }
        return v;
    }

    private Object addValue(Object delta) throws ExecutionException {
        lock.lock();
        try {
            final var current = value.get();
            final Object sum;
            if (Cast.isLong(current) && Cast.isLong(delta)) {
                sum = Cast.toLong(current) + Cast.toLong(delta);
            } else if (Cast.isDouble(current) && Cast.isDouble(delta)) {
                sum = Cast.toDouble(current) + Cast.toDouble(delta);
            } else {
                throw new ExecutionException("Atomic value '%s' cannot be incremented by '%s', they are not numbers",
                        current, delta);
            }
            value.set(sum);
            return sum;
        } finally {
            lock.unlock();
        }
    }

    private Object update(Context context, Object fn) throws ExecutionException {
        if (!(fn instanceof LngCallable callable)) {
            throw new ExecutionException("update() needs a function or closure, got '%s'", fn);
        }
        lock.lock();
        try {
            final var newValue = pin(callable.call(context, new Object[]{value.get()}));
            value.set(newValue);
            return newValue;
        } finally {
            lock.unlock();
        }
    }

    private boolean compareAndSet(Object expected, Object newValue) {
        lock.lock();
        try {
            if (Objects.equals(value.get(), expected)) {
                value.set(pin(newValue));
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private Object setValue(Object newValue) {
        lock.lock();
        try {
            value.set(pin(newValue));
            return newValue;
        } finally {
            lock.unlock();
        }
    }

    private static Object oneArg(String method, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "atomic %s() needs exactly one argument", method);
        return args[0];
    }

    private volatile Map<String, Object> fieldMap = null;

    private Map<String, Object> getFieldMap() {
        if (fieldMap == null) {
            synchronized (this) {
                if (fieldMap == null) {
                    fieldMap = Map.of(
                            "get", (LngCallable.LngCallableClosure) (ctx, args) -> value.get(),
                            "set", (LngCallable.LngCallableClosure) (ctx, args) -> setValue(oneArg("set", args)),
                            "update", (LngCallable.LngCallableClosure) (ctx, args) -> update(ctx, oneArg("update", args)),
                            "cas", (LngCallable.LngCallableClosure) (ctx, args) -> {
                                ExecutionException.when(args.length != 2, "atomic cas() needs two arguments");
                                return compareAndSet(args[0], args[1]);
                            },
                            "incr", (LngCallable.LngCallableClosure) (ctx, args) -> addValue(1L),
                            "decr", (LngCallable.LngCallableClosure) (ctx, args) -> addValue(-1L),
                            "add", (LngCallable.LngCallableClosure) (ctx, args) -> addValue(oneArg("add", args))
                    );
                }
            }
        }
        return fieldMap;
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on an atomic value");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var field = getFieldMap().get(name);
        if (field == null) {
            throw new ExecutionException("Unknown atomic field: " + name);
        }
        return field;
    }

    @Override
    public Set<String> fields() {
        return getFieldMap().keySet();
    }

    @Override
    public String toString() {
        return "atomic[" + Cast.toString(value.get()) + "]";
    }
}
