package ch.turic.embed;

import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.StepLimitReached;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the sandboxing limits of the {@code ch.turic.embed} API. The snippet markers in
 * this file are referenced from {@code EMBEDDING.md}; after changing a snippet, regenerate the
 * document with {@code mdship update EMBEDDING.md}.
 */
class TestEmbeddingLimits {

    @Test
    void stepLimitStopsARunawayScript() {
        // snippet step_limit
        final var policy = SandboxPolicy.builder()
                .stepLimit(10_000)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class, () -> session.eval("""
                    global mut n = 0
                    while true {
                        n = n + 1
                    }
                    """));
            assertInstanceOf(StepLimitReached.class, e.getCause());
            final var n = (long)session.get("n");
            Assertions.assertTrue(10_000 > n && n > 1_000 );
        }
        // end snippet
    }

    @Test
    void theScriptCannotCatchTheHalt() {
        // snippet uncatchable_halt
        final var policy = SandboxPolicy.builder()
                .stepLimit(10_000)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // the script wraps the runaway loop into try/catch; the halt fires anyway,
            // because a halt is not a Turicum exception and cannot be swallowed in-script
            assertThrows(ExecutionException.class, () -> session.eval("""
                    try {
                        mut n = 0
                        while true {
                            n = n + 1
                        }
                    } catch e {
                        "swallowed"
                    }
                    """));
        }
        // end snippet
    }

    @Test
    void graceStepsLetCleanupRunAfterAHalt() {
        // snippet grace_steps
        final var policy = SandboxPolicy.builder()
                .stepLimit(10_000)
                .graceSteps(1_000)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class, () -> session.eval("""
                    global resource = "open"
                    try {
                        mut n = 0
                        while true {
                            n = n + 1
                        }
                    } finally {
                        global resource
                        resource = "closed"
                    }
                    """));
            // the halt fired, but the finally block got its bounded grace budget
            assertEquals("closed", session.get("resource"));
        }
        // end snippet
    }

    @Test
    @Timeout(10)
    void wallClockTimeoutAbortsEvenASleepingScript() {
        // snippet timeout
        final var policy = SandboxPolicy.builder()
                .timeout(Duration.ofMillis(200))
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // sleep() consumes no interpreter steps; only the wall-clock watchdog stops it,
            // and it does so by interrupting the thread, without waiting out the sleep
            assertThrows(TuriTimeoutException.class, () -> session.eval("sleep(60)"));

            // a timed-out session is aborted for good and cannot be used anymore
            assertTrue(session.isTimedOut());
            assertThrows(IllegalStateException.class, () -> session.eval("1"));
        }
        // end snippet
    }

    @Test
    @Timeout(10)
    void threadCapLimitsConcurrentAsyncTasks() {
        // snippet thread_cap
        final var policy = SandboxPolicy.builder()
                .maxThreads(2)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // two concurrent tasks fit into the two permits and run to completion
            assertEquals(3L, session.eval("""
                    let t1 = async { 1 }
                    let t2 = async { 2 }
                    let r1 = await t1
                    let r2 = await t2
                    r1 + r2
                    """));

            // a third task while two are still running does not fit; it fails
            // immediately - the cap never blocks the script waiting for a permit
            final var e = assertThrows(ExecutionException.class, () -> session.eval("""
                    let s1 = async { sleep(10) }
                    let s2 = async { sleep(10) }
                    let s3 = async { sleep(10) }
                    """));
            assertEquals("Thread limit reached, cannot start a new thread", e.getMessage());

            // the permits belong to running tasks, not to the session: they were released
            // when the failed evaluation aborted its tasks, so the session keeps working
            assertEquals(42L, session.eval("await async { 42 }"));
        }
        // end snippet
    }

    @Test
    @Timeout(10)
    void singleThreadForbidsAsyncEntirely() {
        // snippet single_thread
        final var policy = SandboxPolicy.builder()
                .singleThread()             // alias for maxThreads(0)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // the main interpreter thread does not count against the limit;
            // ordinary sequential code is unaffected
            assertEquals(42L, session.eval("6 * 7"));

            // but no additional thread can be started at all
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("async { 1 }"));
            assertEquals("Thread limit reached, cannot start a new thread", e.getMessage());
        }
        // end snippet
    }

    @Test
    void resettingTheStepBudget() {
        // snippet reset_steps
        final var policy = SandboxPolicy.builder()
                .stepLimit(10_000)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // the limit caps the TOTAL steps of the session; once it is hit, every
            // further evaluation halts immediately ...
            assertThrows(ExecutionException.class, () -> session.eval("""
                    mut n = 0
                    while true {
                        n = n + 1
                    }
                    """));
            assertThrows(ExecutionException.class, () -> session.eval("1 + 1"));

            // ... until the embedder - and only the embedder - grants a fresh budget;
            // resetSteps() also reports the steps consumed, for per-evaluation metering
            final long used = session.resetSteps();
            assertTrue(used >= 10_000);
            assertEquals(2L, session.eval("1 + 1"));
        }
        // end snippet
    }

    @Test
    void meteringStepsUsed() {
        // snippet metering
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.eval("""
                    mut sum = 0
                    for i = 1 ; i <= 100 ; i++ {
                        sum += i
                    }
                    sum
                    """);
            // stepsUsed() reports the total interpreter steps of the session; use it to
            // meter tenants and to calibrate stepLimit() for real workloads
            assertTrue(session.stepsUsed() > 100);
        }
        // end snippet
    }
}
