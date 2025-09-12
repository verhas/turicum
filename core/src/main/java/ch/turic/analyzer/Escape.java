package ch.turic.analyzer;


import ch.turic.BadSyntax;

/**
 * This class provides utility methods to handle escape sequences, process normal characters,
 * and normalize newlines in an input stream. It is designed to process and manipulate characters
 * in a controlled manner, such as handling escape sequences in strings and normalizing line endings.
 */
class Escape {
    private static char octal(Input in, int maxLen, StringBuilder lexeme) {
        int i = maxLen;
        int occ = 0;
        while (i > 0 && !in.isEmpty() && in.charAt(0) >= '0' && in.charAt(0) <= '7') {
            occ = 8 * occ + in.charAt(0) - '0';
            in.move(1, lexeme);
            i--;
        }
        return (char) occ;
    }

    private static char hex(final Input in, final int maxLen, final StringBuilder lexeme) {
        int i = maxLen;
        int hx = 0;
        while (i > 0 && !in.isEmpty() && ((in.charAt(0) >= '0' && in.charAt(0) <= '9') || (in.charAt(0) >= 'A' && in.charAt(0) <= 'F') || (in.charAt(0) >= 'a' && in.charAt(0) <= 'f'))) {
            int ch = in.charAt(0);
            in.move(1,lexeme);
            hx = 16 * hx;
            if (ch >= '0' && ch <= '9') {
                hx += ch - '0';
            } else if (ch >= 'A' && ch <= 'F') {
                hx += ch - 'A' + 10;
            } else if (ch >= 'a' && ch <= 'f') {
                hx += ch - 'a' + 10;
            }
            i--;
        }
        return (char) hx;
    }

    private static final String escapes = "btnfr\"'\\`";
    private static final String escaped = "\b\t\n\f\r\"'\\`";

    /**
     * Handle the escape sequence. The escape sequence is
     *
     * <ul>
     *      <li>backslash and a 'b', 't', 'n', 'f', 'r', '"', '\' or an apostrophe, or</li>
     *      <li>backslash and 2 or 3 octal characters.</li>
     * </ul>
     *
     * @param input  the input string
     * @param text the output string where the escaped character is appended
     * @param lexeme the b string where the escaped character is appended, including the escape
     * @throws BadSyntax if the escape sequence is invalid
     */
    static void handleEscape(Input input, StringBuilder text, StringBuilder lexeme) throws BadSyntax {
        input.move(1, lexeme);
        BadSyntax.when(input.position, input.isEmpty(), "Source ended inside a string.");
        final var nextCh = input.charAt(0);
        final int esindex = escapes.indexOf(nextCh);
        if (esindex == -1) {
            if (nextCh >= '0' && nextCh <= '3') {
                text.append(octal(input, 3, lexeme));
            } else if (nextCh >= '4' && nextCh <= '7') {
                text.append(octal(input, 2, lexeme));
            } else if (nextCh == 'u') {
                input.move(1, lexeme);
                text.append(hex(input, 4, lexeme));
            } else {
                throw new BadSyntax(input.position, "Invalid escape sequence in string: \\" + nextCh);
            }
        } else {
            text.append(escaped.charAt(esindex));
            input.move(1, lexeme);
        }
    }

    static void handleNormalCharacter(Input input, StringBuilder text, StringBuilder lexeme) throws BadSyntax {
        final char ch = input.charAt(0);
        BadSyntax.when(input.position, ch == '\n' || ch == '\r', () -> String.format("String not terminated before eol:\n%s...",
                input.substring(1, Math.min(input.length(), 60))));
        text.append(ch);
        lexeme.append(ch);
        input.skip(1);
    }

    static void handleNormalMultiLineStringCharacter(Input input, StringBuilder text, StringBuilder lexeme) {
        char ch = input.charAt(0);
        if (ch == '\n' || ch == '\r') {
            normalizedNewLines(input, text, lexeme);
        } else {
            text.append(ch);
            lexeme.append(ch);
            input.skip(1);
        }
    }

    private static void normalizedNewLines(Input input, StringBuilder text) {
        normalizedNewLines(input, text, new StringBuilder());
    }

    /**
     * <p>Convert many subsequent {@code \n} and {@code \r} characters to {@code \n} only. There will be as many {@code
     * \n} characters in the output as many there were in the input and the {@code \r} characters are simply ignored.
     * The only exception is, when there are no {@code \n} characters. In this case there will be one {@code \n} in the
     * output for all the {@code \r} characters.</p>
     *
     * <p>The method deletes the characters from the start of the input {@code input} and append the output
     * to the {@code output}. The original characters will be appended to the end of {@code original} without any
     * conversion.</p>
     *
     * @param input the input, from which the characters are consumed.
     * @param text  where the converted newline(s) are appended to
     */
    private static void normalizedNewLines(Input input, StringBuilder text, StringBuilder lexeme) {
        char ch = input.charAt(0);
        int countNewLines = 0;
        while (!input.isEmpty() && (ch == '\n' || ch == '\r')) {
            if (ch == '\n') {
                countNewLines++;
            }
            lexeme.append(ch);
            input.skip(1);
            if (!input.isEmpty()) {
                ch = input.charAt(0);
            }
        }
        // if there was a single, or multiple \r without any \n
        if (countNewLines == 0) {
            countNewLines++;
        }
        text.append("\n".repeat(countNewLines));
    }

}
