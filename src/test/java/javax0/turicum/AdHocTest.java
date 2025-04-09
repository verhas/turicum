package javax0.turicum;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals("" + expected, "" + result);
    }

    @Test
    void test() throws Exception {
        test("""
                fn adhoc(){
                  "huhu "
                }
                # {|lexes|
                [""\"
                    fn adhic(){
                      println("hihi")
                    }
                    println("hello");
                    println("1 ",adhoc());
                    println("2 ",adhic());
                ""\",..lexes]
                }
                println("hangyavadasz")
                
                """, null);
    }

}
