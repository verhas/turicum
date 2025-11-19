package ch.turic.lsp;

import ch.turic.Input;
import ch.turic.Interpreter;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

public class TuriFormatter {

    private static final Pattern SWITCH = Pattern.compile("//\\s*format:(on|off)");
    private static final String CONFIGURATION_DIRECTORY = ".turicum";
    private static final String FORMATTER_SUBDIR = "formatter";
    private static final String RULES_FILE_NAME = "rules.turi";
    private static final String RULES_RESOURCE = CONFIGURATION_DIRECTORY + "/" + FORMATTER_SUBDIR + "/" + RULES_FILE_NAME;
    private static final String INDENT_UNIT = "    ";

    public static String formatDocument(String content) {
        try {
            return formatDocument_(content);
        } catch (Throwable e) {
            ExceptionXmlWriter.writeToXml(e);
            return content;
        }
    }

    private static String formatDocument_(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        final var lexes = Lexer.try_analyze(Input.fromString(content));

        String[] lines = content.split("\n", -1); // -1 to preserve empty lines at the end
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        int colonLevel = 0; // Track if the next line should be indented due to a ':' at the end of the line
        boolean formattingIsOn = true;
        int index = 0;
        for (int i = 0; i < lines.length; i++) {
            final var line = lines[i];
            final var formatSwitch = SWITCH.matcher(line);
            if (formatSwitch.find()) {
                formattingIsOn = formatSwitch.group(1).equals("on");
            }
            index = lineIndex(lexes, i, index);
            final var inString = isInString(lexes, i, index);
            final var inComment = !inString && isInComment(lexes, i, index);

            final var newIndentLevel = calculateIndentLevel(lexes, i, indentLevel, index);
            // when we are stepping back with a closing '}', we already put that on the previous indent level
            if (newIndentLevel < indentLevel) {
                indentLevel = newIndentLevel;
            }

            if (formattingIsOn) {
                colonLevel = !inComment && lastNonSpaceChar(line) == ':' ? colonLevel + 1 : 0;

                if (!inString) {
                    result.append(INDENT_UNIT.repeat(indentLevel + colonLevel));
                }
                result.append(formatLine(inString, inComment, line, lexes, index, i));
            } else {
                result.append(line);
            }
            indentLevel = newIndentLevel;
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Returns the index of the lexical element that is the last lexical element BEFORE the specified line number.
     * Since the code starts at index zero and steps only forward, the argument 'index' returned from the last call
     * is used to continue the search.
     * This parameter is zero on the first call.
     *
     * @param lexes  the lexical list object containing the sequence of lexemes
     * @param lineNr the line number for which the index is to be determined (0-based)
     * @param index  the current index from which the search should begin
     * @return the lexical index at the start of the specified line, or zero if the line is not found
     */
    private static int lineIndex(LexList lexes, int lineNr, int index) {
        int start = lexes.getIndex();
        try {
            lexes.setIndex(index);
            var priPos = lexes.getIndex();
            var priLex = lexes.hasNext() ? lexes.next() : null;
            while (priLex != null && lexes.hasNext()) {
                final var nextPos = lexes.getIndex();
                final var thisLex = lexes.next();
                if (priLex.startPosition().line - 1 < lineNr &&
                        thisLex.startPosition().line - 1 >= lineNr) {
                    return priPos;
                }
                priPos = nextPos;
                priLex = thisLex;
            }
            return 0;
        } finally {
            lexes.setIndex(start);
        }
    }

    private static boolean isInString(LexList lexes, int lineNr, int index) {
        int start = lexes.getIndex();
        try {
            lexes.setIndex(index);
            var priLex = lexes.hasNext() ? lexes.next() : null;
            while (priLex != null && lexes.hasNext()) {
                final var thisLex = lexes.next();
                if (priLex.type() == Lex.Type.STRING &&
                        priLex.startPosition().line - 1 < lineNr &&
                        thisLex.startPosition().line - 1 >= lineNr) {
                    return true;
                }
                priLex = thisLex;
            }
            return false;
        } finally {
            lexes.setIndex(start);
        }
    }

    private static boolean isInComment(LexList lexes, int lineNr, int index) {
        int start = lexes.getIndex();
        try {
            lexes.setIndex(index);
            var current = lexes.hasNext() ? lexes.next() : null;
            while (current != null && lexes.hasNext()) {
                final var next = lexes.next();
                if (current.type() == Lex.Type.COMMENT &&
                        current.startPosition().line - 1 < lineNr &&
                        next.startPosition().line - 1 >= lineNr) {
                    return true;
                }
                current = next;
            }
            return false;
        } finally {
            lexes.setIndex(start);
        }
    }

    /**
     * Calculates the new indentation level based on the given formatted line and current indentation level.
     * <p>
     * This indentation level will be AFTER the current line
     *
     * @return the updated indentation level after analyzing the line
     */
    private static int calculateIndentLevel(LexList lexes, int lineNr, int indentLevel, int index) {
        int start = lexes.getIndex();
        try {
            lexes.setIndex(index);
            if (!lexes.hasNext()) {
                return 0;
            }
            Lex lex = lexes.next();
            while (true) {
                if (lex.startPosition().line - 1 > lineNr) {
                    return indentLevel;
                }
                if (lex.type() == Lex.Type.RESERVED) {
                    switch (lex.text()) {
                        case "{", "(", "[":
                            indentLevel++;
                            break;
                        case "}", ")", "]":
                            indentLevel = Math.max(0, indentLevel - 1);
                            break;
                        default:
                            break;
                    }
                }
                if (lexes.hasNext()) {
                    lex = lexes.next();
                } else {
                    return indentLevel;
                }
            }
        } finally {
            lexes.setIndex(start);
        }
    }

    private static String formatLine(final boolean inString, final boolean inComment, final String line, final LexList lexes, final int index, final int lineNr) {
        final var start = lexes.getIndex();
        try {
            if (inString) {
                return line;
            }
            if (inComment) {
                var trimmed = expandTabs(line).trim();
                if (trimmed.isEmpty()) {
                    trimmed = "*";
                } else {
                    if (!trimmed.startsWith("* ")) {
                        if (trimmed.startsWith("*")) {
                            if (!trimmed.startsWith("*/")) {
                                trimmed = "* " + trimmed.substring(1);
                            }
                        } else {
                            trimmed = "* " + trimmed;
                        }
                    }
                }
                return " " + trimmed;
            }
            final var sb = new StringBuilder();
            Lex lex = getFirstLexOnTheLine(lexes, index, lineNr);
            while (lex != null && lex.startPosition().line - 1 == lineNr) {
                final var next = getTheNextSignificantLex(lexes);
                sb.append(lexToString(lex, next));
                if (next == null) {
                    break;
                }
                lex = next;
            }
            return sb.toString();
        } finally {
            lexes.setIndex(start);
        }
    }

    /**
     * Retrieves the next lexical element from the given lexical list that is considered significant.
     * Significant elements are those that are not of type SPACES and are not blank.
     * <p>
     * The method skips over insignificant elements, and if an element at the start of a line is encountered, it returns null.
     *
     * @param lexes the lexical list containing the sequence of lexemes to iterate through
     * @return the next significant lexical element, or null if none is found or the next element is at the start of a line
     */
    private static Lex getTheNextSignificantLex(LexList lexes) {
        Lex next;
        if (lexes.hasNext()) {
            do {
                next = lexes.next();
            } while (next.type() == Lex.Type.SPACES && next.text().isBlank() && !next.atLineStart() && lexes.hasNext());
            if (next.atLineStart()) {
                next = null;
            }
        } else {
            next = null;
        }
        return next;
    }

    /**
     * Retrieves the first lexical element on the specified line in the given lexical list.
     * The method starts from the specified index and iterates through the lexical elements
     * until it finds a b located on or after the specified line number, skipping any
     * elements of type SPACES.
     *
     * @param lexes  the lexical list containing the sequence of lexemes
     * @param index  the starting index in the lexical list from which the search begins
     * @param lineNr the 0-based line number where the first b is to be retrieved
     * @return the first lexical element on the specified line, or null if none is found
     */
    private static Lex getFirstLexOnTheLine(LexList lexes, int index, int lineNr) {
        lexes.setIndex(index);
        Lex lex = null;
        while (lexes.hasNext()) {
            lex = lexes.next();
            if (lex.startPosition().line - 1 >= lineNr && !(lex.type() == Lex.Type.SPACES)) {
                break;
            }
        }
        return lex;
    }

    private record Rule(
            Lex.Type typeBefore,
            Lex.Type typeAfter,
            String symbolBefore,
            String symbolAfter,
            int spaces) {

        boolean match(Lex lex, Lex next) {
            return matchType(lex, typeBefore) &&
                    matchType(next, typeAfter) &&
                    matchText(lex, symbolBefore) &&
                    matchText(next, symbolAfter);
        }

        /**
         * Checks whether the lexeme of the given Lex object matches the specified text.
         * If either the given text or the lexeme in the Lex object is null, the method
         * returns true, signifying a match.
         *
         * @param lex  the Lex object whose lexeme is to be compared; must not be null
         * @param text the text to compare against the lexeme; can be null
         * @return true if the text and lexeme are equal, or if either is null; false otherwise
         */
        private boolean matchText(Lex lex, String text) {
            if (text == null) return true;
            if (lex.lexeme() == null) return true;
            return lex.lexeme().equals(text);
        }

        /**
         * Checks whether the type of the given Lex object matches the specified type.
         * If either the specified type or the Lex object's type is null, the method
         * returns true, signifying a match.
         *
         * @param lex  the Lex object whose type is to be compared; must not be null
         * @param type the type to compare against the Lex object's type; can be null
         * @return true if the types match, or if either is null; false otherwise
         */
        private boolean matchType(Lex lex, Lex.Type type) {
            // if there is no type defined, it matches
            if (type == null) return true;
            if (lex.type() == null) return true;
            return switch (type) {
                case KEYWORD -> lex.type() == Lex.Type.RESERVED && Character.isAlphabetic(lex.text().charAt(0));
                case RESERVED -> lex.type() == Lex.Type.RESERVED && !Character.isAlphabetic(lex.text().charAt(0));
                case IDENTIFIER -> lex.type() == Lex.Type.IDENTIFIER;
                case INTEGER -> lex.type() == Lex.Type.INTEGER;
                case STRING -> lex.type() == Lex.Type.STRING;
                case FLOAT -> lex.type() == Lex.Type.FLOAT;
                case COMMENT -> lex.type() == Lex.Type.COMMENT;
                case CHARACTER -> lex.type() == Lex.Type.CHARACTER;
                case SPACES -> lex.type() == Lex.Type.SPACES;
            };
        }
    }

    /**
     * An array of rules used to define formatting specifications.
     * The rules in this array specify how lexical elements should be formatted,
     * including the spacing.
     * <p>
     * These rules are applied throughout the formatting process to ensure consistent
     * structure and readability of the formatted content. The rules can be loaded
     * from an external configuration file or resource.
     * <p>
     * There are no built-in rules. Rules are defined using a simple Turicum program.
     * It can be placed under the local directory or in the user's home directory under
     * {@code .turicum/formatter/rules.turi}. If none of these two are defined then the
     * rules file supplied in the LSP JAR is used.
     */
    private static final Rule[] rules = loadRulesFromFileSystem();


    /**
     * Loads formatting rules for the application from the file system or bundled resources.
     * <p>
     * This method attempts to load rules from an external file specified by the path
     * resolved via the `getRulesPath` method. If a valid path is not found or the file cannot
     * be accessed, it falls back to loading a default set of rules embedded within the
     * application's resources.
     * <p>
     * Behavior:
     * - If a file path is resolved:
     * - Attempts to read the file content.
     * - Calls `loadRulesFile` to process the file.
     * - Handles potential `IOException` during file reading and logs the exception.
     * - If no valid file path is resolved:
     * - Attempts to load rules from a bundled resource stream.
     * - Calls `loadRulesFile` to process the resource content.
     * - Logs an error and terminates the application if the resource cannot be accessed.
     * - Logs progress and errors to the standard error stream.
     * <p>
     * Dependencies:
     * - Uses the `getRulesPath` method to determine the rules file location.
     * - Processes the rules using the `loadRulesFile` method.
     * <p>
     * This method is critical for initializing the application's rule set, either from an
     * external configuration or embedded defaults.
     */
    private static Rule[] loadRulesFromFileSystem() {
        System.err.println("Loading default rules.");
        final Optional<Path> rulesPath = getRulesPath();
        if (rulesPath.isEmpty()) {
            try (final var is = TuriFormatter.class.getClassLoader().getResourceAsStream(RULES_RESOURCE)) {
                if (is != null) {
                    final String rulesTuriSource = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return loadRulesFile(rulesTuriSource, "resource(" + RULES_RESOURCE + ")");
                } else {
                    System.err.println("Error loading default rules from resource.");
                    return null;
                }
            } catch (IOException e) {
                System.err.println("Error loading default rules from resources.");
                ExceptionXmlWriter.writeToXml(e);
                return null;
            }
        } else {
            System.err.println("Rules are to be loaded from " + rulesPath.get());
            final String rulesTuriSource;
            try {
                rulesTuriSource = Files.readString(rulesPath.get());
                return loadRulesFile(rulesTuriSource, rulesPath.get().toString());
            } catch (IOException g) {
                ExceptionXmlWriter.writeToXml(g);
                return null;
            }
        }
    }

    private static Rule[] loadRulesFile(String rulesTuriSource, String rulesPath) {
        try {
            try (final var interpreter = new Interpreter(rulesTuriSource)) {
                final var ruleTable = interpreter.compileAndExecute();
                if (ruleTable instanceof LngList ruleList) {
                    final var loadedRules = new Rule[(int) ruleList.size()];
                    for (int i = 0; i < loadedRules.length; i++) {
                        if (ruleList.array.get(i) instanceof LngList rule) {
                            try {
                                final var type1 = (Lex.Type) rule.array.get(0);
                                final var symbol1 = (String) rule.array.get(1);
                                final var type2 = (Lex.Type) rule.array.get(2);
                                final var symbol2 = (String) rule.array.get(3);
                                final var spaces = (int) (long) rule.array.get(4);
                                loadedRules[i] = new Rule(type1, type2, symbol1, symbol2, spaces);
                            } catch (ClassCastException e) {
                                System.err.println("Invalid rule in rules file: " + e.getMessage());
                                System.err.println("Rule is " + rule);
                                System.err.println("0: " + rule.array.get(0));
                                System.err.println("1: " + rule.array.get(1));
                                System.err.println("2: " + rule.array.get(2));
                                System.err.println("3: " + rule.array.get(3));
                                System.err.println("4: " + rule.array.get(4));
                                System.err.println("Rule list is " + ruleList.array);
                                System.err.println("Rule list index is " + i);
                                return null;
                            }
                        } else {
                            System.err.println("Rule line is not an aray.");
                            System.err.println("Rule is " + ruleList.array + "[" + i + "]");
                            return null;
                        }
                    }
                    // if it was filled successfully, overwrite the current rules with the loaded ones
                    System.err.println("Loaded " + loadedRules.length + " rules from " + rulesPath);
                    return loadedRules;
                }
                System.err.println("The result of the rule file is not a list.");
                return null;
            }
        } catch (BadSyntax | ExecutionException i) {
            System.err.println("Error loading rules from " + rulesPath);
            ExceptionXmlWriter.writeToXml(i);
            return null;
        }
    }


    /**
     * Resolves the path to the configuration file containing formatting rules.
     * It first checks for the existence of the rules file in the local directory
     * and, if not found, falls back to the user's home directory.
     *
     * @return an Optional containing the path to the rules file if it exists, or an empty Optional if no rules file is found
     */
    private static Optional<Path> getRulesPath() {
        final var userHome = System.getProperty("user.home");
        final var globalRules = Path.of(userHome, CONFIGURATION_DIRECTORY, FORMATTER_SUBDIR, RULES_FILE_NAME);
        final var localRules = Path.of(".", CONFIGURATION_DIRECTORY, FORMATTER_SUBDIR, RULES_FILE_NAME);
        return Optional.of(localRules).filter(p -> p.toFile().exists()).or(() -> Optional.of(globalRules).filter(p -> p.toFile().exists()));
    }

    /**
     * Determines the number of spaces to be inserted between the current lexical element and the next lexical element.
     * This method uses predefined rules to decide the spacing based on the relationship between the two lexical elements.
     * If no matching rule is found or the next lexical element is null, the method returns 0.
     *
     * @param lex  the current lexical element being analyzed
     * @param next the next lexical element in the sequence; may be null
     * @return the number of spaces to be inserted between the current and next lexical elements
     */
    private static int spacesFor(Lex lex, Lex next) {
        if (rules == null || next == null) return 0;
        for (final var rule : rules) {
            if (rule.match(lex, next)) {
                return rule.spaces;
            }
        }
        return 0;
    }

    /**
     * Converts a lexical element (Lex) to its corresponding string representation based on its type.
     * Handles various types of lexical elements such as spaces, a, identifiers, integers, floats,
     * strings, comments, and reserved keywords. The method also considers the next lexical element
     * to determine accurate spacing or formatting.
     * <p>
     * When this is a comment or a string, then return only the part from the start till the new line if this is a
     * multi-line string or comment.
     *
     * @param lex  the current lexical element to be converted to a string representation
     * @param next the next lexical element, used to determine spacing or additional formatting
     * @return the string representation of the current lexical element, formatted appropriately
     */
    private static String lexToString(Lex lex, Lex next) {
        if (lex == null) {
            return "";
        }
        if (lex.type() == Lex.Type.SPACES) {
            return "";
        }

        if (lex.type() == Lex.Type.STRING)
            return firstLine(lex.lexeme()) + " ".repeat(spacesFor(lex, next));
        if (lex.type() == Lex.Type.COMMENT)
            return firstLine(lex.text()) + " ".repeat(spacesFor(lex, next));

        return lex.lexeme() + " ".repeat(spacesFor(lex, next));
    }

    /**
     * Extracts the first line from the provided text. If the text does not contain a newline character, the entire
     * text is returned.
     *
     * @param text the input string to process; must not be null
     * @return the first line of the input text, or the entire text if no newline is found
     */
    private static String firstLine(String text) {
        int i = text.indexOf('\n');
        if (i < 0) {
            return text;
        } else {
            return text.substring(0, i);
        }
    }

    private static String expandTabs(String line) {
        StringBuilder result = new StringBuilder();
        int column = 0;

        for (char c : line.toCharArray()) {
            if (c == '\t') {
                // Calculate spaces needed to reach the next 4th column
                int spacesToAdd = 4 - (column % 4);
                result.append(" ".repeat(spacesToAdd));
                column += spacesToAdd;
            } else {
                result.append(c);
                column++;
            }
        }

        return result.toString();
    }

    private static class LineContent {
        String code;
        String comment;

        LineContent(String code, String comment) {
            this.code = code;
            this.comment = comment;
        }
    }

    /**
     * Parses a line of a and separates it into code and comment components.
     * Identifies and handles line comments, block comments, and nested block comments.
     *
     * @param line the input line of a to be parsed
     * @return a LineContent object containing the code and comment extracted from the input line
     */
    private static LineContent parseLineContent(String line) {
        StringBuilder code = new StringBuilder();
        String comment = "";
        int i = 0;
        boolean inBlockComment = false;
        int blockCommentDepth = 0;

        while (i < line.length()) {
            char current = line.charAt(i);
            char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

            if (inBlockComment) {
                // Inside block comment
                code.append(current);

                // Check for nested block comment start
                if (current == '/' && next == '*') {
                    blockCommentDepth++;
                    code.append(next);
                    i += 2;
                    continue;
                }
                // Check for block comment end
                else if (current == '*' && next == '/') {
                    blockCommentDepth--;
                    if (blockCommentDepth == 0) {
                        inBlockComment = false;
                    }
                    code.append(next);
                    i += 2;
                    continue;
                }
            } else {
                // Check for the start of line comment
                if (current == '/' && next == '/') {
                    comment = line.substring(i);
                    break;
                }
                // Check for the start of block comment
                else if (current == '/' && next == '*') {
                    inBlockComment = true;
                    blockCommentDepth = 1;
                    code.append(current).append(next);
                    i += 2;
                    continue;
                }
                // Regular code character
                else {
                    code.append(current);
                }
            }

            i++;
        }

        return new LineContent(code.toString(), comment);
    }

    /**
     * Finds and returns the last non-space character from the code portion of the given input line.
     * Spaces and tab characters are ignored during the search. If no non-space character is found,
     * a null character ('\0') is returned.
     *
     * @param line the input string to analyze, containing both code and optional comments
     * @return the last non-space character from the code portion of the input, or '\0' if none exists
     */
    private static char lastNonSpaceChar(String line) {
        LineContent parsed = parseLineContent(line);
        String codePart = parsed.code;

        // Find the last non-space character without creating new strings
        for (int i = codePart.length() - 1; i >= 0; i--) {
            char c = codePart.charAt(i);
            if (c != ' ' && c != '\t') {
                return c;
            }
        }

        return '\0';
    }

}
