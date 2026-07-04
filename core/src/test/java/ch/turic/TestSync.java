package ch.turic;

import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code sync} command and the {@code mutex()} built-in: mutual exclusion, guaranteed
 * release on error and on thread abortion, interruptible acquisition, reentrancy, and the
 * {@code mtx} type.
 */
class TestSync {

    private Object run(String source) {
        try (final var interpreter = new Interpreter(source)) {
            return interpreter.compileAndExecute();
        }
    }

    @Test
    void syncProvidesMutualExclusion() {
        // 4 children increment a global 200 times each under the mutex; without mutual
        // exclusion the read-modify-write would lose updates
        final var result = run("""
                global counter
                counter = 0
                let l = mutex()
                let bump = {||
                    sync l : counter = counter + 1
                }
                let worker = {||
                    mut i = 0
                    while i < 200 : {
                        bump()
                        i = i + 1
                    }
                }
                let t1 = async { worker() }
                let t2 = async { worker() }
                let t3 = async { worker() }
                let t4 = async { worker() }
                t1.get()
                t2.get()
                t3.get()
                t4.get()
                counter
                """);
        assertEquals(800L, result);
    }

    @Test
    void syncReturnsTheBodyValue() {
        assertEquals(42L, run("""
                let l = mutex()
                let r = { sync l : 42 }
                r
                """));
    }

    @Test
    void syncIsReentrant() {
        assertEquals(1L, run("""
                let l = mutex()
                let r = { sync l { sync l : 1 } }
                r
                """));
    }

    @Test
    void syncReleasesOnException() {
        assertEquals(true, run("""
                let l = mutex()
                try {
                    sync l : die "boom"
                } catch e {
                }
                let acquired = l.try_lock()
                l.unlock()
                acquired
                """));
    }

    @Test
    void syncNeedsAMutex() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                sync 5 : 1
                """));
        assertTrue(e.getMessage().contains("'sync' needs a mutex"), "unexpected message: " + e.getMessage());
    }

    @Test
    void unlockWithoutHoldingIsAnError() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                let l = mutex()
                l.unlock()
                """));
        assertTrue(e.getMessage().contains("does not hold"), "unexpected message: " + e.getMessage());
    }

    @Test
    void mtxTypeAcceptsMutexesAndRejectsOthers() {
        assertEquals("mutex[unlocked]", run("""
                let m : mtx = mutex()
                "" + m
                """).toString());
        assertThrows(ExecutionException.class, () -> run("""
                let m : mtx = 5
                """));
    }

    /**
     * A child aborted by its time limit while *holding* the mutex must release it: the release
     * is in the Java finally of the sync command, which runs even on abortion. Without the
     * release the parent would deadlock here.
     */
    @Test
    void abortedThreadReleasesTheHeldMutex() throws Exception {
        assertOnTime(() -> assertEquals("reacquired", run("""
                let l = mutex()
                let hold = {||
                    sync l {
                        sleep 3600
                    }
                }
                let t = async[time = 0.2] { hold() }
                sleep 0.5
                sync l : "reacquired"
                """)));
    }

    /**
     * A child aborted by its time limit while *waiting* for the mutex must stop waiting:
     * the acquisition is interruptible and abort() interrupts the thread.
     */
    @Test
    void abortedThreadStopsWaitingForTheMutex() throws Exception {
        assertOnTime(() -> assertEquals(true, run("""
                let l = mutex()
                l.lock()
                let wait = {||
                    sync l : "never"
                }
                let t = async[time = 0.2] { wait() }
                sleep 0.5
                let erred = t.is_err()
                l.unlock()
                erred
                """)));
    }

    private void assertOnTime(Runnable test) throws InterruptedException {
        final Throwable[] failure = new Throwable[1];
        final var thread = new Thread(() -> {
            try {
                test.run();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        thread.setDaemon(true);
        thread.start();
        thread.join(15_000);
        assertFalse(thread.isAlive(), "test hung: the mutex was not released/interrupted");
        if (failure[0] != null) {
            fail(failure[0]);
        }
    }
}
