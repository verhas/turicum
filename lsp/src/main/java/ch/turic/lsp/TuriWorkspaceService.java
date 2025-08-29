package ch.turic.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

// Workspace Service - handles workspace-related operations
class TuriWorkspaceService implements WorkspaceService {

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        System.out.println("Executing command: " + params.getCommand());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        System.out.println("Configuration changed");
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        System.out.println("Watched files changed");
    }
}
