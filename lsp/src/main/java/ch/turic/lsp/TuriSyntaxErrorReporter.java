package ch.turic.lsp;

import ch.turic.Interpreter;
import ch.turic.exceptions.BadSyntax;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        publishDiagnostics(uri, syntaxAnalysis(content, uri));
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
     * Analyzes a given content string for syntax errors and returns a list of diagnostics
     * representing the errors found during the analysis.
     *
     * @param content The content to analyze for syntax errors.
     * @param uri     The URI of the document being analyzed, used for context and error reporting.
     * @return A list of {@code Diagnostic} objects representing the syntax errors found in the content.
     * If no syntax errors are found, an empty list is returned.
     */
    private List<Diagnostic> syntaxAnalysis(String content, String uri) {
        final var diagnostics = new ArrayList<Diagnostic>();
        try (Interpreter interpreter = new Interpreter(ch.turic.Input.fromString(content, uri))) {
            interpreter.compile();
        } catch (BadSyntax bs) {
            final var pos = bs.getPosition();
            final var msg = getTheFirstLineOfTheExceptionMessage(bs);
            diagnostics.add(
                    createDiagnostic(pos.line - 1, pos.column, pos.column + 2, msg)
            );
        } catch (Exception e) {
            diagnostics.add(
                    createDiagnostic(1, 0, 2, e.getMessage())
            );
        }

        return diagnostics;
    }

    /**
     * Extracts the first line of the message from an {@code Exception} exception.
     *
     * @param bs The {@code BadSyntax} exception containing the message.
     * @return The first line of the exception message. If the message contains no newline characters,
     * the entire message is returned.
     */
    private static String getTheFirstLineOfTheExceptionMessage(Exception bs) {
        final var msg = bs.getMessage();
        int i = msg.indexOf("\n");
        if (i == -1) {
            i = msg.length();
        }
        return msg.substring(0, i);
    }


    /**
     * Creates a diagnostic object that represents a syntax-related issue in a document.
     *
     * @param line     The line number where the diagnostic should be reported (0-based index).
     * @param startCol The start column of the error within the specified line (0-based index).
     * @param endCol   The end column of the error within the specified line (0-based index).
     * @param message  A descriptive message explaining the diagnostic.
     * @return A {@code Diagnostic} object containing details of the issue including its location,
     * message, severity, and source information.
     */
    private Diagnostic createDiagnostic(int line, int startCol, int endCol,
                                        String message) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(
                new Position(line, startCol),
                new Position(line, endCol)
        ));
        diagnostic.setMessage(message);
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        diagnostic.setSource("syntax-checker");
        return diagnostic;
    }

}