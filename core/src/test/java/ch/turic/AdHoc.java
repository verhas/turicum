package ch.turic;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class AdHoc {
    @Test
    void adHocTest() throws Exception {
        try (Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/adhoc.turi")))) {
            interpreter.compileAndExecute();
        }
    }
}
