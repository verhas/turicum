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
let turicum = 13
let list = [1,2,3]
let object = { x:1, y:2};
pin turicum, [list], {object}

try {
  turicum = 14;
}catch e: println("could not change the variable")

try {
  list[1] = 0;
}catch e: println("could not change the list")
list = [ 0, ..list, 4]
println("variable 'list' still can be changed: ", list)

try {
  object.x = 3;
}catch e: println("could not change the object")
object = { x:1, y:3 }
println("variable 'object' still can be changed: ", object)
                """, null);
    }

}
