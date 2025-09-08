package ch.turic.lsp;

import ch.turic.Input;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;

public class TuriFormatter {

    public static String formatDocument(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        final var lexes = Lexer.try_analyze(Input.fromString(content));

        String[] lines = content.split("\n", -1); // -1 to preserve empty lines at the end
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean nextLineIndentedFromColon = false; // Track if the next line should be indented due to colon

        int index = 0;
        for (int i = 0; i < lines.length; i++) {
            final var line = lines[i];
            index = lineIndex(lexes, i, index);
            boolean inString = isInString(lexes, i, index);
            boolean inComment = !inString && isInComment(lexes, i, index);

            final var newIndentLevel = calculateIndentLevel(lexes, i, indentLevel, index);
            if (newIndentLevel < indentLevel) {
                indentLevel = newIndentLevel;
            }
            char lastChar = getLastNonSpaceChar(line);
            nextLineIndentedFromColon = lastChar == ':';
            // Apply extra indent for colon if needed
            int currentIndent = indentLevel + (nextLineIndentedFromColon ? 1 : 0);
            var formattedLine = formatLine(line, currentIndent, inString, inComment);

            // if we are tabbing back, we need to adjust the indent level starting with the next line
            indentLevel = newIndentLevel;

            result.append(formattedLine);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Determines the lexical index before the start of a specific line number in a lexical list.
     * Iterates through the given list to locate the starting index of the specified line
     * relative to the provided index.
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
                if (priLex.position().line - 1 < lineNr &&
                        thisLex.position().line - 1 >= lineNr) {
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
                        priLex.position().line - 1 < lineNr &&
                        thisLex.position().line - 1 >= lineNr) {
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
                        current.position().line - 1 < lineNr &&
                        next.position().line - 1 >= lineNr) {
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
                if (lex.position().line - 1 > lineNr) {
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

    private static String formatLine(String line, int indentLevel, boolean inString, boolean inComment) {
        // if we are in a multi-line string, we do not even extend the tabs to spaces
        if (inString) {
            return line;
        }
        var trimmed = expandTabs(line).trim();
        if (inComment) {
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
            final var gutter = " "; // add an extra space to align the '*' characters
            return "    ".repeat(indentLevel) +
                    gutter + trimmed;
        }
        // Convert tabs to spaces (align to next 4th character)
        return "    ".repeat(indentLevel) + trimmed.trim();
    }

    private static String expandTabs(String line) {
        StringBuilder result = new StringBuilder();
        int column = 0;

        for (char c : line.toCharArray()) {
            if (c == '\t') {
                // Calculate spaces needed to reach next 4th column
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
     * Parses a line of text and separates it into code and comment components.
     * Identifies and handles line comments, block comments, and nested block comments.
     *
     * @param line the input line of text to be parsed
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

    private static char getLastNonSpaceChar(String line) {
        // Parse to get only the code part (excluding line comments)
        LineContent parsed = parseLineContent(line);
        String codePart = parsed.code.replaceAll("\\s+$", ""); // Remove trailing spaces

        if (codePart.isEmpty()) {
            return '\0';
        }

        return codePart.charAt(codePart.length() - 1);
    }

}
