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
                class B {
                   fn constructor {
                     print("b.this.cls ", this.cls,"\\n");
                     print("b.cls ", cls,"\\n");
                     print("b.this ", this,"\\n");
                     this.b_field = "b_field";
                     this.field = "b field value";
                   }
                   inherited = "inherited"
                   field = "overridden"
                   fn fun(field) {
                        print("b object field ? ",this.field,"\\n");
                        print("b object feld ? ",this.feld,"\\n");
                        print("b class field ? ",cls.field,"\\n");
                        print("b argument ? ",field,"\\n");
                   }
                }
                class A : B {
                   fn constructor {
                     B.constructor();
                     print("a.this ", this,"\\n");
                     this.field = "object field";
                   }
                   field = "class field";
                   fn fun(field) {
                        print("this.this ? ",this.this,"\\n");
                        print("this.cls ? ",this.cls,"\\n");
                        print("object field ? ",this.field,"\\n");
                        print("object b_field ? ",this.b_field,"\\n");
                        print("object feld ? ",this.feld,"\\n");
                        print("class field ? ",cls.field,"\\n");
                        print("class inherited ? ",cls.inherited,"\\n");
                        print("argument ? ",field,"\\n");
                   }
                }
                
                A().fun("local");
                """, null);
    }

}
