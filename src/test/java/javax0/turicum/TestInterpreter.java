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
                fn z(a)=a*2;
                a = [1,2,3,4,5 ? {|a| a % 2 == 0 || a == 5 } -> z -> {|x| x / 2}];
                """, "[2, 4, 5]");
    }

}
