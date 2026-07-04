package ch.turic.commands;

import ch.turic.Interpreter;
import ch.turic.memory.LocalContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code AbstractCommand.toLngObject} must build a fresh snapshot on every call. The result is a
 * mutable {@code LngObject} bound to the caller's interpreter, so caching it on the command would
 * leak mutations between {@code as_object()} calls and would bind the object to whichever
 * context happened to call first.
 */
class ToLngObjectTest {

    @Test
    void eachCallCreatesAFreshObject() {
        try (final var interpreter = new Interpreter("1 + 1")) {
            final var program = interpreter.compile();
            final var ctx = new LocalContext();
            final var first = program.toLngObject(ctx);
            final var second = program.toLngObject(ctx);
            assertNotSame(first, second, "toLngObject must not cache the converted object");

            first.setField("marker", 42L);
            assertNull(second.getField("marker"),
                    "mutation of one snapshot must not leak into another");
        }
    }

    @Test
    void conversionIsBoundToTheCallingInterpreter() {
        try (final var interpreter = new Interpreter("1 + 1")) {
            final var program = interpreter.compile();
            // the same compiled program converted in two different interpreters (global contexts)
            final var ctx1 = new LocalContext();
            final var ctx2 = new LocalContext();
            final var first = program.toLngObject(ctx1);
            final var second = program.toLngObject(ctx2);
            assertSame(ctx1.globalContext, first.context().globalContext,
                    "the object must live in the calling interpreter");
            assertSame(ctx2.globalContext, second.context().globalContext,
                    "the object must live in the calling interpreter, not the one that called first");
        }
    }
}
