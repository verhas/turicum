package javax0.turicum;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(expected, result.toString());
    }

    @Test
    void test() throws Exception {
        test("""
                class meClass {
                    global some =  "it works";
                    other = "local";
                    fn a {1}
                    fn b {2}
                }
                some =  "it works";
                some + meClass.a() + meClass.b();
                """, "it works12");
    }

}
