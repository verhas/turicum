package ch.turic.lsp;

import ch.turic.Input;
import ch.turic.analyzer.Keywords;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;

import java.util.regex.Pattern;

public class TuriFormatter {

    private static final Pattern SWITCH = Pattern.compile("//\\s*format:(on|off)");

    public static String formatDocument(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        final var lexes = Lexer.try_analyze(Input.fromString(content));

        String[] lines = content.split("\n", -1); // -1 to preserve empty lines at the end
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        int colonLevel = 0; // Track if the next line should be indented due to colon
        boolean formattingIsOn = true;
        int index = 0;
        for (int i = 0; i < lines.length; i++) {
            final var line = lines[i];
            final var formatSwitch = SWITCH.matcher(line);
            if (formatSwitch.find()) {
                formattingIsOn = formatSwitch.group(1).equals("on");
            }
            index = lineIndex(lexes, i, index);
            boolean inString = isInString(lexes, i, index);
            boolean inComment = !inString && isInComment(lexes, i, index);

            final var newIndentLevel = calculateIndentLevel(lexes, i, indentLevel, index);
            // when we are stepping back with a closing '}', we already put that on the previous indent level
            if (newIndentLevel < indentLevel) {
                indentLevel = newIndentLevel;
            }

            if (formattingIsOn) {
                var formattedLine = (inString ? "" : "    ".repeat(indentLevel + colonLevel))
                        + formatLine(inString, inComment, line, lexes, index, i);

                colonLevel = lastNonSpaceChar(line) == ':' && !inComment ? colonLevel + 1 : 0;

                result.append(formattedLine);
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
     * Since the code starts at index zero and steps only forward the argument 'index' returned from the last call
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

    /**
     * Converts a lexical element (Lex) to its corresponding string representation based on its type.
     * Handles various types of lexical elements such as spaces, a, identifiers, integers, floats,
     * strings, comments, and reserved keywords. The method also considers the next lexical element
     * to determine accurate spacing or formatting.
     *
     * @param lex  the current lexical element to be converted to a string representation
     * @param next the next lexical element, used to determine spacing or additional formatting
     * @return the string representation of the current lexical element, formatted appropriately
     */
    private static String lexToString(Lex lex, Lex next) {
        return switch (lex.type()) {
            case SPACES -> "";
            case CHARACTER -> lex.lexeme();
            case IDENTIFIER, INTEGER, FLOAT -> lex.lexeme() + spc(lex, next);
            case STRING -> firstLine(lex.lexeme()) + spc(lex, next);
            case COMMENT -> firstLine(lex.text()) + spcOrEol(next);
            case RESERVED -> {
                if (lex.is(Keywords.RETURN)) {
                    yield "return" + spcOrEol(next);
                }
                if (isKw(lex)) {
                    yield lex.text() + spc(lex, next);
                } else {
                    if (next != null) {
                        yield switch (lex.text()) {
                            case "&&", "||", "===", "==", "!=", ">=", "<=", ">", "<", "=", "##", "->" ->
                                    " " + lex.text() + spcOrEol(next);
                            case "," -> "," + spc(lex, next);
                            case ":" -> ":" + spc(lex, next);
                            case ";" -> "; ";
                            default -> lex.text();
                        };
                    } else
                        yield lex.text();
                }
            }
        };
    }

    private static String firstLine(String text) {
        int i = text.indexOf('\n');
        if (i < 0) {
            return text;
        } else {
            return text.substring(0, i);
        }
    }

    private static String spcOrEol(Lex next) {
        return next == null ? "" : " ";
    }

    private static String spc(Lex lex, Lex next) {
        if (next == null) return "";
        return switch (next.type()) {
            case INTEGER, FLOAT, SPACES, CHARACTER, IDENTIFIER, STRING, COMMENT -> " ";
            case RESERVED -> isKw(next) || !next.is(".", "[", "{", "(", ")", "}", "]") ? " " : "";
        };
    }

    private static boolean isKw(Lex lex) {
        return Character.isJavaIdentifierStart(lex.text().charAt(0));
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
                // Check for start of line comment
                if (current == '/' && next == '/') {
                    comment = line.substring(i);
                    break;
                }
                // Check for start of block comment
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

    private static char lastNonSpaceChar(String line) {
        // Parse to get only the code part (excluding line comments)
        LineContent parsed = parseLineContent(line);
        String codePart = parsed.code.replaceAll("\\s+$", ""); // Remove trailing spaces

        if (codePart.isEmpty()) {
            return '\0';
        }

        return codePart.charAt(codePart.length() - 1);
    }

}
