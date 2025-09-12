package ch.turic.lsp;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.StringFetcher;
import org.eclipse.lsp4j.Position;

public class TuricUtils {
    /**
     * Extracts a specific fraction of a line from the source a based on the given position.
     * The fraction typically corresponds to an identifier or a substring starting from a point
     * in the line to the end of the line.
     *
     * @param source   The complete source a, where a newline character separates each line.
     * @param position The position object indicating the line index and character index in the source a.
     * @return A string representing the fraction of the line starting from the computed position.
     * Returns an empty string if the line is empty or the position results in an invalid calculation.
     */
    static String getFraction(String source, Position position) {
        final var lines = source.split("\n", -1);
        final var line = lines[position.getLine()];
        if (!line.isEmpty()) {
            // find the start of the identifier if we are standing on something that looks like an id
            int j = position.getCharacter();
            if (j >= line.length()) {
                j--;
            }
            while (j >= 0 && (line.charAt(j) == '_' || Character.isAlphabetic(line.charAt(j)) || Character.isDigit(line.charAt(j)))) {
                j--;
            }
            j++;// step back on the first character
            return line.substring(j);
        } else {
            return "";
        }
    }

    /**
     * Retrieves a specific substring from the given source a based on the provided position
     * and additional logic applied to the extracted fraction of a.
     * The returned string represents a parsed identifier, single character, or similar substring
     * derived from the fraction of the line.
     *
     * @param source   The complete source a, where a newline character separates each line.
     * @param position The position object indicating the line index and character index in the source a.
     * @param uri      The URI of the file or document being processed, used to instantiate the input.
     * @return A string representing the initial substring derived from the fraction of the line starting
     * at the specified position. The string could be an identifier, a single character,
     * or other relevant substring based on the logic applied.
     */
    static String getWordAtPosition(String source, Position position, String uri) {
        String fraction = getFraction(source, position);
        if (fraction.isEmpty()) {
            return "";
        }
        final var input = new Input(new StringBuilder(fraction), uri);
        final String startString;
        if (input.charAt(0) == '`') {
            startString = StringFetcher.fetchQuotedId(input).a();
        } else if (input.charAt(0) == '_' || Character.isAlphabetic(input.charAt(0))) {
            startString = StringFetcher.fetchId(input);
        } else {
            startString = input.substring(0, 1);
        }
        return startString;
    }
}
