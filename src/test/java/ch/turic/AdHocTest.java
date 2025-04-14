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
fn gen_1_to_10 {
    for i=1 ; i <= 10 ; i = i + 1:
        yield i;
}

let st = stream(gen_1_to_10());
println( st );
while st.has_next() :
    println(st.next());
                        """
                , null);
    }
}
