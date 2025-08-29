package ch.turic.lsp;

import ch.turic.BadSyntax;
import ch.turic.Interpreter;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TuriSyntaxErrorReporter {
    private final LanguageClient client;
    private final DocumentManager documentManager;

    public TuriSyntaxErrorReporter(LanguageClient client, DocumentManager documentManager) {
        this.client = client;
        this.documentManager = documentManager;
    }

    /**
     * Analyze a document for syntax errors and publish diagnostics
     */
    public void analyzeAndReportErrors(String uri) {
        String content = documentManager.getContent(uri);
        if (content == null) {
            return;
        }

        List<Diagnostic> diagnostics = analyzeSyntaxErrors(content);
        publishDiagnostics(uri, diagnostics);
    }

    /**
     * Publish diagnostics to the client
     */
    private void publishDiagnostics(String uri, List<Diagnostic> diagnostics) {
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.setUri(uri);
        params.setDiagnostics(diagnostics);
        client.publishDiagnostics(params);
    }

    /**
     * Clear all diagnostics for a document
     */
    public void clearDiagnostics(String uri) {
        publishDiagnostics(uri, Collections.emptyList());
    }

    /**
     * Main syntax analysis method - customize this for your language
     */
    private List<Diagnostic> analyzeSyntaxErrors(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        String[] lines = content.split("\n");

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];

            // Check various syntax errors
            diagnostics.addAll(checkUnclosedStrings(line, lineNum));
            diagnostics.addAll(checkIndentationErrors(line, lineNum));
        }

        // Check document-wide errors
        diagnostics.addAll(checkGlobalBracketMatching(content));
        diagnostics.addAll(checkUnclosedBlockComments(content));
        diagnostics.addAll(fullSyntaxAnalysis(content));

        return diagnostics;
    }

    /**
     * Check for unclosed strings on a line
     */
    private List<Diagnostic> checkUnclosedStrings(String line, int lineNum) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        boolean inString = false;
        int stringStart = -1;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringStart = i;
                } else {
                    inString = false;
                }
            }
        }

        if (inString) {
            diagnostics.add(createDiagnostic(
                    lineNum, stringStart, line.length(),
                    "Unclosed string literal",
                    DiagnosticSeverity.Error
            ));
        }

        return diagnostics;
    }

    private List<Diagnostic> fullSyntaxAnalysis(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            Interpreter interpreter = new Interpreter(content);
            interpreter.compile();
        }catch (BadSyntax bs){
            final var line = bs.getPosition().line;
            final var column = bs.getPosition().column;
            int i = bs.getMessage().indexOf("\n");
            if( i == -1 ){
                i = bs.getMessage().length();
            }
            diagnostics.add(createDiagnostic(
                    line-1, column, column + 2,
                    bs.getMessage().substring(0,i),
                    DiagnosticSeverity.Error
            ));
        }

        return diagnostics;
    }


    /**
     * Check for indentation errors
     */
    private List<Diagnostic> checkIndentationErrors(String line, int lineNum) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        // Check for mixed tabs and spaces
        boolean hasSpaces = line.contains(" ");
        boolean hasTabs = line.contains("\t");

        if (hasSpaces && hasTabs) {
            int firstNonWhitespace = 0;
            while (firstNonWhitespace < line.length() &&
                    Character.isWhitespace(line.charAt(firstNonWhitespace))) {
                firstNonWhitespace++;
            }

            diagnostics.add(createDiagnostic(
                    lineNum, 0, firstNonWhitespace,
                    "Mixed tabs and spaces in indentation",
                    DiagnosticSeverity.Warning
            ));
        }

        return diagnostics;
    }

    /**
     * Check for global bracket matching across the entire document
     */
    private List<Diagnostic> checkGlobalBracketMatching(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Stack<BracketInfo> brackets = new Stack<>();

        String[] lines = content.split("\n");
        boolean inString = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            inLineComment = false; // Reset for each line

            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                char next = (col + 1 < line.length()) ? line.charAt(col + 1) : '\0';

                // Handle comments
                if (!inString && !inBlockComment && c == '/' && next == '/') {
                    inLineComment = true;
                    col++; // Skip next character
                    continue;
                }

                if (!inString && !inLineComment && c == '/' && next == '*') {
                    inBlockComment = true;
                    col++; // Skip next character
                    continue;
                }

                if (inBlockComment && c == '*' && next == '/') {
                    inBlockComment = false;
                    col++; // Skip next character
                    continue;
                }

                if (inLineComment || inBlockComment) {
                    continue;
                }

                // Handle strings
                if (c == '"' && (col == 0 || line.charAt(col - 1) != '\\')) {
                    inString = !inString;
                    continue;
                }

                if (inString) {
                    continue;
                }

                // Handle brackets
                if (c == '(' || c == '[' || c == '{') {
                    brackets.push(new BracketInfo(c, lineNum, col));
                } else if (c == ')' || c == ']' || c == '}') {
                    if (brackets.isEmpty()) {
                        diagnostics.add(createDiagnostic(
                                lineNum, col, col + 1,
                                "Unmatched closing bracket '" + c + "'",
                                DiagnosticSeverity.Error
                        ));
                    } else {
                        BracketInfo opening = brackets.pop();
                        if (!isMatchingBracket(opening.bracket, c)) {
                            diagnostics.add(createDiagnostic(
                                    lineNum, col, col + 1,
                                    "Mismatched bracket: expected '" + getClosingBracket(opening.bracket) + "'",
                                    DiagnosticSeverity.Error
                            ));
                        }
                    }
                }
            }
        }

        // Check for unclosed brackets
        while (!brackets.isEmpty()) {
            BracketInfo unclosed = brackets.pop();
            diagnostics.add(createDiagnostic(
                    unclosed.line, unclosed.column, unclosed.column + 1,
                    "Unclosed bracket '" + unclosed.bracket + "'",
                    DiagnosticSeverity.Error
            ));
        }

        return diagnostics;
    }

    /**
     * Check for unclosed block comments
     */
    private List<Diagnostic> checkUnclosedBlockComments(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Stack<CommentInfo> blockComments = new Stack<>();

        String[] lines = content.split("\n");

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];

            for (int col = 0; col < line.length() - 1; col++) {
                char c = line.charAt(col);
                char next = line.charAt(col + 1);

                if (c == '/' && next == '*') {
                    blockComments.push(new CommentInfo(lineNum, col));
                    col++; // Skip next character
                } else if (c == '*' && next == '/') {
                    if (!blockComments.isEmpty()) {
                        blockComments.pop();
                    }
                    col++; // Skip next character
                }
            }
        }

        // Report unclosed block comments
        while (!blockComments.isEmpty()) {
            CommentInfo unclosed = blockComments.pop();
            diagnostics.add(createDiagnostic(
                    unclosed.line, unclosed.column, unclosed.column + 2,
                    "Unclosed block comment",
                    DiagnosticSeverity.Error
            ));
        }

        return diagnostics;
    }

    /**
     * Helper method to create a diagnostic
     */
    private Diagnostic createDiagnostic(int line, int startCol, int endCol,
                                        String message, DiagnosticSeverity severity) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(
                new Position(line, startCol),
                new Position(line, endCol)
        ));
        diagnostic.setMessage(message);
        diagnostic.setSeverity(severity);
        diagnostic.setSource("syntax-checker");
        return diagnostic;
    }

    /**
     * Helper method to check if brackets match
     */
    private boolean isMatchingBracket(char opening, char closing) {
        return (opening == '(' && closing == ')') ||
                (opening == '[' && closing == ']') ||
                (opening == '{' && closing == '}');
    }

    /**
     * Helper method to get the expected closing bracket
     */
    private char getClosingBracket(char opening) {
        switch (opening) {
            case '(':
                return ')';
            case '[':
                return ']';
            case '{':
                return '}';
            default:
                return opening;
        }
    }

    /**
     * Helper classes for tracking bracket and comment positions
     */
    private static class BracketInfo {
        char bracket;
        int line;
        int column;

        BracketInfo(char bracket, int line, int column) {
            this.bracket = bracket;
            this.line = line;
            this.column = column;
        }
    }

    private static class CommentInfo {
        int line;
        int column;

        CommentInfo(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }
}