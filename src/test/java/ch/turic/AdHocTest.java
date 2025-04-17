package ch.turic;

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
fn generator start=1 {
        while {
            println "yielding %s" % start
            yield start;
            start = start + 1;
        }
    }

let z = stream|100| generator(1)
let i = 0;
for each z in z {
        println("fetching %s" % z)
        sleep 0.1
        i = i + 1;
}until i > 100;
none
                """
                , null);
    }
}
