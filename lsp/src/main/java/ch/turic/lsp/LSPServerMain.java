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
        TuriLanguageServer server = new TuriLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        Future<?> listening = launcher.startListening();
        System.out.println("Language server started. Listening for requests...");

        try {
            listening.get();
        } catch (Throwable e) {
            ExceptionXmlWriter.writeToXml(e, new java.io.File("/Users/verhasp/github/turicum/lsp-exception.xml"));
        }
    }
}
