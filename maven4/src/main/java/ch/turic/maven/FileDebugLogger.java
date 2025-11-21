package ch.turic.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileDebugLogger {

    public static void debug(final String message) {
        try {
            Files.writeString(Path.of("/Users/verhasp/github/turicum/debug.log"), message + "\n", StandardOpenOption.APPEND);
        } catch (IOException ignore) {
        }
    }
}
