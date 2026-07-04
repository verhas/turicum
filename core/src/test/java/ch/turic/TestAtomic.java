package ch.turic;

import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code atomic()} built-in: an atomic cell holding any value, with lock-free reads,
 * synchronized mutation, exactly-once {@code update}, numeric convenience operations,
 * pinned (immutable) snapshots, and guaranteed lock release on thread abortion.
 */
class TestAtomic {

    private Object run(String source) {
        try (final var interpreter = new Interpreter(source)) {
            return interpreter.compileAndExecute();
        }
    }

    @Test
    void holdsAnyValue() {
        assertEquals("hello", run("""
                let a = atomic("hello")
                a.get()
                """));
        assertNull(run("""
                let a = atomic()
                a.get()
                """));
        assertEquals(42L, run("""
                let a = atomic(0)
                a.set(42)
                a.get()
                """));
    }

    @Test
    void updateAppliesTheFunctionAndReturnsTheNewValue() {
        assertEquals(84L, run("""
                let a = atomic(42)
                a.update({|x| x * 2})
                """));
        assertEquals(84L, run("""
                let a = atomic(42)
                a.update({|x| x * 2})
                a.get()
                """));
    }

    @Test
    void incrementIsAtomicAcrossThreads() {
        // four children increment the same cell 200 times each without any other
        // synchronization; a non-atomic read-modify-write would lose updates
        assertEquals(800L, run("""
                let c = atomic(0)
                let t1 = async { mut i = 0; while i < 200 : { c.incr(); i = i + 1 } }
                let t2 = async { mut i = 0; while i < 200 : { c.incr(); i = i + 1 } }
                let t3 = async { mut i = 0; while i < 200 : { c.incr(); i = i + 1 } }
                let t4 = async { mut i = 0; while i < 200 : { c.incr(); i = i + 1 } }
                t1.get()
                t2.get()
                t3.get()
                t4.get()
                c.get()
                """));
    }

    @Test
    void numericConvenienceOperations() {
        assertEquals(10L, run("""
                let a = atomic(0)
                a.incr()
                a.incr()
                a.add(9)
                a.decr()
                a.get()
                """));
        assertEquals(2.5, run("""
                let a = atomic(1.5)
                a.add(1)
                """));
        final var e = assertThrows(ExecutionException.class, () -> run("""
                let a = atomic("x")
                a.incr()
                """));
        assertTrue(e.getMessage().contains("not numbers"), "unexpected message: " + e.getMessage());
    }

    @Test
    void casUsesLanguageEquality() {
        assertEquals(true, run("""
                let a = atomic([1, 2])
                a.cas([1, 2], "replaced")
                """));
        assertEquals(false, run("""
                let a = atomic([1, 2])
                a.cas([1, 3], "replaced")
                """));
        assertEquals("kept", run("""
                let a = atomic("kept")
                a.cas("other", "replaced")
                a.get()
                """));
    }

    @Test
    void storedValuesArePinnedSnapshots() {
        // the list stored in the cell is pinned; mutating it through any reference fails
        assertThrows(ExecutionException.class, () -> run("""
                mut lst = [1, 2]
                let a = atomic(lst)
                lst[0] = 9
                """));
        assertThrows(ExecutionException.class, () -> run("""
                let a = atomic([1, 2])
                let v = a.get()
                v[0] = 9
                """));
    }

    @Test
    void updateNeedsACallable() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                let a = atomic(0)
                a.update(5)
                """));
        assertTrue(e.getMessage().contains("needs a function or closure"), "unexpected message: " + e.getMessage());
    }

    @Test
    void atmTypeAcceptsAtomicsAndRejectsOthers() {
        assertEquals("atomic[0]", run("""
                let a : atm = atomic(0)
                "" + a
                """));
        assertThrows(ExecutionException.class, () -> run("""
                let a : atm = 5
                """));
    }

    /**
     * A child aborted by its time limit while inside {@code update}'s function must release
     * the cell's lock; otherwise the parent would deadlock on the next mutation.
     */
    @Test
    void abortedThreadReleasesTheCellLock() throws Exception {
        final Throwable[] failure = new Throwable[1];
        final var thread = new Thread(() -> {
            try {
                assertEquals(1L, run("""
                        let a = atomic(0)
                        let stall = {||
                            a.update({|x|
                                sleep 3600
                                x
                            })
                        }
                        let t = async[time = 0.2] { stall() }
                        sleep 0.5
                        a.incr()
                        """));
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        thread.setDaemon(true);
        thread.start();
        thread.join(15_000);
        assertFalse(thread.isAlive(), "test hung: the atomic cell's lock was not released on abort");
        if (failure[0] != null) {
            fail(failure[0]);
        }
    }
}
