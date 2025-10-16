package ch.turic.exceptions;

import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Pos;

import java.util.function.Supplier;

/**
 * Runtime exception for reporting syntax errors in the Turi language with detailed positional information.
 * Provides context about where the syntax error occurred, including file location and surrounding code.
 */
public class BadSyntax extends RuntimeException {
    private final Pos position;

    public Pos getPosition() {
        return position;
    }

    /**
     * Constructs a new BadSyntax exception with detailed error information.
     *
     * @param position The position where the syntax error occurred
     * @param s        The error message format string
     * @param params   The parameters to be formatted into the message
     */
    public BadSyntax(Pos position, String s, Object... params) {
        super(format(position, s, params));
        this.position = position;
    }

    /**
     * Formats the error message with contextual information about the error location.
     * Shows up to 3 lines of code before the error position and marks the exact column with a caret.
     *
     * @param position        The position information about where the error occurred
     * @param errorMessageFmt The message format string
     * @param params          The parameters to be formatted into the message
     * @return A formatted error message with context
     */
    private static String format(Pos position, String errorMessageFmt, Object... params) {
        final var start = position != null && position.line >= 3 ? position.line - 3 : 0;
        final var sb = new StringBuilder();
        for (int i = start; position != null && i < position.line; i++) {
            sb.append(String.format("\n%3d. %s", i + 1, position.lines[i]));
        }

        return String.format(errorMessageFmt, params) +
                (position == null ? "" :
                        (String.format("\nat %s:%d:%d", position.file, position.line, position.column)
                                + sb
                                + String.format("\n   %s^", "-".repeat(position.column))));

    }

    /**
     * Throws a BadSyntax exception when the given condition is true using lexer position information.
     *
     * @param lexes      The lexer containing position information
     * @param b          The condition that triggers the exception when true
     * @param msg        The error message format string
     * @param parameters The parameters to be formatted into the message
     * @throws BadSyntax when the condition is true
     */
    public static void when(LexList lexes, final boolean b, String msg, Object... parameters) throws BadSyntax {
        when(lexes.startPosition(), b, msg, parameters);
    }

    /**
     * Throws a BadSyntax exception when the given condition is true using explicit position information.
     *
     * @param position   The position information for the error
     * @param b          The condition that triggers the exception when true
     * @param msg        The error message format string
     * @param parameters The parameters to be formatted into the message
     * @throws BadSyntax when the condition is true
     */
    public static void when(Pos position, final boolean b, String msg, Object... parameters) throws BadSyntax {
        if (b) {
            throw new BadSyntax(position, msg, parameters);
        }
    }

    /**
     * Throws a BadSyntax exception when the given condition is true using a message supplier.
     *
     * @param position The position information for the error
     * @param b        The condition that triggers the exception when true
     * @param msg      A supplier that provides the error message
     * @throws BadSyntax when the condition is true
     */
    public static void when(Pos position, final boolean b, Supplier<String> msg) throws BadSyntax {
        if (b) {
            throw new BadSyntax(position, msg.get());
        }
    }
}
