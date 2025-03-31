package javax0.turicum;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestExpression {

    @DisplayName("Test various constant expressions")
    @Test
    void constantIsAValidExpression() throws Exception {
        TuriTest
            .input("1 + { if true {3;} else {4} }* 4 ")
            .shouldResultIn(13L);
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
