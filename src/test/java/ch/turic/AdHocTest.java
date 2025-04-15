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
                let i = 0
                while i < 10 {
                  let k = 5;
                  println( k + i)
                  if i > 0 : let k = 7
                  i = i + 1
                }
                """
                , "10");
    }
}
