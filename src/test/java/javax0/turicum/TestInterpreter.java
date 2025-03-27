package javax0.turicum;

import javax0.turicum.commands.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestInterpreter {

    private void test(String input, Object expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(expected, result);
    }

    private void testE(String input) {
        Assertions.assertThrows(ExecutionException.class, () -> new Interpreter(input).execute());
    }

    @Test
    void test() throws Exception {
        test("""
                a = [1..4];
                k = 0;
                for each z in a : k = k + z
                """, 6L);
    }

}
