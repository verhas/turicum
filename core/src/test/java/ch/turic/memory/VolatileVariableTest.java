package ch.turic.memory;

import ch.turic.Interpreter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Global variables live in the heap {@link VarTable}, which stores them as
 * {@link VolatileVariable}s so that a write on one thread is promptly visible on the others.
 * Local frames keep plain {@link Variable}s: they are thread confined, and cross-thread
 * handoffs (channels, async start, futures) provide the happens-before edge themselves,
 * so the interpreter's hot path pays no volatile cost.
 */
class VolatileVariableTest {

    @Test
    void heapCreatesVolatileVariablesLocalFramesPlainOnes() {
        final var heap = new VarTable(true);
        heap.set("g", 1L);
        assertInstanceOf(VolatileVariable.class, heap.get("g"));
        assertEquals(1L, heap.get("g").get());

        final var defined = heap.define("h");
        assertInstanceOf(VolatileVariable.class, defined);
        assertSame(defined, heap.get("h"), "define must store the very instance it returns");

        final var local = new VarTable();
        local.set("l", 1L);
        assertFalse(local.get("l") instanceof VolatileVariable);
        assertFalse(local.define("m") instanceof VolatileVariable);
    }

    @Test
    void putIntoTheHeapConvertsPlainVariables() {
        final var heap = new VarTable(true);
        final var plain = new Variable("imported");
        plain.set(42L);
        heap.put("imported", plain);
        final var stored = heap.get("imported");
        assertInstanceOf(VolatileVariable.class, stored);
        assertEquals(42L, stored.get());

        // a local table must keep the instance untouched
        final var local = new VarTable();
        local.put("imported", plain);
        assertSame(plain, local.get("imported"));
    }

    @Test
    void topLevelLetCreatesAVolatileHeapVariable() {
        // the root context's frame IS the global heap
        final var ctx = new LocalContext();
        ctx.define("x", 1L);
        assertInstanceOf(VolatileVariable.class, ctx.globalContext.heap.get("x"));
        assertEquals(1L, ctx.get("x"));
    }

    /**
     * Functional smoke test: a global written by an async child must become visible to the
     * polling parent. The child cannot assign the global directly (async children receive
     * frozen snapshots of the parent variables), so it calls a closure that captured the root
     * context — the one language construct that writes a global from another thread. With
     * volatile storage the polling loop is guaranteed to terminate; the iteration bound only
     * guards the test against a visibility regression.
     */
    @Test
    void globalWriteInChildBecomesVisibleInParent() {
        try (final var interpreter = new Interpreter("""
                global signal
                signal = 0
                let setit = {|| signal = 1}
                let t = async {
                    setit()
                }
                mut n = 0
                while signal == 0 && n < 10000000 : {
                    n = n + 1
                }
                signal
                """)) {
            assertEquals(1L, interpreter.compileAndExecute(),
                    "the parent must observe the global written by the child");
        }
    }
}
