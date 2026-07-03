package ch.turic;

import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cells of a flow are local to the flow ({@code LocalContext.registerLocal}). A flow does not care
 * what variables are defined around it: a cell may freely shadow a variable of the surrounding
 * scope (or a global). The cell is used inside the flow; the outer variable is neither read (not
 * even before the cell computes its first value) nor modified. Only an undefined variable that is
 * not a cell is an error in the exit condition.
 */
class TestFlowCellShadowing {

    private Object run(String source) {
        try (final var interpreter = new Interpreter(source)) {
            return interpreter.compileAndExecute();
        }
    }

    @Test
    void cellMayShadowLocalVariableOfSurroundingScope() {
        // without cell locality the exit condition would read the outer 'finished' (true)
        // during warm-up and exit immediately with a wrong result
        final var e = run("""
                let finished : bool = true;
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
        assertTrue((long)e >= 5);
    }

    @Test
    void cellMayShadowGlobalVariable() {
        final var e = run("""
                global finished
                finished = true
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
        assertTrue((long)e >= 5);
    }

    @Test
    void undefinedNonCellVariableInExitConditionIsAnError() {
        // a typo in the exit condition must not be tolerated as warm-up
        final var e = assertThrows(ExecutionException.class, () -> run("""
                let r = {
                    flow until finishedd == true {
                        counter <- 0;
                        counter <- counter + 1;
                        finished <- counter >= 5;
                        yield counter
                    }
                }
                r
                """));
        assertTrue(e.getMessage().contains("Variable 'finishedd' is not defined in flow"),
                "unexpected message: " + e.getMessage());
    }

    @Test
    void exitConditionMayUseNonCollidingSurroundingVariables() {
        final var result = run("""
                let threshold = 5
                let r = {
                    flow until counter >= threshold {
                        counter <- 0;
                        counter <- counter + 1;
                        yield counter
                    }
                }
                r
                """);
        assertEquals(5L, result);
    }

    @Test
    void exitConditionToleratesNotYetComputedCellsDuringWarmup() {
        // 'b' is undefined at the first evaluation of the exit condition
        final var result = run("""
                let r = {
                    flow until b >= 2 {
                        a <- 1;
                        b <- a * 2;
                        yield b
                    }
                }
                r
                """);
        assertEquals(2L, result);
    }
}
