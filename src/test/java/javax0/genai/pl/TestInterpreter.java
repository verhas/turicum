package javax0.genai.pl;

import javax0.genai.pl.commands.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestInterpreter {

    private void test(String input, Object expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals(expected, result);
    }

    private void testE(String input) throws Exception {
        Assertions.assertThrows(ExecutionException.class, () -> new Interpreter(input).execute());
    }

    @Test
    void testBasicCommands() throws Exception {
        test("""
                a.x = 10;
                b["key"] = 20;
                c = 30;
                a.x + b["key"] + c;
                """, 60L);
    }

    @Test
    void testArithmeticOperations() throws Exception {
        test("""
                x = 100;
                y = 50;
                z = x - y * 2;
                z;
                """, 0L);
    }

    @Test
    void testNestedArrayAccess() throws Exception {
        test("""
                arr["outer"]["inner"] = 42;
                arr["outer"]["inner"] * 2;
                """, 84L);
    }

    @Test
    void testMultipleAssignments() throws Exception {
        test("""
                obj.a = 5;
                obj.b = obj.a * 3;
                arr["x"] = obj.b + 2;
                arr["x"];
                """, 17L);
    }

    @Test
    void testComplexExpressions() throws Exception {
        test("""
                x.val = 10;
                y[{"num"}] = 5;
                z = 2;
                result = (x.val + y["num"]) * z;
                result;
                """, 30L);
    }

    @Test
    void testFloatingPointOperations() throws Exception {
        test("""
                x = 10.5;
                y = 3.25;
                z = x * y + 2.5;
                z / 2;
                """, 18.3125);
    }

    @Test
    void testIfStatement() throws Exception {
        test("""
                x = 10;
                result ={
                    if x > 5 {
                        result = x * 2;
                    } else {
                        result = x / 2;
                    }
                }
                result;
                """, 20L);

        test("""
                x = 3;
                result = {if x > 5 {
                    x * 2;
                } else {
                    x / 2;
                }}
                result;
                """, 1.5);
    }

    @Test
    void testExportAssignment() throws Exception {
        test("""
                x = 10;
                { z = 1 }
                { if z {"not ok"} else {"ok"} }
                """, "ok");
        test("""
                 x = 10;
                 z = 7;
                 { z := 2 }
                x * z;
                """, 20L);
        test("""
                 x = 10;
                 z = 7;
                 {{{{{{ z := 2 }}}}}}
                x * z;
                """, 20L);
    }


    @Test
    void testLocalAssignment() throws Exception {
        test("""
                x = 10;
                z = 7;
                h = 6;
                { h= {z = 1};
                  local z,h=2;
                  z = 2 }
                x * z * h;
                """, 10L);
    }

    @Test
    void testGlobalDeclaration() throws Exception {
        test("""
                x = 10;
                z = 7;
                h = 6;
                { global x,z,h }
                5;
                """, 5L);
    }

    @Test
    void testFunctionDeclaration() throws Exception {
        test("""
                fn function x2, z, h {
                } null; //anything undefined is null
                """, null);
    }

    @Test
    void testFinalDeclaration() throws Exception {
        test("""
                final a=1, b=2;
                a*b;
                """, 2L);
        testE("""
                final a=1, b=2;
                a = 3;
                """);
    }

    @Test
    void testQuotedIdentifier() throws Exception {
        test("""
                final `a`=1, `final`=2;
                a*`final`;
                """, 2L);
        test("""
                final a=1, `this is a\\nmulti-line identifier`=2;
                a *`this is a\\nmulti-line identifier`;
                """, 2L);
    }

    @Test
    void testCallingFunction1() throws Exception {
        test("""
                fn f a,b,c {
                    a + b + c
                }
                f(1,1,1);
                """, 3L);
    }

    @Test
    void testCallingFunction2() throws Exception {
        test("""
                f = {fn(a,b,c) {
                    a + b + c
                }}
                f(1,1,1);
                """, 3L);
    }

    @Test
    void testCallingFunction3() throws Exception {
        test("""
                f.k = {fn z(a,b,c) {
                    a + b + c
                }}
                f.k(1,1,1);
                """, 3L);
    }

    @Test
    void testClassDeclaration() throws Exception {
        test("""
                class meClass {
                    global some =  "it works";
                    other = "local";
                    fn a {1}
                    fn b {2}
                }
                some =  "it works";
                some + meClass.a() + meClass.b();
                """, "it works12");
    }

    @Test
    void testClassInstanceCreation() throws Exception {
        test("""
                class meClass {
                    fn a {1}
                    fn b {2}
                }
                object = meClass();
                object.a() + object.b();
                """, 3L);
    }

    @Test
    void testClassInheritance() throws Exception {
        test("""
                class Parent {
                    fn a {1}
                }
                class meClass : Parent {
                    fn b {2}
                }
                object = meClass();
                object.a() + object.b();
                """, 3L);
    }

    @Test
    void testMultipleInheritance() throws Exception {
        test("""
                class P1 {
                 fn p1 {1}
                }
                class P2 : P1 {
                 fn p2 {2}
                }
                class P3 {
                    fn p3 {3}
                }
                class P4 : P3, P2 {
                    fn p4 {4}
                }
                object = P4();
                "" + object.p1() + object.p2() + object.p3() + object.p4();
                """, "1234");
    }


    @Test
    void testConstructorInheritance() throws Exception {
        test("""
                class P1 {
                 fn p1 {this.a}
                }
                class P2 : P1 {
                 fn p2 {b}
                }
                class P3 {
                    fn p3 {this.c}
                }
                class P4(a,b,c) : P3, P2 {
                    fn p4 {
                        a =4;
                        p1()
                    }
                }
                object = P4(1,2,3);
                "" + object.p1() + object.p2() + object.p3() + object.p4();
                """, "1234");
    }

    @Test
    void testMethodInheritance() throws Exception {
        test("""
                class Parent {
                 fn parent {1}
                }
                class Child : Parent {
                 fn child { parent() }
                }
                child = Child();
                child.child();
                """, 1L);
    }

    @Test
    void testThisIsFinal() throws Exception {
        testE("""
                class Class {
                 fn fun {
                   this = 55;
                  }
                }
                `class` = Class();
                `class`.fun();
                """);
    }

}
