package ch.turic;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestExpression {

    private void test(final String input, final Object expected) throws Exception {
        final var interpreter = new Interpreter(input);
        final var result = interpreter.compileAndExecute();
        Assertions.assertEquals(expected, result);
    }

    @DisplayName("Test various constant expressions")
    @Test
    void constantIsAValidExpression() throws Exception {
        test("1 + { if true {3;} else {4} }* 4 ", 13L);
    }
}
