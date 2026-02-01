package ch.turic;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class XmlBuilderTest {
    @Test
    void xml_builder() throws Exception {
        try (Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/xml_builder_test.turi")))) {
            interpreter.compileAndExecute();
        }
    }
}
