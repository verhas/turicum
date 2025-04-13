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
// snippet pinObject
// null
let a = {x:1,y:2};
pin {a}
let hu = { try {
    a.x = 3
    print("huhh")
}catch e {
    break 55
}};

hu
                
                """, null);
    }
}
