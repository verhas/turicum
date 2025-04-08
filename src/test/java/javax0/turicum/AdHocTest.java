package javax0.turicum;

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
                let z :lst = [1,2,3,4,5,6,7,8,9]
                z[inf..inf] = ["a"]
                println(z)
                null
                """, null);
    }

}
