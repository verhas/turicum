package ch.turic.utils;

import ch.turic.BadSyntax;
import ch.turic.analyzer.Input;
import ch.turic.analyzer.Pos;

public class UnexpectedCharacter extends BadSyntax {

    /**
     * Constructs a new BadSyntax exception with detailed error information.
     *
     * @param position The position where the syntax error occurred
     * @param input   the input where the string was not terminated
     */
    public UnexpectedCharacter(Pos position, Input input) {
        super(position, "Unexpected character '%s' in the input",
                input == null ? "%NULL_INPUT%" :
                        !input.isEmpty() ? input.charAt(0) : "%EMPTY_INPUT%");
    }
}
