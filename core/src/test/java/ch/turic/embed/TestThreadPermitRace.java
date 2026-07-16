package ch.turic.embed;

import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test: the thread permits of async tasks are acquired on the spawning thread, but
 * used to be waited for only through the task's registered thread. A task that had not started
 * running when the failing eval cleaned up had no registered thread yet, so
 * {@code joinThreads()} neither aborted nor awaited it, and the still-held permit made the next
 * eval fail with "Thread limit reached". The loop makes the scheduling race practically certain
 * to be hit at least once when the bug is present.
 */
public class TestThreadPermitRace {

    @Test
    @Timeout(60)
    void permitsAreBackAfterAFailedEval() {
        for (int i = 0; i < 100; i++) {
            final var policy = SandboxPolicy.trusted()
                    .maxThreads(2)
                    .build();
            try (final var engine = TuriEngine.create(policy);
                 final var session = engine.newSession()) {
                assertThrows(ExecutionException.class, () -> session.eval("""
                        let s1 = async { sleep(10) }
                        let s2 = async { sleep(10) }
                        let s3 = async { sleep(10) }
                        """));
                assertEquals(42L, session.eval("await async { 42 }"), "iteration " + i);
            }
        }
    }
}
