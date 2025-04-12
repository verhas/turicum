package ch.turic;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestExpression {

    private void test(final String input, final Object expected) throws Exception {
        final var interpreter = new Interpreter(input);
        final var result = interpreter.execute();
        Assertions.assertEquals(expected, result);
    }

    @DisplayName("Test various constant expressions")
    @Test
    void constantIsAValidExpression() throws Exception {
        test("1 + { if true {3;} else {4} }* 4 ", 13L);
        //     test("+13", 13L);
        //     test("113", 113L);
        //     test("1+1", 2L);
        //     test("1-1", 0L);
        //     test("1*1", 1L);
        //     test("1/1", 1L);
        //     test("1%1", 0L);
        //     test("1+ 1", 2L);
        //     test("1- 1", 0L);
        //     test("1* 1", 1L);
        //     test("1/ 1", 1L);
        //     test("1% 1", 0L);
        //     test("1 + 1 *3", 4L);
        //     test("(1 + 1 )*3", 6L);
        //     test("(1 + 1/2 )*3", 3L);
        //     test("-13", -13L);
        //     test("(1 + 1/2 )*3 && 0", false);
        //     test("!((1 + 1/2 )*3) && 0", false);
        //     test("1 < 2", true);
    }
}
