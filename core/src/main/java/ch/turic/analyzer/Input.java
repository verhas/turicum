package ch.turic.analyzer;

import java.util.Objects;

import static ch.turic.utils.Require.require;

public final class Input implements ch.turic.Input, CharSequence {

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
    public int select(final String... s) {
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
            move(1, output);
        }
        return output.toString();
    }


    /**
     * Checks if the internal {@code builder} starts with the given string.
     * 
     * @param s the string to check against the start of the {@code builder}'s content
     * @return true if the builder starts with the given string, false otherwise
     * @throws NullPointerException if {@code s} is {@code null}          
     */
    public boolean startsWith(String s) {
        Objects.requireNonNull(s);
        return s.length() <= builder.length() && builder.subSequence(0, s.length()).equals(s);
    }

    /**
     * Checks if the internal {@code builder} starts with any of the given strings.
     *
     * @param s one or more strings to check against the start of the {@code builder}'s content
     * @return true if the builder starts with any of the given strings, false otherwise
     * @throws NullPointerException if {@code s} or any of its elements are {@code null}
     */
    public boolean startsWithEither(String... s) {
        Objects.requireNonNull(s);
        return select(s) != -1;
    }


    /**
     * Checks if the internal {@code builder} starts with the given string, ignoring case.
     *
     * @param s the string to check against the start of the {@code builder}'s content
     * @return true if the builder starts with the given string (ignoring case), false otherwise
     * @throws NullPointerException if {@code s} is {@code null}
     */
    public boolean startsWithIgnoreCase(String s) {
        Objects.requireNonNull(s);
        return s.length() <= builder.length() &&
                builder.subSequence(0, s.length()).toString().equalsIgnoreCase(s);
    }

    public String fetchNumber() {
        final var output = new StringBuilder();
        while (length() > 0 && (Character.isDigit(charAt(0)) || charAt(0) == '_')) {
            move(1, output);
        }
        return output.toString();
    }

    public void skip(int numberOfCharacters) {
        require(() -> numberOfCharacters > 0, "numberOfCharacters must be non-negative");
        if (builder.charAt(0) == '\n') {
            position.line++;
            position.column = 0;
        } else {
            position.column += numberOfCharacters;
        }
        builder.delete(0, numberOfCharacters);
    }

    /**
     * Moves a specified number of characters from the internal {@code builder} to the provided {@code target}.
     * Appends the specified number of characters from the beginning of the {@code builder} to {@code target},
     * and then skips those characters in the internal {@code builder}.
     *
     * @param numberOfCharacters the number of characters to move and skip; must be non-negative
     * @param target             the {@code StringBuilder} to which the characters are appended
     * @throws IllegalArgumentException if {@code numberOfCharacters} is less than or equal to 0
     */
    public void move(int numberOfCharacters, final StringBuilder target) {
        require(() -> numberOfCharacters > 0, "numberOfCharacters must be non-negative");
        target.append(builder, 0, numberOfCharacters);
        skip(numberOfCharacters);
    }

    public void try_move(int numberOfCharacters, final StringBuilder target) {
        require(() -> numberOfCharacters > 0, "numberOfCharacters must be non-negative");
        final var z = Math.min(builder.length(), numberOfCharacters);
        if (z > 0) {
            target.append(builder, 0, z);
            skip(z);
        }
    }

    /**
     * Checks if the given character is a valid first character for a Java identifier.
     * <p>
     * It is the same as in Java, with the exception that '$' is not a valid identifier start character.
     *
     * @param c the character to check
     * @return {@code true} if the character is a valid starting character for a Java identifier,
     * {@code false} otherwise
     */
    static boolean validId1stChar(char c) {
        return Character.isJavaIdentifierStart(c) && c != '$';
    }

    /**
     * @param c the character to check
     * @return {@code true} if the character can be used in a lazy identifier. These are the same characters that can
     * be used as first characters (see {@link #validId1stChar(char)}) and also digits.
     */
    static boolean validIdChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
