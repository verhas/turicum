package ch.turic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code until}, {@code limit}, and {@code timeout} clauses of the {@code flow} command are
 * parsed as expressions, like every other condition in the language ({@code if}, {@code while},
 * the loop {@code until}). A bare identifier condition therefore does not swallow the flow body
 * as a call argument, and no semicolon is needed between the clauses.
 */
class TestFlowConditions {

    private Object run(String source) {
        try (final var interpreter = new Interpreter(source)) {
            return interpreter.compileAndExecute();
        }
    }

    @Test
    void bareIdentifierExitCondition() {
        final var result = run("""
                let r = {
                    flow until finished {
                        counter <- 0;
                        counter <- counter + 1;
                        finished <- counter >= 5;
                        yield counter
                    }
                }
                r
                """);
        assertTrue((long) result >= 5, "unexpected result: " + result);
    }

    @Test
    void multipleClausesNeedNoSemicolons() {
        assertEquals(2L, run("""
                let r = {
                    flow until b >= 2 limit 100 timeout 10 {
                        a <- 1;
                        b <- a * 2;
                        yield b
                    }
                }
                r
                """));
    }

    @Test
    void clausesStillAcceptOptionalSemicolons() {
        assertEquals(2L, run("""
                let r = {
                    flow until b >= 2; limit 100; timeout 10 {
                        a <- 1;
                        b <- a * 2;
                        yield b
                    }
                }
                r
                """));
    }
}
