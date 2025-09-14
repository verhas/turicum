package ch.turic.lsp;

import ch.turic.analyzer.Lexer;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Main Language Server class
public class TuriLanguageServer implements LanguageServer, LanguageClientAware {

    private LanguageClient client;
    private TuriTextDocumentService textDocumentService;
    private TuriWorkspaceService workspaceService;
    private int errorCode = 1;

    public TuriLanguageServer() {
        this.textDocumentService = new TuriTextDocumentService();
        this.workspaceService = new TuriWorkspaceService();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        final var capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        // Completion support
        final var completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(true);
        completionOptions.setTriggerCharacters(List.of("."));
        capabilities.setCompletionProvider(completionOptions);

        // Hover support
        capabilities.setHoverProvider(true);
        capabilities.setDocumentSymbolProvider(true);

        // Add code lens support
        final var codeLensOptions = new CodeLensOptions();
        codeLensOptions.setResolveProvider(false);
        capabilities.setCodeLensProvider(codeLensOptions);

        capabilities.setReferencesProvider(true);

        // Definition support
        capabilities.setDefinitionProvider(true);

        // Document formatting
        capabilities.setDocumentFormattingProvider(true);

        // Diagnostic support (error reporting)
        capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());

        // Semantic tokens for better syntax highlighting
        final var semanticTokens = new SemanticTokensWithRegistrationOptions();
        SemanticTokensLegend legend = new SemanticTokensLegend();
        legend.setTokenTypes(List.of(Lexer.RESERVED.toArray(String[]::new)));
        legend.setTokenModifiers(List.of("documented"));

        semanticTokens.setLegend(legend);
//        capabilities.setSemanticTokensProvider(semanticTokens);


        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    public static final Executor VIRTUAL_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());


    @Override
    public CompletableFuture<Object> shutdown() {
        errorCode = 0;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(errorCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        textDocumentService.connect(client);
    }
}

