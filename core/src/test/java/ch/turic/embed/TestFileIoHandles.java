package ch.turic.embed;

import ch.turic.Capability;
import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of the file I/O handle built-ins: the sequential {@code file_reader}/{@code file_writer}
 * (FILE_IO.md §5.3), the random-access handles (§5.4), the memory-mapped handles (§5.5) with the
 * {@code maxMappedBytes} budget, the {@code with} command integration, and the session-end
 * force-close.
 */
class TestFileIoHandles {

    private static TuriEngine unrestricted() {
        return TuriEngine.create();
    }

    // ---- sequential handles (§5.3) -------------------------------------------------------------

    @Test
    void readerReadsLinesChunksAndAll(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("in.txt"), "alpha\nbeta\ngamma");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("in.txt").toString());
            assertEquals("alpha", session.eval("let r = file_reader(p)\nr.read_line()"));
            assertEquals("be", session.eval("r.read(2)"));
            assertEquals("ta\ngamma", session.eval("r.read_all()"));
            // at EOF read_line and read(n) return none, read_all returns the empty rest
            assertNull(session.eval("r.read_line()"));
            assertNull(session.eval("r.read(1)"));
            assertEquals("", session.eval("r.read_all()"));
            session.eval("r.close()");
            // close is idempotent, everything else on a closed handle is an error
            session.eval("r.close()");
            final var e = assertThrows(ExecutionException.class, () -> session.eval("r.read_line()"));
            assertTrue(e.getMessage().contains("closed"), e.getMessage());
        }
    }

    @Test
    void binaryReaderReturnsBin(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("in.bin"), new byte[]{1, 2, 3, (byte) 255});
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("in.bin").toString());
            session.eval("let r = file_reader(p, binary=true)");
            assertEquals(true, session.eval("r.read(2) == bin([1, 2])"));
            assertEquals(true, session.eval("r.read_all() == bin([3, 255])"));
            assertNull(session.eval("r.read(1)"));
            // read_line has no meaning on a binary reader
            session.eval("r.close()");
            session.eval("let r2 = file_reader(p, binary=true)");
            assertThrows(ExecutionException.class, () -> session.eval("r2.read_line()"));
            session.eval("r2.close()");
        }
    }

    @Test
    void writerWithCommandClosesOnEveryPath(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("report.txt").toString());
            session.eval("""
                    with file_writer(p) as out {
                        out.write_line("header")
                        out.write("body")
                    }
                    """);
            assertEquals("header\nbody", Files.readString(dir.resolve("report.txt")));

            // the handle variable survives the with block, but it is closed
            session.set("q", dir.resolve("second.txt").toString());
            session.eval("let h = file_writer(q)\nwith h as out { out.write(\"x\") }");
            final var e = assertThrows(ExecutionException.class, () -> session.eval("h.write(\"y\")"));
            assertTrue(e.getMessage().contains("closed"), e.getMessage());
            assertEquals("x", Files.readString(dir.resolve("second.txt")));
        }
    }

    @Test
    void withClosesAndFlushesOnException(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("partial.txt").toString());
            assertThrows(ExecutionException.class, () -> session.eval("""
                    with file_writer(p) as out {
                        out.write("before the failure")
                        die "boom"
                    }
                    """));
            // exit ran in the finally: the buffered content was flushed by the close
            assertEquals("before the failure", Files.readString(dir.resolve("partial.txt")));
        }
    }

    @Test
    void unclosedHandlesAreForceClosedWithTheSession(@TempDir Path dir) throws Exception {
        final var file = dir.resolve("leak.txt");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", file.toString());
            session.eval("let w = file_writer(p)\nw.write(\"flushed by the force-close\")");
            // no close: the buffered content may not be on disk yet; the session close must
            // force-close the handle, which flushes it
        }
        assertEquals("flushed by the force-close", Files.readString(file));
    }

    @Test
    void binaryWriterAcceptsBinOnly(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("out.bin").toString());
            session.eval("let w = file_writer(p, binary=true)");
            assertThrows(ExecutionException.class, () -> session.eval("w.write(\"str\")"));
            assertThrows(ExecutionException.class, () -> session.eval("w.write_line(bin([1]))"));
            session.eval("w.write(bin([104, 105]))\nw.close()");
            assertArrayEquals(new byte[]{104, 105}, Files.readAllBytes(dir.resolve("out.bin")));
        }
    }

    @Test
    void writerDemandsCreateAtRuntime(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("existing.txt"), "old");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE) // no FILE_CREATE
                .fileReadWriteRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.eval("with file_writer(\"existing.txt\") as out { out.write(\"new\") }");
            assertEquals("new", Files.readString(dir.resolve("existing.txt")));
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_writer(\"fresh.txt\")"));
            assertTrue(e.getMessage().contains("FILE_CREATE"), e.getMessage());
        }
    }

    // ---- random access (§5.4) ------------------------------------------------------------------

    @Test
    void randomReaderSeeksAndReads(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "0123456789");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.txt").toString());
            session.eval("let r = file_random_reader(p)");
            assertEquals(10L, session.eval("r.size()"));
            assertEquals(0L, session.eval("r.position()"));
            assertEquals(true, session.eval("r.read(3) == \"012\".bytes()"));
            assertEquals(3L, session.eval("r.position()"));
            session.eval("r.seek(8)");
            assertEquals(true, session.eval("r.read(5) == \"89\".bytes()"));
            // absolute reads do not move the position
            assertEquals(10L, session.eval("r.position()"));
            assertEquals(true, session.eval("r.read_at(4, 2) == \"45\".bytes()"));
            assertEquals(10L, session.eval("r.position()"));
            // reads at EOF: none
            assertNull(session.eval("r.read(1)"));
            // the reader handle has no write methods at all
            assertThrows(ExecutionException.class, () -> session.eval("r.write(\"x\".bytes())"));
            session.eval("r.close()");
        }
    }

    @Test
    void randomEditorPatchesInPlace(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "0123456789");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.txt").toString());
            session.eval("""
                    with file_random_editor(p) as f {
                        f.write_at(2, "XY".bytes())
                        f.seek(f.size())
                        f.write("!".bytes())
                    }
                    """);
            assertEquals("01XY456789!", Files.readString(dir.resolve("data.txt")));
            session.eval("with file_random_editor(p) as f { f.truncate(4) }");
            assertEquals("01XY", Files.readString(dir.resolve("data.txt")));
        }
    }

    @Test
    void randomEditorCreateSemantics(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("fresh.bin").toString());
            // a missing file without create=true is an error
            assertThrows(ExecutionException.class, () -> session.eval("file_random_editor(p)"));
            session.eval("with file_random_editor(p, create=true) as f { f.write(bin([7])) }");
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(dir.resolve("fresh.bin")));
        }
        // in a sandbox, create=true on a missing file demands FILE_CREATE at runtime
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE)
                .fileReadWriteRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_random_editor(\"another.bin\", create=true)"));
            assertTrue(e.getMessage().contains("FILE_CREATE"), e.getMessage());
        }
    }

    // ---- memory-mapped files (§5.5, D10) -------------------------------------------------------

    @Test
    void mapReaderReadsTheRegion(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "0123456789");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.txt").toString());
            session.eval("let m = file_map_reader(p)");
            assertEquals(10L, session.eval("m.length()"));
            assertEquals(true, session.eval("m.get(3, 4) == \"3456\".bytes()"));
            // a region: offset+length window
            session.eval("let w = file_map_reader(p, offset=5, length=3)");
            assertEquals(3L, session.eval("w.length()"));
            assertEquals(true, session.eval("w.get(0, 3) == \"567\".bytes()"));
            // out-of-region access is an error; a reader has no put at all
            assertThrows(ExecutionException.class, () -> session.eval("w.get(2, 2)"));
            assertThrows(ExecutionException.class, () -> session.eval("w.put(0, bin([1]))"));
            session.eval("m.close()\nw.close()");
            final var e = assertThrows(ExecutionException.class, () -> session.eval("m.get(0, 1)"));
            assertTrue(e.getMessage().contains("closed"), e.getMessage());
        }
    }

    @Test
    void mapEditorWritesThrough(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "0123456789");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.txt").toString());
            session.eval("""
                    with file_map_editor(p) as m {
                        m.put(0, "AB".bytes())
                        m.put(8, "YZ".bytes())
                    }
                    """);
            assertEquals("AB234567YZ", Files.readString(dir.resolve("data.txt")));
        }
    }

    @Test
    void mappedBytesAreBudgeted(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "0123456789");
        // the untrusted default budget is 0: mmap is effectively unusable without opting in
        final var closedPolicy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(closedPolicy); final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_map_reader(\"data.txt\")"));
            assertTrue(e.getMessage().contains("mapped bytes"), e.getMessage());
        }
        // with a budget, mappings work; closing returns the budget
        final var openPolicy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(dir)
                .maxMappedBytes(10)
                .build();
        try (final var engine = TuriEngine.create(openPolicy); final var session = engine.newSession()) {
            session.eval("let m = file_map_reader(\"data.txt\")");
            // the whole budget is in use: a second full mapping does not fit
            assertThrows(ExecutionException.class, () -> session.eval("file_map_reader(\"data.txt\")"));
            // a closed mapping returns its budget
            session.eval("m.close()");
            session.eval("let m2 = file_map_reader(\"data.txt\")");
            assertEquals(true, session.eval("m2.get(0, 2) == \"01\".bytes()"));
            session.eval("m2.close()");
        }
    }

    // ---- with statement and HasFields-only resources -------------------------------------------

    @Test
    void handleWithoutAliasNeedsAnAlias(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("in.txt"), "x");
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("in.txt").toString());
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("with file_reader(p) { }"));
            assertTrue(e.getMessage().contains("'as' alias"), e.getMessage());
        }
    }
}
