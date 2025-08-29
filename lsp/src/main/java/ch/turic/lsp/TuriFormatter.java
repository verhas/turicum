package ch.turic.lsp;

public class TuriFormatter {

    public static String formatDocument(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String[] lines = content.split("\n", -1); // -1 to preserve empty lines at end
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean nextLineIndentedFromColon = false; // Track if next line should be indented due to colon

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Apply extra indent for colon if needed
            int currentIndent = indentLevel + (nextLineIndentedFromColon ? 1 : 0);
            String formattedLine = formatLine(line, currentIndent);

            // Reset colon indent flag after applying it
            nextLineIndentedFromColon = false;

            // Check if we need to adjust indent for next line
            String trimmedFormatted = formattedLine.trim();
            if (!trimmedFormatted.isEmpty()) {
                char lastChar = getLastNonSpaceChar(formattedLine);
                if (lastChar == '{' || lastChar == '(' || lastChar == '[') {
                    indentLevel++;
                } else if (lastChar == ':') {
                    // For colon, only indent the next line, don't change base indent level
                    nextLineIndentedFromColon = true;
                }
            }

            // Check if current line should decrease indent (closing brackets)
            if (!trimmedFormatted.isEmpty()) {
                char firstChar = trimmedFormatted.charAt(0);
                if (firstChar == '}' || firstChar == ')' || firstChar == ']') {
                    indentLevel = Math.max(0, indentLevel - 1);
                    // Re-format the line with correct indent (no colon indent for closing brackets)
                    formattedLine = formatLine(line, indentLevel);
                }
            }

            result.append(formattedLine);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private static String formatLine(String line, int indentLevel) {
        if (line.trim().isEmpty()) {
            return ""; // Empty lines become completely empty
        }

        // Convert tabs to spaces (align to next 4th character)
        String expandedTabs = expandTabs(line);

        // Parse the line to separate code from comments
        LineContent parsed = parseLineContent(expandedTabs);

        // Format the code part (trim trailing spaces, apply indentation)
        String formattedCode = parsed.code.replaceAll("\\s+$", ""); // Remove trailing spaces

        // Apply indentation (4 spaces per level)
        String indent = "    ".repeat(indentLevel);
        String trimmedCode = formattedCode.trim();

        if (trimmedCode.isEmpty()) {
            return parsed.comment.isEmpty() ? "" : indent + parsed.comment;
        }

        return indent + trimmedCode + parsed.comment;
    }

    private static  String expandTabs(String line) {
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

    private static LineContent parseLineContent(String line) {
        StringBuilder code = new StringBuilder();
        String comment = "";
        int i = 0;
        boolean inBlockComment = false;
        int blockCommentDepth = 0;

        while (i < line.length()) {
            char current = line.charAt(i);
            char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

            if (!inBlockComment) {
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
            } else {
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
