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
                class A {
                    fn equals(other) {
                      return a == other.a;
                    }
                }
                let a = A()
                a.a = 1
                a.b = 4
                let b = A()
                b.a = 1
                b.b = 3
                println(a == b)
                null
                """, null);
    }

}
