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
println("%s = %s" % [4,3])
{
    fn k(x,this=me){
        if x == 1 || x ==  0 : 1
        else: me(x-1) + me(x-2)
    }
k(10)
}
                        """
                , "89");
    }
}
