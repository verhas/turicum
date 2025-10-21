package ch.turic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

public class AdHoc {
    @Test
    void adHocTest() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        System.out.println(System.getProperty("user.home"));
        try (Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/adhoc.turi")))) {
            interpreter.compileAndExecute();
        }
    }
}
