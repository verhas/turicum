package javax0.turicum;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

public class TuriTest {

    static Result input(String input) {
        return new Result(input);
    }

    public record Result(String input) {

        public void shouldResultIn(Object expected) {
            try {
                final var result = new Interpreter(input).execute();
                Assertions.assertEquals(expected, result);
            } catch (BadSyntax e) {
                throw new AssertionFailedError("Bad syntax", e);
            }
        }

    }

}
