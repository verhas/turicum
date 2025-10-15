package ch.turic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface Input permits ch.turic.analyzer.Input{

    static ch.turic.analyzer.Input fromString(final String s) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), "none");
    }

    static ch.turic.analyzer.Input fromString(final String s, String fn) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), fn);
    }

    static ch.turic.analyzer.Input fromFile(final Path path) throws IOException {
        return new ch.turic.analyzer.Input(new StringBuilder(Files.readString(path, StandardCharsets.UTF_8)), path.normalize().toAbsolutePath().toString());
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
    public static boolean validId1stChar(char c) {
        return Character.isJavaIdentifierStart(c) && c != '$';
    }

    /**
     * @param c the character to check
     * @return {@code true} if the character can be used in a lazy identifier. These are the same characters that can
     * be used as first characters (see {@link #validId1stChar(char)}) and also digits.
     */
    public static boolean validIdChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }

}
