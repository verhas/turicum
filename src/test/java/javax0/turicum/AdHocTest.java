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
                let imp = import("/Users/verhasp/github/turicum/src/test/resources/import.sample.turi");
                imp.myPrint("hello")
                """, "3");
    }

}
