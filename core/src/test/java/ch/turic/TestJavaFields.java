package ch.turic;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestJavaFields {
    private static Long testField = 2L;
    private Long testField2 = 2L;

    @DisplayName("Test static field setting and reading")
    @Test
    void readAndSetStaticField() throws Exception {
        final String input = """
                let k = java_class("ch.turic.TestJavaFields")
                let p = k.testField
                k.testField = 3
                p
                """;
        testField = 2L;
        final var interpreter = new Interpreter(input);
        final var p = interpreter.compileAndExecute();
        Assertions.assertEquals(2L, p);
        Assertions.assertEquals(3L, testField);
    }

    @DisplayName("Test non-static field setting and reading")
    @Test
    void readAndSetField() throws Exception {
        final String input = """
                let miserably = none
                let k = java_object("ch.turic.TestJavaFields")
                die miserably when k.testField2 != 2
                k.testField2 = 3
                k
                """;
        final var interpreter = new Interpreter(input);
        final var k = interpreter.compileAndExecute();
        Assertions.assertEquals(TestJavaFields.class, k.getClass());
        final var tF = ((TestJavaFields) k).testField2;
        Assertions.assertEquals(3L, tF);
    }
}
