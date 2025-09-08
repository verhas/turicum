package ch.turic.dap;

import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main entry point for the Turicum Debug Adapter
 */
public class TuricumDebugAdapterLauncher {

    private static final Logger logger = Logger.getLogger(TuricumDebugAdapterLauncher.class.getName());

    public static void main(String[] args) {
        // Configure logging
        configureLogging();

        logger.info("Starting Turicum Debug Adapter...");

        try {
            // Create the debug server
            TuricumDebugServer server = new TuricumDebugServer();

            // Determine communication method
            if (args.length > 0 && "--socket".equals(args[0])) {
                // Socket communication (for standalone usage)
                startWithSocket(server, getPort(args));
            } else {
                // Stdio communication (default for IDE integration)
                startWithStdio(server);
            }

        } catch (Exception e) {
            logger.severe("Failed to start debug adapter: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void startWithStdio(TuricumDebugServer server) {
        logger.info("Starting debug adapter with stdio communication");

        // Use stdin/stdout for communication with the IDE
        InputStream inputStream = System.in;
        OutputStream outputStream = System.out;

        startDebugAdapter(server, inputStream, outputStream);
    }

    private static void startWithSocket(TuricumDebugServer server, int port) {
        logger.info("Socket communication not implemented yet. Using stdio instead.");
        startWithStdio(server);
    }

    private static void startDebugAdapter(TuricumDebugServer server, InputStream inputStream, OutputStream outputStream) {
        try {
            // Create the launcher
            Launcher<IDebugProtocolClient> launcher = DSPLauncher.createServerLauncher(
                    server,
                    inputStream,
                    outputStream
            );

            // Connect the server to the client
            IDebugProtocolClient client = launcher.getRemoteProxy();
            server.connect(client);

            logger.info("Debug adapter started successfully");

            // Start listening
            launcher.startListening().get();

            logger.info("Debug adapter stopped");

        } catch (Exception e) {
            logger.severe("Error in debug adapter communication: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start debug adapter", e);
        }
    }

    private static int getPort(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid port number: " + args[i + 1]);
                }
            }
        }
        return 4711; // Default port
    }

    private static void configureLogging() {
        // Configure java.util.logging to be less verbose
        // In production, you might want to log to a file instead
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT [%4$s] %3$s: %5$s%n");

        // Optionally disable logging to stdout if using stdio for DAP communication
        if (Boolean.parseBoolean(System.getProperty("turicum.debug.disable-logging", "false"))) {
            LogManager.getLogManager().reset();
        }
    }
}

