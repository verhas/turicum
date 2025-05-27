package ch.turic;

import ch.turic.analyzer.Input;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class AdHocTest {
    @Test
    void adHocTest() throws Exception {
        System.out.println(new File(".").getAbsolutePath());
        Interpreter interpreter = new Interpreter(Input.fromFile(Path.of("./src/test/resources/adhoc.turi")));
        interpreter.execute();
    }
}
