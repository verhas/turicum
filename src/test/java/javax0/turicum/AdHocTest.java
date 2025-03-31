package javax0.turicum;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(expected, result.toString());
    }

    @Test
    void test() throws Exception {
        test("""
                class z{
                    fn constructor {
                      k = PrinterClass();
                      k.msg = "Z says: "
                      return k;
                    }
                }
                class PrinterClass{
                  fn constructor {
                    msg = ""
                    this
                  }
                  print = fn (message){
                    global msg;
                    msg = msg + message
                    }
                }
                
                p = z()
                p.print("csaka haha ")
                p.print("fogorvos hihi ")
                p.print("segithet huhu")
               1
                """, "1");
    }

}
