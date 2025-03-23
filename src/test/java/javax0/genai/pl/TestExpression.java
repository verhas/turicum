package javax0.genai.pl;


import javax0.genai.pl.analyzer.ExpressionAnalyzer;
import javax0.genai.pl.analyzer.Input;
import javax0.genai.pl.analyzer.Lexer;
import javax0.genai.pl.memory.Context;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestExpression {

    private void test(final String input, final Object expected) throws Exception {
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(new Lexer().analyze(Input.fromString(input)));
        final var ctx = new Context();
        ctx.let("true", true);
        ctx.let("false", false);
        final var result = expression.execute(ctx);
        Assertions.assertEquals(expected, result);
    }

    @DisplayName("Test various constant expressions")
    @Test
    void constantIsAValidExpression() throws Exception {
        test("1 + { if true {3;} else {4} }* 4 }", 13L);
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
