package javax0.turicum;

import javax0.turicum.commands.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestInterpreter {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(expected, result.toString());
    }

    private void testE(String input) {
        Assertions.assertThrows(ExecutionException.class, () -> new Interpreter(input).execute());
    }

    @Test
    void test() throws Exception {
        test("""
                class MyObject {
                    bibas = 13;
                    fn zee(){
                     
                      1
                    }
                }
                MyObject().zee()
                """, "1");
    }

}
