package ch.turic;

import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code veil} command: veiled fields and methods are not accessible through the field
 * access of the object or class, while methods use them as bare variables the usual way.
 * The veil is a guardrail: {@code with} and macros see through it deliberately.
 */
class TestVeil {

    private Object run(String source) {
        try (final var interpreter = new Interpreter(source)) {
            return interpreter.compileAndExecute();
        }
    }

    @Test
    void veiledInstanceFieldIsNotAccessibleFromOutside() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                class Account {
                    fn init(opening) {
                        mut balance = opening
                        veil balance
                    }
                }
                let a = Account(100)
                a.balance
                """));
        assertTrue(e.getMessage().contains("'balance' is veiled"), "unexpected message: " + e.getMessage());
    }

    @Test
    void veiledFieldIsNotWritableOrIndexableFromOutside() {
        assertThrows(ExecutionException.class, () -> run("""
                class Account {
                    fn init(opening) {
                        mut balance = opening
                        veil balance
                    }
                }
                let a = Account(100)
                a.balance = 0
                """));
        assertThrows(ExecutionException.class, () -> run("""
                class Account {
                    fn init(opening) {
                        mut balance = opening
                        veil balance
                    }
                }
                let a = Account(100)
                a["balance"]
                """));
    }

    @Test
    void methodsUseVeiledFieldsAsBareVariables() {
        assertEquals(150L, run("""
                class Account {
                    fn init(opening) {
                        mut balance = opening
                        veil balance
                    }
                    fn deposit(d) { balance = balance + d }
                    fn total() { balance }
                }
                let a = Account(100)
                a.deposit(50)
                a.total()
                """));
    }

    @Test
    void veiledMethodIsNotCallableFromOutside() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                class Account {
                    fn helper() { 42 }
                    veil helper
                }
                let a = Account()
                a.helper()
                """));
        assertTrue(e.getMessage().contains("'helper' is veiled"), "unexpected message: " + e.getMessage());
        // but other methods of the object can call it
        assertEquals(42L, run("""
                class Account {
                    fn helper() { 42 }
                    fn front() { helper() }
                    veil helper
                }
                let a = Account()
                a.front()
                """));
    }

    @Test
    void subclassMethodsSeeParentVeiledFields() {
        assertEquals(100L, run("""
                class Base {
                    fn init() {
                        mut v = 100
                        veil v
                    }
                }
                class Child : Base {
                    fn peek() { v }
                }
                let c = Child()
                c.peek()
                """));
    }

    @Test
    void withLiftsTheVeil() {
        // the documented escape hatch and the cross-instance idiom
        assertEquals(true, run("""
                class Box {
                    fn init(v0) {
                        mut v = v0
                        veil v
                    }
                    fn same(other) { v == { with other : v } }
                }
                let a = Box(42)
                let b = Box(42)
                a.same(b)
                """));
        assertEquals(false, run("""
                class Box {
                    fn init(v0) {
                        mut v = v0
                        veil v
                    }
                    fn same(other) { v == { with other : v } }
                }
                let a = Box(42)
                let b = Box(43)
                a.same(b)
                """));
    }

    @Test
    void keysFiltersVeiledNamesAndKeysAllListsThem() {
        assertEquals(false, run("""
                class Account {
                    fn init() {
                        mut balance = 0
                        veil balance
                    }
                }
                let a = Account()
                let ks = keys(a)
                mut found = false
                for each k in ks : if k == "balance" : found = true
                found
                """));
        assertEquals(true, run("""
                class Account {
                    fn init() {
                        mut balance = 0
                        veil balance
                    }
                }
                let a = Account()
                let ks = keys_all(a)
                mut found = false
                for each k in ks : if k == "balance" : found = true
                found
                """));
    }

    @Test
    void veilingAnUndefinedNameIsAnError() {
        final var e = assertThrows(ExecutionException.class, () -> run("""
                class Account {
                    veil balancee
                }
                let a = Account()
                """));
        assertTrue(e.getMessage().contains("Cannot veil 'balancee'"), "unexpected message: " + e.getMessage());
    }
}
