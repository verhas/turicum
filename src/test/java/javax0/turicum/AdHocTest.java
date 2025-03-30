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
                try {
                  a = 1;
                  pin a;
                  a = 2;
                }catch(exception){
                  print(exception,"\\n");
                  print(exception.message);
                }
                5
                """, "5");
    }

}
