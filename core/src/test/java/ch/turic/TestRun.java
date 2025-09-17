package ch.turic;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestRun {

    @Test
    void runQueens() throws Exception {
        final var baos = new ByteArrayOutputStream();
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("queens.turi")).transferTo(baos);
        try (final Interpreter interpreter = new Interpreter(Input.fromString(baos.toString(StandardCharsets.UTF_8), "queens.turi"))) {
            interpreter.compileAndExecute();
        }
    }


}

