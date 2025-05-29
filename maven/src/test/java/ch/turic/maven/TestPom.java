package ch.turic.maven;

import ch.turic.Interpreter;
import ch.turic.analyzer.Input;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestPom {

    //@Test
    void test() throws Exception {
        final var fn = "../pom.turi";
        final var interpreter = new Interpreter(Input.fromFile(Path.of(fn)));
        final var pomXml = interpreter.compileAndExecute().toString();
        Files.writeString(Path.of("../pom.xml"), pomXml, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

}
