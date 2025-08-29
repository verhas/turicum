package ch.turic.lsp;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;
import java.util.List;

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
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        // Completion support
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(true);
        completionOptions.setTriggerCharacters(List.of("."));
        capabilities.setCompletionProvider(completionOptions);

        // Hover support
        capabilities.setHoverProvider(true);

        // Definition support
        capabilities.setDefinitionProvider(true);

        // Document formatting
        capabilities.setDocumentFormattingProvider(true);

        // Diagnostic support (error reporting)
        capabilities.setDiagnosticProvider(new DiagnosticRegistrationOptions());

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

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

