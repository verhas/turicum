package ch.turic.embed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the {@code TuriInputStream} and {@code TuriInputStreamReader}
 * {@link ch.turic.TuriClass} classes from Turicum source: a script obtains a
 * {@code java.io.InputStream} (or {@code InputStreamReader}) through Java interop
 * ({@code java_object}) and reads a test file with the curated methods — {@code read_all},
 * {@code read}, {@code read_all_bytes}, {@code available}, {@code close} on the stream, and
 * {@code read_char}, {@code close} on the reader. Runs under {@link SandboxPolicy#UNRESTRICTED},
 * i.e. full Java access, the same as the plain interpreter.
 */
class TestEmbeddingInputStream {

    @Test
    void readAllReadsTheWholeFileAsText(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("greeting.txt");
        Files.writeString(file, "hello input stream\nsecond line\n", StandardCharsets.UTF_8);

        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            session.set("path", file.toString());
            final var content = session.eval("""
                    let is = java_object("java.io.FileInputStream", path)
                    let text = is.read_all()
                    is.close()
                    text
                    """);
            assertEquals("hello input stream\nsecond line\n", content);
        }
    }

    @Test
    void readConsumesTheStreamByteByByte(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("abc.txt");
        Files.writeString(file, "ABC", StandardCharsets.UTF_8);

        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            session.set("path", file.toString());
            // read() yields the next unsigned byte and advances the stream, then -1 at EOF.
            // The comparison is done in Turicum so the test does not depend on how a Java int
            // return is represented back in Java.
            final var ok = session.eval("""
                    let is = java_object("java.io.FileInputStream", path)
                    let a = is.read()
                    let b = is.read()
                    let c = is.read()
                    let d = is.read()
                    is.close()
                    a == 65 && b == 66 && c == 67 && d == -1
                    """);
            assertEquals(true, ok);
        }
    }

    @Test
    void readAllBytesReturnsTheRawBytes(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("bytes.bin");
        Files.write(file, new byte[]{1, 2, 3, 4});

        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            session.set("path", file.toString());
            // read_all_bytes() returns a Java byte[]; len() works on primitive arrays
            final var length = session.eval("""
                    let is = java_object("java.io.FileInputStream", path)
                    let bytes = is.read_all_bytes()
                    is.close()
                    len(bytes)
                    """);
            assertEquals(4L, length);
        }
    }

    // ---- InputStreamReader (character reading) -----------------------------------------------

    @Test
    void readerReadsCharactersOneByOne(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("chars.txt");
        Files.writeString(file, "ABC", StandardCharsets.UTF_8);

        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            session.set("path", file.toString());
            // FileReader is an InputStreamReader, so read_char()/close() dispatch through
            // TuriInputStreamReader, which keeps one decoding reader across calls
            final var result = session.eval("""
                    let r = java_object("java.io.FileReader", path)
                    let a = r.read_char()
                    let b = r.read_char()
                    let c = r.read_char()
                    let ended = r.read_char() == none
                    r.close()
                    let out = [a + b + c, ended]
                    out
                    """);
            assertEquals(java.util.List.of("ABC", true), ((ch.turic.memory.LngList) result).array);
        }
    }

    @Test
    void readerWrappingAStreamDecodesUtf8(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("utf8.txt");
        Files.writeString(file, "áé", StandardCharsets.UTF_8);

        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            session.set("path", file.toString());
            // wrap a FileInputStream in an InputStreamReader explicitly; two UTF-8 multi-byte
            // characters must decode to exactly two characters
            final var result = session.eval("""
                    let is = java_object("java.io.FileInputStream", path)
                    let r = java_object("java.io.InputStreamReader", is)
                    let a = r.read_char()
                    let b = r.read_char()
                    let ended = r.read_char() == none
                    r.close()
                    let out = [a + b, ended]
                    out
                    """);
            assertEquals(java.util.List.of("áé", true), ((ch.turic.memory.LngList) result).array);
        }
    }
}
