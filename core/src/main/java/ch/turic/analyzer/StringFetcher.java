package ch.turic.analyzer;


import ch.turic.exceptions.BadSyntax;
import ch.turic.utils.Require;

/**
 * A utility class that handles string parsing from input, supporting both quoted and unquoted strings,
 * as well as multi-line strings delimited by triple quotes.
 */
public class StringFetcher {
    public static final String MULTI_LINE_STRING_DELIMITER = "\"\"\"";
    private static final int MLSD_LENGTH = MULTI_LINE_STRING_DELIMITER.length();
    private static final char ENCLOSING_CH = '"';
    private static final char IDENTIFIER_CH = '`';

    public record Pair(String a, String b) {
        public static Pair of(CharSequence a, CharSequence b) {
            return new Pair(a.toString(), b.toString());
        }
    }

    /**
     * Gets a string from the input
     *
     * @param input The input to parse from
     * @return The parsed string
     * @throws BadSyntax If there is a syntax error while parsing
     */
    public static String getString(Input input) throws BadSyntax {
        return getPair(input).a;
    }

    public static Pair getPair(Input in) throws BadSyntax {
        BadSyntax.when(in.position, in.length() < 2, "String has to be at least two characters long.");
        if (in.startsWith(MULTI_LINE_STRING_DELIMITER)) {
            return getMultiLineString(in);
        } else {
            return getSimpleString(in);
        }
    }


    /**
     * Parses a multi-line string delimited by triple quotes, handling escape sequences.
     *
     * @param in The input to parse from
     * @return The parsed multi-line string
     * @throws BadSyntax If the multi-line string is not terminated correctly
     */
    private static Pair getMultiLineString(Input in) throws BadSyntax {
        final var text = new StringBuilder();
        final var lexeme = new StringBuilder();
        in.move(MLSD_LENGTH, lexeme);
        while (in.length() >= MLSD_LENGTH && !in.startsWith(MULTI_LINE_STRING_DELIMITER)) {
            if (in.startsWith("\\")) {
                Escape.handleEscape(in, text, lexeme);
            } else {
                Escape.handleNormalMultiLineStringCharacter(in, text, lexeme);
            }
        }
        BadSyntax.when(in.position, in.length() < MLSD_LENGTH, "Multi-line string is not terminated before eof");
        in.move(MLSD_LENGTH, lexeme);
        return new Pair(text.toString(), lexeme.toString());
    }

    /**
     * Parses a simple quoted string, handling escape sequences.
     *
     * @param in The input to parse from
     * @return The parsed string
     * @throws BadSyntax If the string is not terminated correctly
     */
    private static Pair getSimpleString(Input in) throws BadSyntax {
        final var text = new StringBuilder();
        final var lexeme = new StringBuilder();
        in.move(1, lexeme);
        while (!in.isEmpty() && in.charAt(0) != ENCLOSING_CH) {
            final char ch = in.charAt(0);
            if (ch == '\\') {
                Escape.handleEscape(in, text, lexeme);
            } else {
                Escape.handleNormalCharacter(in, text, lexeme);
            }
        }
        BadSyntax.when(in.position, in.isEmpty(), "String is not terminated before eol");
        in.move(1, lexeme);
        return Pair.of(text, lexeme);
    }

    /**
     * Fetches an identifier from the given input. The identifier starts with a valid
     * Java identifier start-character (excluding '$') and is followed by a valid Java
     * identifier part characters.
     * <p>
     * Since the identifier can not have any escapes or other shenanigans in it, the return value
     * is the identifier and the lexeme at the same time; there is no need to return a pair.
     *
     * @param in The input from which to parse the identifier. It must not be empty and must meet
     *           the constraints of valid Java identifier characters.
     * @return The parsed identifier as a string.
     */
    public static String fetchId(Input in) {
        Require.require(!in.isEmpty(), "Input must be at least one character long.");
        Require.require(ch.turic.Input.validId1stChar(in.charAt(0)), "Input must start with a valid identifier character.");
        final var id = new StringBuilder();
        while (!in.isEmpty() && ch.turic.Input.validIdChar(in.charAt(0))) {
            in.move(1, id);
        }
        return id.toString();
    }

    /**
     * Parses a quoted identifier from the given input, handling escape sequences.
     * A specific character encloses the identifier and supports escape handling
     * to allow embedded special characters within the identifier.
     *
     * @param input The input from which to parse the quoted identifier. It should not be empty
     *              and should start and end with the appropriate enclosing character.
     * @return A pair where the first element is the parsed quoted identifier
     * and the second element is the complete lexeme representing the parsed identifier.
     * @throws BadSyntax If the quoted identifier is not terminated correctly or if invalid syntax
     *                   is encountered during parsing.
     */
    public static Pair fetchQuotedId(Input input) throws BadSyntax {
        final var identifier = new StringBuilder();
        final var lexeme = new StringBuilder();
        input.move(1, lexeme);
        while (!input.isEmpty() && input.charAt(0) != IDENTIFIER_CH) {
            final char ch = input.charAt(0);
            if (ch == '\\') {
                Escape.handleEscape(input, identifier, lexeme);
            } else {
                Escape.handleNormalCharacter(input, identifier, lexeme);
            }
        }
        BadSyntax.when(input.position, input.isEmpty(), "String is not terminated before eol");
        input.move(1, lexeme);
        return Pair.of(identifier, lexeme);
    }

}
