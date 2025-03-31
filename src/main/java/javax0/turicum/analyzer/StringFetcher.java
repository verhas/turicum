package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;

/**
 * A utility class that handles string parsing from input, supporting both quoted and unquoted strings,
 * as well as multi-line strings delimited by triple quotes.
 */
public class StringFetcher {
    public static final String MULTI_LINE_STRING_DELIMITER = "\"\"\"";
    private static final int MLSD_LENGTH = MULTI_LINE_STRING_DELIMITER.length();
    private static final char ENCLOSING_CH = '"';
    private static final char IDENTIFIER_CH = '`';

    /**
     * Gets a string from the input
     *
     * @param input The input to parse from
     * @return The parsed string
     * @throws BadSyntax If there is a syntax error while parsing
     */
    public static String getString(Input input) throws BadSyntax {
        BadSyntax.when(input.length() < 2, "String has to be at least two characters long.");
        if (input.length() >= MLSD_LENGTH && input.subSequence(0, MLSD_LENGTH).equals(MULTI_LINE_STRING_DELIMITER)) {
            return getMultiLineString(input);
        } else {
            return getSimpleString(input);
        }
    }

    /**
     * Parses a multi-line string delimited by triple quotes, handling escape sequences.
     *
     * @param input The input to parse from
     * @return The parsed multi-line string
     * @throws BadSyntax If the multi-line string is not properly terminated
     */
    private static String getMultiLineString(Input input) throws BadSyntax {
        final var output = new StringBuilder();
        input.skip(MLSD_LENGTH);
        while (input.length() >= MLSD_LENGTH && !input.subSequence(0, MLSD_LENGTH).equals(MULTI_LINE_STRING_DELIMITER)) {
            final char ch = input.charAt(0);
            if (ch == '\\') {
                Escape.handleEscape(input, output);
            } else {
                Escape.handleNormalMultiLineStringCharacter(input, output);
            }
        }
        BadSyntax.when(input.length() < MLSD_LENGTH, "Multi-line string is not terminated before eof");
        input.skip(MLSD_LENGTH);
        return output.toString();
    }

    /**
     * Parses a simple quoted string, handling escape sequences.
     *
     * @param input The input to parse from
     * @return The parsed string
     * @throws BadSyntax If the string is not properly terminated
     */
    private static String getSimpleString(Input input) throws BadSyntax {
        return getString(input, ENCLOSING_CH);
    }

    private static String getString(Input input, char enclosingCh) throws BadSyntax {
        final var output = new StringBuilder();
        input.skip(1);
        while (!input.isEmpty() && input.charAt(0) != enclosingCh) {
            final char ch = input.charAt(0);
            if (ch == '\\') {
                Escape.handleEscape(input, output);
            } else {
                Escape.handleNormalCharacter(input, output);
            }
        }
        BadSyntax.when(input.isEmpty(), "String is not terminated before eol");
        input.skip(1);
        return output.toString();
    }

    public static String fetchId(Input input) throws BadSyntax {
        return getString(input, IDENTIFIER_CH);
    }

}
