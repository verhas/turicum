package javax0.turicum.analyzer;

public class Input implements CharSequence {

    private final StringBuilder builder;

    public Input(StringBuilder builder) {
        this.builder = builder;
    }

    public static Input fromString(final String s) {
        return new Input(new StringBuilder(s));
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

    public String fetchNumber() {
        final var output = new StringBuilder();
        while (length() > 0 && Character.isDigit(charAt(0))) {
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
        builder.delete(0, numberOfCharacters);
    }

    /**
     * @param c the character to check
     * @return {@code true} if the character can be used as the first character of a macro identifier. Currently, these
     * are {@code $}, {@code _} (underscore), {@code :} (colon) and any alphabetic character.
     */
    static boolean validId1stChar(char c) {
        return c == '_' || Character.isAlphabetic(c);
    }

    /**
     * @param c the character to check
     * @return {@code true} if the character can be used in a macro identifier. These are the same characters that can
     * be used as first characters (see {@link #validId1stChar(char)}) and also digits.
     */
    static boolean validIdChar(char c) {
        return validId1stChar(c) || Character.isDigit(c);
    }
}
