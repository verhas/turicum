package ch.turic.analyzer;

public class Input implements ch.turic.Input, CharSequence {

    public final Pos position;
    private final StringBuilder builder;

    public Input(StringBuilder builder, String fn) {
        final var lines = builder.toString().split("\n", -1);
        position = new Pos(fn, lines);
        this.builder = builder;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public String substring(int start, int end) {
        return builder.substring(start, end);
    }

    @Override
    public boolean isEmpty() {
        return builder.isEmpty();
    }

    @Override
    public int length() {
        return builder.length();
    }

    @Override
    public char charAt(int index) {
        return builder.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return builder.subSequence(start, end);
    }


    /**
     * Checks if the internal {@code builder} starts with any of the given strings.
     * <p>
     * Iterates through the provided array of strings and checks if the content of
     * the {@code builder} starts with any of them. Returns the index of the first
     * matching string. If none match, returns -1.
     * </p>
     *
     * @param s one or more strings to check against the start of the {@code builder}'s content
     * @return the index of the first string that the {@code builder} starts with;
     * -1 if no strings match
     * @throws NullPointerException      if {@code s} or any of its elements are {@code null}
     * @throws IndexOutOfBoundsException if any string in {@code s} is longer than the builder's content
     */
    public int startsWith(final String... s) {
        for (int i = 0; i < s.length; i++) {
            if (s[i].length() <= builder.length() && builder.subSequence(0, s[i].length()).equals(s[i])) return i;
        }
        return -1;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    public String fetchHexNumber() {
        final var output = new StringBuilder();
        while (length() > 0 && isHex(charAt(0))) {
            output.append(charAt(0));
            skip(1);
        }
        return output.toString();
    }


    public String fetchNumber() {
        final var output = new StringBuilder();
        while (length() > 0 && (Character.isDigit(charAt(0)) || charAt(0) == '_')) {
            output.append(charAt(0));
            skip(1);
        }
        return output.toString();
    }

    public String fetchId() {
        final var output = new StringBuilder();
        if (length() > 0 && validId1stChar(charAt(0))) {
            while (length() > 0 && validIdChar(charAt(0))) {
                output.append(charAt(0));
                skip(1);
            }
        } else {
            while (length() > 0 && !Character.isWhitespace(charAt(0))) {
                output.append(charAt(0));
                skip(1);
            }
        }
        return output.toString();
    }

    public void skip(int numberOfCharacters) {
        if (builder.charAt(0) == '\n') {
            position.line++;
            position.column = 0;
        } else {
            position.column += numberOfCharacters;
        }
        builder.delete(0, numberOfCharacters);
    }

    /**
     * Determines if a character is valid as the first character of an identifier.
     *
     * Only underscores and alphabetic characters are considered valid.
     *
     * @param c the character to check
     * @return true if the character is an underscore or an alphabetic character; false otherwise
     */
    static boolean validId1stChar(char c) {
        return c == '_' || Character.isAlphabetic(c);
    }

    /**
     * @param c the character to check
     * @return {@code true} if the character can be used in a lazy identifier. These are the same characters that can
     * be used as first characters (see {@link #validId1stChar(char)}) and also digits.
     */
    static boolean validIdChar(char c) {
        return validId1stChar(c) || Character.isDigit(c);
    }
}
