package ch.turic.embed;

import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the advanced usage of the {@code ch.turic.embed} API. The snippet markers in
 * this file are referenced from {@code EMBEDDING.md}; after changing a snippet, regenerate the
 * document with {@code mdship update EMBEDDING.md}.
 */
class TestEmbeddingAdvanced {

    @Test
    void compileOnceRunManyTimes() {
        // snippet compile_once
        try (final var engine = TuriEngine.create()) {
            // a compiled program is immutable and holds no execution state;
            // compile it once and evaluate it in as many sessions as needed
            final var program = engine.compile("price * (1 + vat_rate)");

            try (final var session = engine.newSession()) {
                session.set("price", 100.0);
                session.set("vat_rate", 0.081);
                assertEquals(108.1, (double) session.eval(program), 0.0001);
            }
            try (final var session = engine.newSession()) {
                session.set("price", 250.0);
                session.set("vat_rate", 0.026);
                assertEquals(256.5, (double) session.eval(program), 0.0001);
            }
        }
        // end snippet
    }

    @Test
    void sessionsAreIsolated() {
        // snippet session_isolation
        try (final var engine = TuriEngine.create();
             final var tenantA = engine.newSession();
             final var tenantB = engine.newSession()) {
            tenantA.eval("global secret = \"tenant A data\"");
            // sessions share nothing; tenant B does not see tenant A's globals
            assertThrows(ExecutionException.class, () -> tenantB.eval("secret"));
        }
        // end snippet
    }

    @Test
    void precompiledProgramsCanBeStored() {
        // snippet serialize
        try (final var engine = TuriEngine.create()) {
            // serialize the compiled program to the binary .turc format ...
            final byte[] turc = engine.compile("6 * 7").serialize();

            // ... store it in a file, a database, a cache ... and load it later,
            // skipping compilation entirely
            try (final var session = engine.newSession()) {
                assertEquals(42L, session.eval(engine.load(turc)));
            }
        }
        // end snippet
    }

    @Test
    @Timeout(10)
    void scriptsCanUseAsyncTasks() {
        // snippet async_scripts
        final var policy = SandboxPolicy.trusted()
                .maxThreads(4)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // scripts may use the language's full concurrency toolbox; the sandbox only
            // caps how many interpreter threads may run at the same time
            assertEquals(3L, session.eval("""
                    let t1 = async { 1 }
                    let t2 = async { 2 }
                    let r1 = await t1
                    let r2 = await t2
                    r1 + r2
                    """));
        }
        // end snippet
    }

    @Test
    void syntaxErrorsAreReportedAtCompileTime() {
        // snippet compile_errors
        try (final var engine = TuriEngine.create()) {
            // syntax errors surface at compile(), before any script code runs
            assertThrows(BadSyntax.class, () -> engine.compile("let let let"));
        }
        // end snippet
    }

    @Test
    void runtimeErrorsCarryScriptLevelStackTraces() {
        // snippet runtime_errors
        try (final var engine = TuriEngine.create();
             final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class, () -> session.eval("""
                    fn explode() {
                        die "something went wrong"
                    }
                    explode()
                    """));
            // the exception message and stack trace point into the script, not into
            // the interpreter's Java internals
            assertTrue(e.getMessage().contains("something went wrong"));
        }
        // end snippet
    }
}
