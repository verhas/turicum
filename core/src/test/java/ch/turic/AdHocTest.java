package ch.turic;

import ch.turic.analyzer.Input;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class AdHocTest {
    @Test
    void adHocTest() throws Exception {
        final var baos = new ByteArrayOutputStream();
        this.getClass().getClassLoader().getResourceAsStream("adhoc.turi").transferTo(baos);
        Interpreter interpreter = new Interpreter(Input.fromString(baos.toString(StandardCharsets.UTF_8), "adhoc.turi"));
        interpreter.execute();
    }
}
