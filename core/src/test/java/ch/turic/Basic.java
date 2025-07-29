package ch.turic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

public class Basic {
    @Test
    void adHocTest() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/basic.turi")));
        interpreter.compileAndExecute();
    }
}
