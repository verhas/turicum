package ch.turic.embed;

import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.StepLimitReached;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Phase 1 embedding API: an engine enforcing a {@link SandboxPolicy} on sessions — step
 * limit, wall-clock timeout, thread cap, I/O redirection, and frozen injected globals.
 */
class TestTuriEngine {

    @Test
    void evaluatesSimpleExpressions() {
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            assertEquals(3L, session.eval("1 + 2"));
        }
    }

    @Test
    void sessionKeepsStateBetweenEvals() {
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.eval("global counter = 40");
            assertEquals(42L, session.eval("counter + 2"));
        }
    }

    @Test
    void sessionsAreIsolated() {
        try (final var engine = TuriEngine.create();
             final var one = engine.newSession();
             final var other = engine.newSession()) {
            one.eval("global leaked = \"secret\"");
            assertThrows(ExecutionException.class, () -> other.eval("leaked"));
        }
    }

    @Test
    void compiledProgramIsReusableAcrossSessions() {
        try (final var engine = TuriEngine.create()) {
            final var program = engine.compile("x * 2");
            try (final var s1 = engine.newSession(); final var s2 = engine.newSession()) {
                s1.set("x", 21);
                s2.set("x", 100);
                assertEquals(42L, s1.eval(program));
                assertEquals(200L, s2.eval(program));
            }
        }
    }

    @Test
    void injectedGlobalsAreFrozen() {
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.set("x", 1);
            assertThrows(ExecutionException.class, () -> session.eval("x = 2"));
        }
    }

    @Test
    void badSyntaxIsReportedAtCompileTime() {
        try (final var engine = TuriEngine.create()) {
            assertThrows(BadSyntax.class, () -> engine.compile("let let let"));
        }
    }

    @Test
    void stdoutIsRedirected() {
        final var captured = new ByteArrayOutputStream();
        final var policy = SandboxPolicy.trusted().stdout(captured).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            session.eval("println \"hello embedder\"");
        }
        assertEquals("hello embedder\n", captured.toString(StandardCharsets.UTF_8));
    }

    @Test
    void stepLimitStopsRunawayLoop() {
        final var policy = SandboxPolicy.trusted().stepLimit(10_000).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class, () -> session.eval("""
                    mut n = 0
                    while true {
                        n = n + 1
                    }
                    """));
            // the halt is converted to the documented ExecutionException at the API boundary
            assertInstanceOf(StepLimitReached.class, e.getCause());
        }
    }

    @Test
    void stepLimitCannotBeCaughtByTheScript() {
        final var policy = SandboxPolicy.trusted().stepLimit(10_000).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
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
    }

    @Test
    void stepsUsedIsReported() {
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.eval("1 + 2");
            assertTrue(session.stepsUsed() > 0);
        }
    }

    @Test
    @Timeout(10)
    void timeoutAbortsABusyLoop() {
        final var policy = SandboxPolicy.trusted().timeout(Duration.ofMillis(200)).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertThrows(TuriTimeoutException.class, () -> session.eval("""
                    mut n = 0
                    while true {
                        n = n + 1
                    }
                    """));
            assertTrue(session.isTimedOut());
            // a timed-out session is dead
            assertThrows(IllegalStateException.class, () -> session.eval("1"));
        }
    }

    @Test
    @Timeout(10)
    void timeoutAbortsASleepingScript() {
        // sleep() consumes no steps; only the wall-clock watchdog can stop it
        final var policy = SandboxPolicy.trusted().timeout(Duration.ofMillis(200)).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            final var start = System.nanoTime();
            assertThrows(TuriTimeoutException.class, () -> session.eval("sleep(60)"));
            final var elapsed = Duration.ofNanos(System.nanoTime() - start);
            assertTrue(elapsed.compareTo(Duration.ofSeconds(5)) < 0,
                    "the watchdog must interrupt the sleep, not wait for it; took " + elapsed);
        }
    }

    @Test
    @Timeout(10)
    void fastScriptIsNotAffectedByTimeout() {
        final var policy = SandboxPolicy.trusted().timeout(Duration.ofSeconds(30)).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertEquals(3L, session.eval("1 + 2"));
        }
    }

    @Test
    @Timeout(10)
    void threadCapLimitsAsyncTasks() {
        final var policy = SandboxPolicy.trusted().maxThreads(2).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class, () -> session.eval("""
                    let t1 = async { sleep(10) }
                    let t2 = async { sleep(10) }
                    let t3 = async { sleep(10) }
                    """));
            assertTrue(e.getMessage().contains("Thread limit") || String.valueOf(e.getCause()).contains("Thread limit"),
                    "unexpected error: " + e);
        }
    }

    @Test
    @Timeout(10)
    void threadPermitsAreReleasedWhenTasksFinish() {
        final var policy = SandboxPolicy.trusted().maxThreads(1).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // sequential async tasks each fit into the single permit
            assertEquals(2L, session.eval("""
                    let t1 = async { 1 }
                    let r1 = await t1
                    let t2 = async { 1 }
                    let r2 = await t2
                    r1 + r2
                    """));
        }
    }

    @Test
    void serializedProgramCanBeLoaded() {
        try (final var engine = TuriEngine.create()) {
            final var turc = engine.compile("6 * 7").serialize();
            try (final var session = engine.newSession()) {
                assertEquals(42L, session.eval(engine.load(turc)));
            }
        }
    }

    @Test
    void closedEngineRejectsNewWork() {
        final var engine = TuriEngine.create();
        engine.close();
        assertThrows(IllegalStateException.class, () -> engine.compile("1"));
        assertThrows(IllegalStateException.class, engine::newSession);
    }

    @Test
    void closedSessionRejectsNewWork() {
        try (final var engine = TuriEngine.create()) {
            final var session = engine.newSession();
            session.close();
            assertThrows(IllegalStateException.class, () -> session.eval("1"));
        }
    }
}
