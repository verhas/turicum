package javax0.turicum;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(""+expected, ""+result);
    }

    @Test
    void test() throws Exception {
        test("""
class A(zumba) {
  fn `+`(right) {
     if( right != null ) {
          A(this.zumba + right.zumba)
     } else {
          this
     }
  }
  let `-` = macro({|right|
         if( right != null ) {
              A(this.zumba - evaluate(right).zumba)
         } else {
              A(-this.zumba)
         }
     })
}
let a = -A(1);
let b = A(2);
(b-a).zumba
                """, "3");
    }

}
