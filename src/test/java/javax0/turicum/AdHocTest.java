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
                
                fn a {  b() }
                fn b {  c() }
                fn c {  d() }
                fn d {  let a :str = 1; }
                
                try{
                    a()
                }catch e {
                        for each st in e.stack_trace {
                          println(st);
                          }
                }
                println("--------------------")
                try{
                    a()
                }catch e {
                        for each st in e.stack_trace {
                          println(st);
                          }
                }
                
                
                """, null);
    }
}
