package ch.turic.dap;

import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.InputStream;
import java.io.OutputStream;

// Alternative launcher for use as a library
class TuricumDebugAdapterFactory {

    /**
     * Creates a new debug adapter server instance
     *
     * @return A new TuricumDebugServer instance
     */
    public static TuricumDebugServer createDebugServer() {
        return new TuricumDebugServer();
    }

    /**
     * Creates and starts a debug adapter with the given input/output streams
     *
     * @param inputStream  Input stream for communication
     * @param outputStream Output stream for communication
     * @return The launcher instance
     */
    public static Launcher<IDebugProtocolClient> createDebugAdapterLauncher(
            InputStream inputStream,
            OutputStream outputStream) {

        TuricumDebugServer server = createDebugServer();

        Launcher<IDebugProtocolClient> launcher = DSPLauncher.createServerLauncher(
                server,
                inputStream,
                outputStream
        );

        IDebugProtocolClient client = launcher.getRemoteProxy();
        server.connect(client);

        return launcher;
    }
}
