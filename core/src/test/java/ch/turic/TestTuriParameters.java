package ch.turic;

import ch.turic.commands.Closure;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestTuriParameters {

    @TuriParameters("a, b = 2, @mode = \"fast\", [rest], {meta}, ^body")
    public static class AnnotatedBuiltin implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var rest = (LngList) arguments[3];
            final var meta = (LngObject) arguments[4];
            final var body = (Closure) arguments[5];
            final var bodyValue = body == null ? "none" : body.call(context);
            return "a=%s,b=%s,mode=%s,rest=%s,extra=%s,body=%s".formatted(
                    arguments[0],
                    arguments[1],
                    arguments[2],
                    rest,
                    meta.getField("extra"),
                    bodyValue);
        }
    }

    public static class PositionalOnlyBuiltin implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            return arguments.length;
        }
    }

    @Test
    void annotatedBuiltinUsesFullTuricumArgumentBinding() {
        try (final var interpreter = new Interpreter("""
                annotated(1, 3, 5, 8, mode = "slow", extra = 9) {|| 10}
                """)) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("annotated", new AnnotatedBuiltin()));
            assertEquals("a=1,b=3,mode=slow,rest=[5, 8],extra=9,body=10", result);
        }
    }

    @Test
    void annotatedBuiltinAppliesDefaultsAndEmptyRestAndMeta() {
        try (final var interpreter = new Interpreter("annotated(1)")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("annotated", new AnnotatedBuiltin()));
            assertEquals("a=1,b=2,mode=fast,rest=[],extra=null,body=none", result);
        }
    }

    @Test
    void unannotatedBuiltinKeepsLegacyPositionalCalling() {
        try (final var interpreter = new Interpreter("legacy(a = 1)")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("legacy", new PositionalOnlyBuiltin()));
            // The legacy path still evaluates named arguments as their value only, so the builtin
            // receives one positional argument and returns normally. If this ever starts failing,
            // the compatibility contract changed.
            assertEquals(1, result);
        }
    }
}
