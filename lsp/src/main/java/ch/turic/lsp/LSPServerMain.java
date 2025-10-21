package ch.turic.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;

// Main class to start the server
class LSPServerMain {
    static void main(String[] args) {
        startServer(System.in, System.out);
    }

    public static void startServer(InputStream in, OutputStream out) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            System.err.println("=== UNCAUGHT EXCEPTION in thread " + thread.getName() + " ===");
            ExceptionXmlWriter.writeToXml(ex);
        });
        try {
            TuriLanguageServer server = new TuriLanguageServer();
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);
            Future<?> listening = launcher.startListening();
            listening.get();
        } catch (Throwable e) {
            ExceptionXmlWriter.writeToXml(e);
        }
    }
}
