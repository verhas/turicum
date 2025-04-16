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
class FibCalculator {
    let cache = [];
    fn fib(x) {
        if x == 1 || x == 0 : 1
        else{
            println(x)
            println( cache )
            return cache[x] if cache[x] != none;
            println( "cache = " , cache )
            println("this =", this )
            cache[x] = fib(x - 1) + fib(x - 2);
            println( "cache = " , cache )
            cache[x]
        }
    }
}
println(FibCalculator.fib(10))
none
                """
                , null);
    }
}
