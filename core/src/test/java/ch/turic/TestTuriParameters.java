package ch.turic;

import ch.turic.commands.Closure;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.utils.parameter.Declare;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static ch.turic.utils.parameter.Declare.Parameter.param;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    public static class FactoryBuiltin implements TuriFunction {
        private final ParameterList parameters = Declare.params(
                param("a").integer().mandatory(),
                param("b").integer().defaultValue(2),
                param("mode").str().named().defaultValue("fast")
        ).rest("rest").meta("meta").closure("body").done();

        @Override
        public ParameterList parameters() {
            return parameters;
        }

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

    public static class ConstantDefaultsBuiltin implements TuriFunction {
        private final ParameterList parameters = Declare.params(
                param("i").integer().defaultValue(1),
                param("l").integer().defaultValue(2L),
                param("f").floating().defaultValue(2.5F),
                param("d").num().defaultValue(1.5),
                param("flag").bool().defaultValue(true),
                param("text").str().defaultValue("turicum"),
                param("nothing").any().defaultNone()
        ).done();

        @Override
        public ParameterList parameters() {
            return parameters;
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            return "i=%s/%s,l=%s/%s,f=%s/%s,d=%s/%s,flag=%s,text=%s,nothing=%s".formatted(
                    arguments[0], arguments[0].getClass().getSimpleName(),
                    arguments[1], arguments[1].getClass().getSimpleName(),
                    arguments[2], arguments[2].getClass().getSimpleName(),
                    arguments[3], arguments[3].getClass().getSimpleName(),
                    arguments[4],
                    arguments[5],
                    arguments[6]);
        }
    }

    public static class ObjectDefaultBuiltin implements TuriFunction {
        private static final Object DEFAULT_OBJECT = new Object();

        private final ParameterList parameters = Declare.params(
                param("value").any().defaultValue(DEFAULT_OBJECT)
        ).done();

        @Override
        public ParameterList parameters() {
            return parameters;
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            return arguments[0] == DEFAULT_OBJECT;
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
    void factoryDeclaredBuiltinUsesFullTuricumArgumentBinding() {
        try (final var interpreter = new Interpreter("""
                factory(1, 3, 5, 8, mode = "slow", extra = 9) {|| 10}
                """)) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("factory", new FactoryBuiltin()));
            assertEquals("a=1,b=3,mode=slow,rest=[5, 8],extra=9,body=10", result);
        }
    }

    @Test
    void factoryDeclaredBuiltinAppliesDefaultsAndEmptyRestAndMeta() {
        try (final var interpreter = new Interpreter("factory(1)")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("factory", new FactoryBuiltin()));
            assertEquals("a=1,b=2,mode=fast,rest=[],extra=null,body=none", result);
        }
    }

    @Test
    void factoryDeclaredBuiltinUsesConstantDefaultValues() {
        try (final var interpreter = new Interpreter("constants()")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("constants", new ConstantDefaultsBuiltin()));
            assertEquals("i=1/Long,l=2/Long,f=2.5/Double,d=1.5/Double,flag=true,text=turicum,nothing=null", result);
        }
    }

    @Test
    void factoryDeclaredBuiltinUsesObjectDefaultValue() {
        try (final var interpreter = new Interpreter("object_default()")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program, Map.of("object_default", new ObjectDefaultBuiltin()));
            assertEquals(true, result);
        }
    }

    @Test
    void fluentTypeHelpersAppendTypes() {
        final var builder = param("path").str();
        assertSame(builder, builder.or());

        final var parameter = builder.or().none().named().defaultNone();
        final var types = parameter.types();

        assertEquals(2, types.length);
        assertEquals("str", types[0].identifier());
        assertEquals("none", types[1].identifier());

        final var concurrencyParameter = param("cell").mtx().or().atm().or().some().mandatory();
        final var concurrencyTypes = concurrencyParameter.types();

        assertEquals(3, concurrencyTypes.length);
        assertEquals("mtx", concurrencyTypes[0].identifier());
        assertEquals("atm", concurrencyTypes[1].identifier());
        assertEquals("some", concurrencyTypes[2].identifier());
    }

    @Test
    void jsonifyBeautyUsesFactoryDeclaredParameters() {
        try (final var interpreter = new Interpreter("jsonify_beauty([1, 2, 3], margin = 1, tab = 2)")) {
            final var program = interpreter.compile();
            final var result = interpreter.execute(program);
            assertEquals("""
                    [
                      1,
                      2,
                      3
                    ]""", result);
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
