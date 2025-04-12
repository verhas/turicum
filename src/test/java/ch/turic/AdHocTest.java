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
                let a = java_object("java.math.BigInteger","13227564339977585599444343999543452348834588845343232885343")
                let b = java_object("java.math.BigInteger","13227564339977585599444343999543452348834588845343232885343")
                let c = java_call(a,"add",b)
                println(c);
                
                """, null);
    }
}
