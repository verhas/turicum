package ch.turic;

import ch.turic.builtins.functions.debugger.DebugSession;
import ch.turic.builtins.functions.debugger.DebugSessionFactory;
import ch.turic.memory.LocalContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives a debug session programmatically, at machine speed — the way a GUI debugger does.
 * The CLI REPL debugger exercises the same machinery with a human in the loop, whose typing
 * latency hides races between the controller completing a work item and the debugged thread
 * arriving at the await.
 */
class TestDebugSession {

    private DebugSession startSession(Path program) throws Exception {
        final var ctx = new LocalContext();
        final var session = (DebugSession) new DebugSessionFactory().call(ctx, new Object[]{program.toString()});
        // wait for the debugged program to reach its first pause
        final long deadline = System.currentTimeMillis() + 5_000;
        while (!session.is_started() && !session.is_finished()) {
            assertTrue(System.currentTimeMillis() < deadline, "the debugged program did not start");
            Thread.sleep(1);
        }
        // the reported thread name is that of the interpreter-creating thread (the
        // ThreadContext is created before the debugged virtual thread starts), so the first
        // reported thread is selected, exactly like the FX debugger front end does
        final var threads = session.threads();
        assertFalse(threads.array.isEmpty(), "the paused session must report a thread");
        session.set_thread(threads.array.get(0).toString());
        return session;
    }

    private void awaitPaused(DebugSession session) throws Exception {
        final long deadline = System.currentTimeMillis() + 5_000;
        while (!session.is_paused() && !session.is_finished()) {
            assertTrue(System.currentTimeMillis() < deadline,
                    "the debugged thread did not pause again: step command lost");
            Thread.sleep(1);
        }
    }

    /**
     * Steps through a program as fast as the machine allows. Each step is a
     * complete()/await() rendezvous on a fresh {@code ConcurrentWorkItem}; a race between
     * the controller's {@code complete()} and the debuggee's {@code await()} shows up here
     * as an exception or as a lost wake-up (hang, caught by the deadline).
     */
    @Test
    void machineSpeedStepping() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 0
                a = a + 1
                a = a + 2
                a = a + 3
                a = a + 4
                a = a + 5
                a
                """);
        try {
            final var session = startSession(program);
            int steps = 0;
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished() && steps < 1000) {
                assertTrue(System.currentTimeMillis() < deadline, "stepping hung after " + steps + " steps");
                if (session.is_paused()) {
                    // step_into: the first pause is at the outermost program node, and a
                    // step_over there runs the whole program in one go
                    session.step_into();
                    steps++;
                    awaitPaused(session);
                } else {
                    Thread.sleep(1);
                }
            }
            assertTrue(session.is_finished(), "the program must run to completion, executed steps: " + steps);
            assertTrue(steps > 3, "stepping must have advanced the program, executed steps: " + steps);
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * Line-granular stepping, the way the FX front end's Step button works: repeat the
     * node-level step_into until the execution point leaves the current source line. The
     * program must pause on each of its lines in order.
     */
    @Test
    void lineGranularStepping() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 1
                a = a + 1
                a = a + 2
                a
                """);
        try {
            final var session = startSession(program);
            final var visited = new java.util.ArrayList<Integer>();
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished()) {
                assertTrue(System.currentTimeMillis() < deadline, "line stepping hung, visited: " + visited);
                if (!session.is_paused()) {
                    Thread.sleep(1);
                    continue;
                }
                session.fetch_pos();
                awaitPaused(session);
                if (session.is_finished()) {
                    break;
                }
                final int line = session.start_pos().line;
                if (visited.isEmpty() || visited.get(visited.size() - 1) != line) {
                    visited.add(line);
                }
                // step until the line changes (the front end's step_line)
                final int start = line;
                int guard = 0;
                while (!session.is_finished() && guard++ < 100) {
                    session.step_into();
                    awaitPaused(session);
                    if (session.is_finished()) {
                        break;
                    }
                    session.fetch_pos();
                    awaitPaused(session);
                    if (session.is_finished() || session.start_pos().line != start) {
                        break;
                    }
                }
            }
            // every line of the program must have been visited, in order
            assertEquals(java.util.List.of(1, 2, 3, 4), visited, "visited lines");
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * The FX front end's "Step Over" scenario: after opening, the session pauses at the
     * outermost Program node — stepping over that would run the whole program. The front
     * end therefore descends one node first; from there, step-over advances one statement
     * at a time instead of finishing the program on the first press.
     */
    @Test
    void stepOverAfterInitialDescent() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 1
                a = a + 1
                a = a + 2
                a
                """);
        try {
            final var session = startSession(program);
            // descend from the Program node to the first statement, like debug() does
            session.step_into();
            awaitPaused(session);
            final var visited = new java.util.ArrayList<Integer>();
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished()) {
                assertTrue(System.currentTimeMillis() < deadline, "hung, visited: " + visited);
                if (!session.is_paused()) {
                    Thread.sleep(1);
                    continue;
                }
                session.fetch_pos();
                awaitPaused(session);
                if (session.is_finished()) {
                    break;
                }
                final int line = session.start_pos().line;
                if (visited.isEmpty() || visited.get(visited.size() - 1) != line) {
                    visited.add(line);
                }
                // one step_line(step_over) press
                int guard = 0;
                while (!session.is_finished() && guard++ < 100) {
                    session.step_over();
                    awaitPaused(session);
                    if (session.is_finished()) {
                        break;
                    }
                    session.fetch_pos();
                    awaitPaused(session);
                    if (session.is_finished() || session.start_pos().line != line) {
                        break;
                    }
                }
            }
            assertTrue(visited.size() >= 3,
                    "step-over must visit the statements one by one, not finish at once; visited: " + visited);
            assertEquals(1, visited.get(0), "stepping must start at the first line");
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * Stepping into a function call must pause inside the function, at a node that has a
     * source position. Runtime-generated helper commands (argument binding, internal field
     * accesses) have no position; if single-stepping stopped on them, the front end would
     * face a pause it cannot display.
     */
    @Test
    void stepIntoAFunctionCallPausesInsideTheFunction() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                fn f(x) {
                    let y = x + 1
                    y
                }
                let r = f(5)
                r
                """);
        try {
            final var session = startSession(program);
            session.step_into(); // descend from the Program node
            awaitPaused(session);
            // step line-wise until we stand on the call line (5)
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished() && currentLine(session) != 5) {
                assertTrue(System.currentTimeMillis() < deadline, "did not reach the call line");
                session.step_over();
                awaitPaused(session);
            }
            assertFalse(session.is_finished(), "must not finish before the call line");
            // ONE line-granular step-into press
            final int start = 5;
            int guard = 0;
            int line = start;
            while (!session.is_finished() && guard++ < 100) {
                session.step_into();
                awaitPaused(session);
                if (session.is_finished()) {
                    break;
                }
                line = currentLine(session);
                if (line != start) {
                    break;
                }
            }
            assertFalse(session.is_finished(), "step-into must not run the program to the end");
            assertEquals(2, line, "must pause on the first statement inside the function");
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * A breakpoint added while paused must stop a subsequent 'run' (Continue) at that line.
     */
    @Test
    void continueStopsAtBreakpoint() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 1
                a = a + 1
                a = a + 2
                a = a + 3
                a = a + 4
                a
                """);
        try {
            final var session = startSession(program);
            session.step_into(); // descend from the Program node
            awaitPaused(session);
            session.add_breakpoint(5);
            awaitPaused(session);
            session.run();
            awaitPaused(session);
            assertFalse(session.is_finished(), "the run must stop at the breakpoint, not finish");
            assertEquals(5, currentLine(session), "must stop at the breakpoint line");
            // removing it lets the next run finish
            session.remove_breakpoint(5);
            awaitPaused(session);
            session.run();
            final long deadline = System.currentTimeMillis() + 5_000;
            while (!session.is_finished()) {
                assertTrue(System.currentTimeMillis() < deadline, "must finish after the breakpoint is removed");
                Thread.sleep(1);
            }
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * A single 'run' issued while paused AT a breakpoint must leave that line and stop at
     * the next breakpoint (or finish) — not re-pause on the next node of the same line.
     */
    @Test
    void runFromABreakpointLineLeavesTheLine() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 1
                a = a + 1
                a = a + 2
                a = a + 3
                a
                """);
        try {
            final var session = startSession(program);
            session.step_into();
            awaitPaused(session);
            session.add_breakpoint(2);
            awaitPaused(session);
            session.add_breakpoint(4);
            awaitPaused(session);
            session.run();
            awaitPaused(session);
            assertEquals(2, currentLine(session), "first run must stop at the first breakpoint");
            session.run(); // ONE run from the breakpoint line
            awaitPaused(session);
            assertFalse(session.is_finished(), "must stop at the next breakpoint");
            assertEquals(4, currentLine(session), "one run from a breakpoint must reach the NEXT breakpoint");
            session.run();
            final long deadline = System.currentTimeMillis() + 5_000;
            while (!session.is_finished()) {
                assertTrue(System.currentTimeMillis() < deadline, "must finish after the last breakpoint");
                Thread.sleep(1);
            }
        } finally {
            Files.deleteIfExists(program);
        }
    }

    /**
     * The post-resume suppression must re-arm when execution leaves the line: a breakpoint
     * inside a loop body fires on every iteration.
     */
    @Test
    void breakpointInALoopFiresEveryIteration() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut i = 0
                while i < 3 {
                    i = i + 1
                }
                i
                """);
        try {
            final var session = startSession(program);
            session.step_into();
            awaitPaused(session);
            session.add_breakpoint(3);
            awaitPaused(session);
            int hits = 0;
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished()) {
                assertTrue(System.currentTimeMillis() < deadline, "loop breakpoint test hung, hits: " + hits);
                session.run();
                awaitPaused(session);
                if (session.is_finished()) {
                    break;
                }
                assertEquals(3, currentLine(session), "every stop must be at the loop-body breakpoint");
                hits++;
            }
            assertEquals(3, hits, "the breakpoint must fire once per loop iteration");
        } finally {
            Files.deleteIfExists(program);
        }
    }

    private int currentLine(DebugSession session) throws Exception {
        session.fetch_pos();
        awaitPaused(session);
        if (session.is_finished() || session.start_pos() == null) {
            return -1;
        }
        return session.start_pos().line;
    }

    /**
     * The fetch-and-wait dance the debugger front ends use: request the position, wait for
     * the pause, read the response — repeatedly and fast.
     */
    @Test
    void machineSpeedPositionFetching() throws Exception {
        final var program = Files.createTempFile("debugged", ".turi");
        Files.writeString(program, """
                mut a = 0
                a = a + 1
                a = a + 2
                a
                """);
        try {
            final var session = startSession(program);
            int rounds = 0;
            final long deadline = System.currentTimeMillis() + 10_000;
            while (!session.is_finished() && rounds < 200) {
                assertTrue(System.currentTimeMillis() < deadline, "fetch/step hung after " + rounds + " rounds");
                if (session.is_paused()) {
                    session.fetch_pos();
                    awaitPaused(session);
                    if (session.is_finished()) {
                        break;
                    }
                    assertNotNull(session.start_pos(), "position response must be available after the pause");
                    session.step_over();
                    rounds++;
                    awaitPaused(session);
                } else {
                    Thread.sleep(1);
                }
            }
            assertTrue(session.is_finished(), "the program must run to completion, rounds: " + rounds);
        } finally {
            Files.deleteIfExists(program);
        }
    }
}
