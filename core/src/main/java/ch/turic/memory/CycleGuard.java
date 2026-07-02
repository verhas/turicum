package ch.turic.memory;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Utility class that provides cycle-safe {@code equals()} and {@code hashCode()} support
 * for Turicum runtime objects ({@link LngList}, {@link LngObject}, etc.).
 * <p>
 * Both methods use a {@link ThreadLocal} guard to track objects currently being compared
 * or hashed on the call stack. When a cycle is detected (the same object pair or object
 * is encountered again), the method short-circuits instead of recursing infinitely.
 * <p>
 * The first invocation on a given thread ("root") creates the guard and cleans it up
 * in a {@code finally} block. All nested invocations reuse the same guard, regardless
 * of which runtime type they originate from.
 */
public class CycleGuard {

    /**
     * An identity-based pair used as key in the equals guard set.
     * Uses {@code ==} for comparison and {@link System#identityHashCode} for hashing,
     * so it tracks specific object instances rather than their logical equality.
     */
    private static final class Pair {
        final Object a;
        final Object b;

        Pair(Object a, Object b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Pair p && a == p.a && b == p.b;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(a) * 31 + System.identityHashCode(b);
        }
    }

    private static final ThreadLocal<HashSet<Pair>> EQUALS_GUARD = new ThreadLocal<>();
    private static final ThreadLocal<IdentityHashMap<Object, Object>> HASH_GUARD = new ThreadLocal<>();
    private static final ThreadLocal<IdentityHashMap<Object, Object>> TO_STRING_GUARD = new ThreadLocal<>();

    private CycleGuard() {
    }

    private static final class Guard<T> implements AutoCloseable {
        private final boolean isRoot;
        private final ThreadLocal<T> threadLocal;
        private final T guardMap;

        static <K> Guard<K> of(final ThreadLocal<K> threadLocal, final Supplier<K> supplier) {
            return new Guard<>(threadLocal, supplier);
        }

        private Guard(final ThreadLocal<T> threadLocal, final Supplier<T> supplier) {
            this.threadLocal = threadLocal;
            if (threadLocal.get() == null) {
                isRoot = true;
                this.threadLocal.set(supplier.get());
            } else {
                isRoot = false;
            }
            this.guardMap = threadLocal.get();
        }

        T seen() {
            return guardMap;
        }

        @Override
        public void close() {
            if (isRoot) {
                threadLocal.remove();
            }
        }
    }

    /**
     * Cycle-safe equality check. Call this from {@code equals()} implementations
     * after handling identity, type, and cheap structural checks (like size).
     * <p>
     * If the pair {@code (a, b)} is already being compared on this thread's call stack,
     * the method returns {@code true} (assumes equal to break the cycle). Otherwise it
     * registers the pair and delegates to {@code comparator} for the actual field-by-field
     * comparison.
     *
     * @param a          the first object ({@code this} in the caller)
     * @param b          the second object (the other object)
     * @param comparator performs the actual structural comparison; called only when no cycle is detected
     * @return {@code true} if equal (or if a cycle is detected), {@code false} otherwise
     */
    public static boolean equals(Object a, Object b, BooleanSupplier comparator) {
        try (final var guard = Guard.of(EQUALS_GUARD, HashSet::new)) {
            final var pair = new Pair(a, b);

            if (!guard.seen().add(pair)) {
                return true;
            }
            try {
                return comparator.getAsBoolean();
            } finally {
                guard.seen().remove(pair);
            }
        }
    }

    /**
     * Cycle-safe {@code toString()} computation. Call this from {@code toString()} implementations.
     * <p>
     * If {@code obj} is already being converted to string on this thread's call stack, returns
     * {@code cycleValue} to break the cycle. Otherwise it registers the object and delegates
     * to {@code computer}.
     *
     * @param obj        the object being converted ({@code this} in the caller)
     * @param cycleValue the string to return when a cycle is detected (e.g. {@code "..."})
     * @param computer   performs the actual string computation; called only when no cycle is detected
     * @return the string representation, or {@code cycleValue} if a cycle is detected
     */
    public static String toString(Object obj, String cycleValue, Supplier<String> computer) {
        try (final var guard = Guard.of(TO_STRING_GUARD, IdentityHashMap::new)) {
            if (guard.seen().containsKey(obj)) return cycleValue;
            guard.seen().put(obj, null);
            try {
                return computer.get();
            } finally {
                guard.seen().remove(obj);
            }
        }
    }

    /**
     * Cycle-safe hash code computation. Call this from {@code hashCode()} implementations.
     * <p>
     * If {@code obj} is already being hashed on this thread's call stack, returns {@code 0}
     * to break the cycle. Otherwise, it registers the object and delegates to {@code computer}.
     *
     * @param obj      the object being hashed ({@code this} in the caller)
     * @param computer performs the actual hash computation; called only when no cycle is detected
     * @return the hash code, or {@code 0} if a cycle is detected
     */
    public static int hashCode(Object obj, IntSupplier computer) {
        try (final var guard = Guard.of(HASH_GUARD, IdentityHashMap::new)) {
            if (guard.seen().containsKey(obj)) {
                return 0;
            }
            guard.seen().put(obj, null);
            try {
                return computer.getAsInt();
            } finally {
                guard.seen().remove(obj);
            }
        }
    }
}
