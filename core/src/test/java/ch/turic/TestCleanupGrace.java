package ch.turic;

import ch.turic.exceptions.StepLimitReached;
import ch.turic.memory.LocalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bounded cleanup-grace mechanism: after a halt (step limit or abort), a finally/exit
 * block is given a small, one-shot, bounded allowance of extra steps to release resources.
 * It can never suppress the halt, never gets a second allowance for a nested cleanup, and a
 * cleanup that is itself hostile (never terminates) still gets cut off, unconditionally.
 * <p>
 * These tests compile with a plain {@link Interpreter} (for parsing only) and execute the
 * resulting {@link Program} against a manually constructed {@link LocalContext}, because the
 * grace budget is configured via {@code new LocalContext(stepLimit, graceSteps)} — there is
 * deliberately no public {@code Interpreter} constructor for it yet; it is an embedder-level
 * knob, not language syntax.
 */
class TestCleanupGrace {

    private LocalContext newContext(int stepLimit, int graceSteps) {
        final var ctx = new LocalContext(stepLimit, graceSteps);
        BuiltIns.registerGlobalConstants(ctx);
        BuiltIns.register(ctx);
        return ctx;
    }

    private Program compile(String source) {
        try (final var i = new Interpreter(source)) {
            return i.compile();
        }
    }

    @Test
    void defaultConstructorDisablesGraceEntirely() {
        // the 1-arg LocalContext(stepLimit) constructor must behave exactly as before:
        // cleanup never gets a chance to run, matching every pre-existing test in the suite
        final var ctx = newContext0(20);
        final var program = compile("""
                global sideEffect = "untouched"
                try {
                    mut n = 0
                    while true {
                        n = n + 1
                    }
                } finally {
                    global sideEffect
                    sideEffect = "cleanup ran"
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        assertEquals("untouched", ctx.get("sideEffect"), "cleanup must not run when grace is disabled (the default)");
    }

    private LocalContext newContext0(int stepLimit) {
        final var ctx = new LocalContext(stepLimit);
        BuiltIns.registerGlobalConstants(ctx);
        BuiltIns.register(ctx);
        return ctx;
    }

    @Test
    void graceLetsAShortCleanupActuallyRun() {
        final var ctx = newContext(20, 50);
        final var program = compile("""
                global sideEffect = "untouched"
                try {
                    mut n = 0
                    while true {
                        n = n + 1
                    }
                } finally {
                    global sideEffect
                    sideEffect = "cleanup ran"
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        assertEquals("cleanup ran", ctx.get("sideEffect"),
                "a short finally must actually execute under a sufficient grace budget");
    }

    @Test
    void graceIsBoundedAHostileCleanupIsAlsoKilled() {
        final var ctx = newContext(20, 50);
        final var program = compile("""
                mut n = 0
                try {
                    while true {
                        n = n + 1
                    }
                } finally {
                    while true {
                        n = n + 1
                    }
                }
                """);
        final long start = System.currentTimeMillis();
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        final long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 5_000,
                "a finally block that never terminates must still be cut off by the grace budget, not hang; took " + elapsed + "ms");
    }

    @Test
    void withExitCannotSuppressAHalt() {
        final var ctx = newContext(20, 50);
        final var program = compile("""
                class R {
                    fn entry() { none }
                    fn exit(e) {
                        // returning true suppresses an ORDINARY exception; it must have
                        // no effect at all on a halt
                        true
                    }
                }
                mut n = 0
                with R() as r {
                    while true {
                        n = n + 1
                    }
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx),
                "exit() returning true must not suppress a step-limit/abort halt");
    }

    @Test
    void withExitGetsAChanceToRunUnderGrace() {
        final var ctx = newContext(20, 50);
        final var program = compile("""
                global sideEffect = "untouched"
                class R {
                    fn entry() { none }
                    fn exit(e) {
                        global sideEffect
                        sideEffect = "exit ran"
                        none
                    }
                }
                mut n = 0
                with R() as r {
                    while true {
                        n = n + 1
                    }
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        assertEquals("exit ran", ctx.get("sideEffect"), "exit() must actually run under a sufficient grace budget");
    }

    /**
     * Regression for the bug found while designing this feature: {@code callExitMethods}'s
     * overly broad {@code catch (Exception e)} used to catch the halt re-triggered by exit()'s
     * own first statement, wrap it in a plain {@code RuntimeException}, and lose its identity.
     * This must hold even with grace fully disabled.
     */
    @Test
    void withExitDoesNotCorruptTheHaltIdentityEvenWithoutGrace() {
        final var ctx = newContext0(20);
        final var program = compile("""
                class R {
                    fn entry() { none }
                    fn exit(e) { none }
                }
                mut n = 0
                with R() as r {
                    while true {
                        n = n + 1
                    }
                }
                """);
        final var thrown = assertThrows(RuntimeException.class, () -> program.execute(ctx));
        assertInstanceOf(StepLimitReached.class, thrown,
                "the halt must propagate as a genuine StepLimitReached, not a generic wrapped RuntimeException; got: "
                        + thrown.getClass() + ": " + thrown.getMessage());
    }

    @Test
    void nestedCleanupsShareOneGraceWindowNotOneEach() {
        // the inner finally's own infinite loop will consume the ENTIRE grace budget by
        // itself (it never yields it back voluntarily); the outer finally must therefore
        // never get to run at all, proving the budget is shared across the whole unwind,
        // not granted fresh to each nested frame
        final var ctx = newContext(20, 30);
        final var program = compile("""
                global counter = 0
                try {
                    try {
                        mut n = 0
                        while true {
                            n = n + 1
                        }
                    } finally {
                        global counter
                        counter = counter + 1
                        mut m = 0
                        while true {
                            m = m + 1
                        }
                    }
                } finally {
                    global counter
                    counter = counter + 100
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        assertEquals(1L, ctx.get("counter"),
                "the outer finally must never run once the inner one has exhausted the shared grace budget");
    }

    @Test
    void whileLoopsOwnFinallyGetsAChanceUnderGrace() {
        // WhileLoop needed a genuinely new code path (a catch(InterpreterHalt) around the
        // loop, since unlike TryCatch/WithCommand it had no existing Java-level finally at
        // all wrapping the loop body) - this is the regression test for that new path
        final var ctx = newContext(20, 50);
        final var program = compile("""
                global sideEffect = "untouched"
                mut n = 0
                while true {
                    n = n + 1
                } finally {
                    global sideEffect
                    sideEffect = "while-finally ran"
                }
                """);
        assertThrows(StepLimitReached.class, () -> program.execute(ctx));
        assertEquals("while-finally ran", ctx.get("sideEffect"),
                "a while loop's own finally must run under grace when the loop body is what halted");
    }

    /**
     * An {@code async[steps=N]} child gets its {@link LocalContext} through a different
     * constructor path (via {@code ctx.thread()}, used by {@code AsyncEvaluation}) than the
     * other tests here, which all run on the context's own creating thread. This confirms
     * the child's {@code ThreadContext} correctly inherits {@code graceSteps} from the
     * shared {@code GlobalContext} along that path too.
     */
    @Test
    void asyncChildThreadInheritsGraceFromTheRootContext() throws InterruptedException {
        final var ctx = newContext(-1, 50); // unlimited on the root; the child sets its own via steps=
        final var program = compile("""
                // an atomic cell, not a 'global', is the correct way for an async child to
                // publish back to its parent: the child gets a frozen COPY of the variable
                // 'sideEffect', but that copy still refers to the SAME shared atomic cell -
                // whereas reassigning a plain 'global' from inside the child would collide
                // with the child's own frozen snapshot of it (a separate, pre-existing
                // property of async's variable-copying semantics, unrelated to grace)
                let sideEffect = atomic("untouched")
                let z = async[steps = 20] {
                    try {
                        mut n = 0
                        while true {
                            n = n + 1
                        }
                    } finally {
                        sideEffect.set("async cleanup ran")
                    }
                }
                mut i = 0
                while ! z.is_done() && i < 100000 {
                    i = i + 1
                }
                z.is_err()
                """);
        final var result = program.execute(ctx);
        assertEquals(true, result, "the async child must have failed with its own step limit");
        // 'sideEffect' is a top-level 'let', so it lives in ctx's own frame (the root
        // context's frame IS the global heap) and survives across separate execute() calls
        // against the same ctx; freezing only blocks reassigning the BINDING, not calling
        // methods on the atomic cell it refers to. The child ran on its own thread, so give
        // it a moment to finish its finally write after is_done() flips - poll briefly
        // instead of guessing a fixed sleep.
        final var peek = compile("sideEffect.get()");
        final long deadline = System.currentTimeMillis() + 2000;
        String seen = "";
        while (System.currentTimeMillis() < deadline) {
            seen = (String) peek.execute(ctx);
            if ("async cleanup ran".equals(seen)) break;
            Thread.sleep(1);
        }
        assertEquals("async cleanup ran", seen,
                "the async child's own finally must run under the grace inherited from the root context");
    }

    @Test
    void whileLoopNormalFinallyPathsAreUnaffected() {
        // sanity check: the two PRE-EXISTING finally invocation sites (break path, normal
        // completion path) must still work exactly as before and must not be double-invoked
        // by the new catch(InterpreterHalt) path (which must never fire here, since there is
        // no halt at all in this program)
        final var ctx = newContext(100_000, 50);
        final var program = compile("""
                mut count = 0
                mut i = 0
                while i < 5 {
                    i = i + 1
                } finally {
                    global finallyRuns
                    global finallyRuns = 0
                    finallyRuns = finallyRuns + 1
                }
                finallyRuns
                """);
        final var result = program.execute(ctx);
        assertEquals(1L, result, "the normal-completion finally must run exactly once, not zero or twice");
    }
}
