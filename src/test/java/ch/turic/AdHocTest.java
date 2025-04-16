package ch.turic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals("" + expected, "" + result);
    }

    @Test
    void test() throws Exception {
        test("""
(fn (!arg,[rest],{meta},^closure)
            { arg(..rest,..meta,..closure)})
     (fn () {println("Hello")})
                """
                , null);
    }
}
