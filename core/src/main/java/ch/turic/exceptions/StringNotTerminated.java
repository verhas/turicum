package ch.turic.exceptions;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.Pos;

public class StringNotTerminated extends BadSyntax {

    public static final int MAX_PREVIEW_LENGTH = 60;

    /**
     * Constructs a new BadSyntax exception with detailed error information.
     *
     * @param position The position where the syntax error occurred
     * @param input    the input where the string was not terminated
     */
    public StringNotTerminated(Pos position, Input input) {
        super(position, "String not terminated before eol:\n%s...",
                input == null ? "%NULL_INPUT%" :
                        input.isEmpty() ? "%EMPTY_INPUT%" :
                                input.substring(0, Math.min(input.length(), MAX_PREVIEW_LENGTH)));
    }
}
