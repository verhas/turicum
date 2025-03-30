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
                for i=1 ; i < 10 ; i = i +1 {
                   {{{break i when i == 3}}}
                }
                """, "3");
    }

}
