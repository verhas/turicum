package ch.turic.embed;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the basic usage of the {@code ch.turic.embed} API. The snippet markers in this
 * file are referenced from {@code EMBEDDING.md}; after changing a snippet, regenerate the
 * document with {@code mdship update EMBEDDING.md}.
 */
class TestEmbeddingBasics {

    @Test
    void helloWorld() {
        // snippet hello_world
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            final var result = session.eval("2 + 2 * 20");
            assertEquals(42L, result);
        }
        // end snippet
    }

    @Test
    void resultsAreJavaValues() {
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            // snippet result_types
            assertEquals(42L, session.eval("42"));                  // integers are Long
            assertEquals(3.14, session.eval("3.14"));               // floats are Double
            assertEquals("ab", session.eval("\"a\" + \"b\""));      // strings are String
            assertEquals(true, session.eval("1 < 2"));              // conditions are Boolean
            assertNull(session.eval("none"));                       // none is null

            final var list = (LngList) session.eval("[1, 2, 3]");
            assertEquals(List.of(1L, 2L, 3L), list.array);

            final var obj = (LngObject) session.eval("{name: \"turicum\", version: 1}");
            assertEquals("turicum", obj.getField("name"));
            // end snippet
        }
    }

    @Test
    void injectingVariables() {
        // snippet inject_variables
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.set("greeting", "Hello");
            session.set("count", 3);
            assertEquals("Hello Hello Hello ", session.eval("""
                    mut text = ""
                    for i = 0 ; i < count ; i++ {
                        text += greeting + " "
                    }
                    text
                    """));
        }
        // end snippet
    }

    @Test
    void injectedVariablesAreFrozen() {
        // snippet frozen_variables
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.set("limit", 100);
            // the script can read the injected value, but cannot reassign it
            assertThrows(ExecutionException.class, () -> session.eval("limit = 0"));
        }
        // end snippet
    }

    @Test
    void readingGlobalsBack() {
        // snippet read_globals
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.eval("""
                    global answer = 6 * 7
                    global name = "turicum"
                    """);
            assertEquals(42L, session.get("answer"));
            assertEquals("turicum", session.get("name"));
        }
        // end snippet
    }

    @Test
    void sessionKeepsStateBetweenEvals() {
        // snippet session_state
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            session.eval("global counter = 0");
            session.eval("counter = counter + 1");
            session.eval("counter = counter + 1");
            assertEquals(2L, session.get("counter"));
        }
        // end snippet
    }

    @Test
    void capturingOutput() {
        // snippet capture_output
        final var captured = new ByteArrayOutputStream();
        final var policy = SandboxPolicy.builder()
                .stdout(captured)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            session.eval("println \"hello from the script\"");
        }
        assertEquals("hello from the script\n", captured.toString(StandardCharsets.UTF_8));
        // end snippet
    }
}
