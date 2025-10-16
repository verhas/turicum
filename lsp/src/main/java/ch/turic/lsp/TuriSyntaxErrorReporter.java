package ch.turic.lsp;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Interpreter;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.*;

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

        List<Diagnostic> diagnostics = syntaxAnalysis(content);
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

    private List<Diagnostic> syntaxAnalysis(String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            // <editor-fold>
            Interpreter interpreter = new Interpreter(content);
            interpreter.compile();
            // </editor-fold>
        } catch (BadSyntax bs) {
            final var line = bs.getPosition().line;
            final var column = bs.getPosition().column;
            int i = bs.getMessage().indexOf("\n");
            if (i == -1) {
                i = bs.getMessage().length();
            }
            diagnostics.add(createDiagnostic(
                    line - 1, column, column + 2,
                    bs.getMessage().substring(0, i),
                    DiagnosticSeverity.Error
            ));
        } catch (Exception e) {
            diagnostics.add(createDiagnostic(
                    1, 0, 2,
                    e.getMessage(),
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

}