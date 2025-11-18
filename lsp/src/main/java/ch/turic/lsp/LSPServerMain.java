package ch.turic.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;

// Main class to start the server
class LSPServerMain {
    static void main(String[] args) {
        System.err.println("=== Turi Language Server Started " + OffsetDateTime.now() + " ===");
        System.err.flush();
        startServer(new ErrLogging.InputStream(System.in), new ErrLogging.PrintStream(System.out));
    }

    /**
     * Starts the Turi Language Server, initializing the necessary components and establishing
     * a connection with the Language Client over the provided input and output streams.
     * Sets a default uncaught exception handler to log unexpected errors.
     *
     * @param in  the {@code InputStream} to read messages from the client
     * @param out the {@code PrintStream} to send messages to the client
     */
    public static void startServer(InputStream in, PrintStream out) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            System.err.println("=== UNCAUGHT EXCEPTION in thread " + thread.getName() + " ===");
            ExceptionXmlWriter.writeToXml(ex);
            System.err.flush();
        });
        try {
            TuriLanguageServer server = new TuriLanguageServer();
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);
            Future<?> listening = launcher.startListening();
            listening.get();
        } catch (Throwable e) {
            System.err.println("=== MAIN THREAD EXCEPTION ===");
            ExceptionXmlWriter.writeToXml(e);
        }
        System.err.println("=== SERVER EXITED NORMALLY - listening.get() returned ===");
        System.err.flush();
    }
}
