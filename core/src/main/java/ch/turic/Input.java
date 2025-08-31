package ch.turic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface Input permits ch.turic.analyzer.Input{

    static ch.turic.analyzer.Input fromString(final String s) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), "none");
    }

    static ch.turic.analyzer.Input fromString(final String s, String fn) {
        return new ch.turic.analyzer.Input(new StringBuilder(s), fn);
    }

    static ch.turic.analyzer.Input fromFile(final Path path) throws IOException {
        return new ch.turic.analyzer.Input(new StringBuilder(Files.readString(path, StandardCharsets.UTF_8)), path.normalize().toAbsolutePath().toString());
    }

}
