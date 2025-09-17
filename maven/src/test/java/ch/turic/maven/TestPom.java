package ch.turic.maven;

import ch.turic.Interpreter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestPom {

    @Test
    void test() throws Exception {
        final var fn = "../pom.turi";
        System.setProperty("APPIA", "..");
        try (final var interpreter = new Interpreter(ch.turic.Input.fromFile(Path.of(fn)))) {
            final var pomXml = interpreter.compileAndExecute().toString();
            Files.writeString(Path.of("../pom-test.xml"), pomXml, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

}
