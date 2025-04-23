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
if is_defined(z) : println "defined" else: println "not defined";
let z = 1
if is_defined(z) : println "defined" else: println "not defined";
try:
    println {if is_defined(z+2) : "defined" else: "not defined"}
catch: println "no way"
none
                """
                , null);
    }
}
