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
                z = 55;
                x = {|| print(z,"\\n")};
                x();
                {
                  local z = 63;
                  x();
                  reclose(x)();
                }
                1
                """, "1");
    }

}
