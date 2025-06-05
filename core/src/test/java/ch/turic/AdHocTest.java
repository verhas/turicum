package ch.turic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

public class AdHocTest {
    @Test
    void adHocTest() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        Interpreter interpreter = new Interpreter((ch.turic.analyzer.Input) Input.fromFile(Path.of("./src/test/resources/adhoc.turi")));
        interpreter.compileAndExecute();
    }
}
