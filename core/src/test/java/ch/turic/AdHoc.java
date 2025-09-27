package ch.turic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

public class AdHoc {
    @Test
    void adHocTest() throws Exception {
        System.out.println(""+Integer.MAX_VALUE);
        System.out.println(new File(".").getAbsolutePath());
        try (Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/adhoc.turi")))) {
            interpreter.compileAndExecute();
        }
    }
}
