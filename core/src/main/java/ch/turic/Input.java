package ch.turic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Input {

    public static Input fromString(final String s) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), "none");
    }

    public static Input fromString(final String s, String fn) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), fn);
    }

    public static Input fromFile(final Path path) throws IOException {
        return new ch.turic.analyzer.Input(new StringBuilder(Files.readString(path, StandardCharsets.UTF_8)), path.normalize().toAbsolutePath().toString());
    }

}
