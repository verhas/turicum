package ch.turic.embed;

import ch.turic.Capability;
import ch.turic.exceptions.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of the file I/O built-in family ({@code ch.turic.builtins.functions.fileio}): plain
 * behavior, capability hiding, runtime capability demands, sandbox root confinement, and the
 * temp scratch area. See {@code FILE_IO.md} for the design.
 */
class TestFileIo {

    private static TuriEngine unrestricted() {
        return TuriEngine.create();
    }

    // ---- plain behavior (unrestricted) ---------------------------------------------------------

    @Test
    void textWriteReadRoundTrip(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("hello.txt").toString());
            session.eval("file_write(p, \"hello wörld\")");
            assertEquals("hello wörld", session.eval("file_read(p)"));
        }
    }

    @Test
    void charsetIsHonored(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("latin.txt").toString());
            session.eval("file_write(p, \"höllo\", charset=\"ISO-8859-1\")");
            assertEquals("höllo", session.eval("file_read(p, charset=\"ISO-8859-1\")"));
            // read with the wrong charset differs
            assertNotEquals("höllo", session.eval("file_read(p)"));
        }
    }

    @Test
    void binaryWriteReadRoundTrip(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.bin").toString());
            session.eval("file_write(p, bin([0, 1, 127, 128, 255]), binary=true)");
            final var read = session.eval("file_read(p, binary=true)");
            assertArrayEquals(new byte[]{0, 1, 127, (byte) 128, (byte) 255}, (byte[]) read);
            // the bin surface is unsigned
            assertEquals(true, session.eval("file_read(p, binary=true)[4] == 255"));
        }
    }

    @Test
    void binaryWriteAcceptsBinOnly(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("data.bin").toString());
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_write(p, \"text\", binary=true)"));
            assertTrue(e.getMessage().contains("bin content"), e.getMessage());
        }
    }

    @Test
    void fileLinesSplitsWithoutTerminators(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("lines.txt").toString());
            session.eval("file_write(p, \"one\\ntwo\\nthree\\n\")");
            assertEquals(3L, session.eval("len(file_lines(p))"));
            assertEquals("two", session.eval("file_lines(p)[1]"));
        }
    }

    @Test
    void appendAndOverwriteModes(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("log.txt").toString());
            session.eval("file_write(p, \"a\")");
            session.eval("file_write(p, \"b\", append=true)");
            assertEquals("ab", session.eval("file_read(p)"));
            // overwrite=false on an existing file is an error
            assertThrows(ExecutionException.class,
                    () -> session.eval("file_write(p, \"c\", overwrite=false)"));
            assertEquals("ab", session.eval("file_read(p)"));
            // ... but works for a fresh file
            session.set("q", dir.resolve("fresh.txt").toString());
            session.eval("file_write(q, \"c\", overwrite=false)");
            assertEquals("c", session.eval("file_read(q)"));
        }
    }

    @Test
    void mkdirsOptionCreatesParents(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("p", dir.resolve("a/b/c/deep.txt").toString());
            assertThrows(ExecutionException.class, () -> session.eval("file_write(p, \"x\")"));
            session.eval("file_write(p, \"x\", mkdirs=true)");
            assertEquals("x", session.eval("file_read(p)"));
        }
    }

    @Test
    void metadataFunctions(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("f", dir.resolve("f.txt").toString());
            session.set("d", dir.toString());
            session.set("missing", dir.resolve("missing").toString());
            session.eval("file_write(f, \"12345\")");
            assertEquals(true, session.eval("file_exists(f)"));
            assertEquals(false, session.eval("file_exists(missing)"));
            assertEquals(true, session.eval("is_file(f)"));
            assertEquals(false, session.eval("is_file(d)"));
            assertEquals(true, session.eval("is_dir(d)"));
            assertEquals(false, session.eval("is_dir(f)"));
            assertEquals(5L, session.eval("file_stat(f).size"));
            assertEquals(true, session.eval("file_stat(f).is_file"));
            assertEquals(false, session.eval("file_stat(f).is_dir"));
            assertEquals(false, session.eval("file_stat(f).is_symlink"));
            assertEquals(true, session.eval("file_stat(f).readable"));
            assertEquals(true, session.eval("file_stat(f).modified > 0"));
            assertThrows(ExecutionException.class, () -> session.eval("file_stat(missing)"));
        }
    }

    @Test
    void mkdirAndDelete(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("d", dir.resolve("x/y/z").toString());
            session.eval("mkdir(d)");
            assertEquals(true, session.eval("is_dir(d)"));
            // non-recursive mkdir of an existing directory is an error
            assertThrows(ExecutionException.class, () -> session.eval("mkdir(d, recurse=false)"));

            session.set("f", dir.resolve("x/y/z/file.txt").toString());
            session.eval("file_write(f, \"data\")");
            session.set("top", dir.resolve("x").toString());
            // deleting a non-empty directory without force is an error
            final var e = assertThrows(ExecutionException.class, () -> session.eval("file_delete(top)"));
            assertTrue(e.getMessage().contains("force=true"), e.getMessage());
            assertEquals(true, session.eval("file_delete(top, force=true)"));
            assertEquals(false, session.eval("file_exists(top)"));
            // deleting a missing path: false, or an error with must_exist
            assertEquals(false, session.eval("file_delete(top)"));
            assertThrows(ExecutionException.class, () -> session.eval("file_delete(top, must_exist=true)"));
        }
    }

    @Test
    void copyAndMove(@TempDir Path dir) throws Exception {
        try (final var engine = unrestricted(); final var session = engine.newSession()) {
            session.set("src", dir.resolve("src.txt").toString());
            session.set("dst", dir.resolve("dst.txt").toString());
            session.eval("file_write(src, \"payload\")");
            session.eval("file_copy(src, dst)");
            assertEquals("payload", session.eval("file_read(dst)"));
            // copying over an existing target needs overwrite=true
            assertThrows(ExecutionException.class, () -> session.eval("file_copy(src, dst)"));
            session.eval("file_write(src, \"payload2\")");
            session.eval("file_copy(src, dst, overwrite=true)");
            assertEquals("payload2", session.eval("file_read(dst)"));

            session.set("moved", dir.resolve("moved.txt").toString());
            session.eval("file_move(src, moved)");
            assertEquals(false, session.eval("file_exists(src)"));
            assertEquals("payload2", session.eval("file_read(moved)"));
        }
    }

    // ---- capability gating ---------------------------------------------------------------------

    @Test
    void readOnlyGrantHidesTheMutatingBuiltins(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("data.txt"), "readable");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            assertEquals("readable", session.eval("file_read(\"data.txt\")"));
            // the mutating built-ins are not even registered
            for (final var call : new String[]{
                    "file_write(\"data.txt\", \"x\")",
                    "mkdir(\"sub\")",
                    "file_delete(\"data.txt\")",
                    "file_move(\"data.txt\", \"y\")",
                    "tmp_file()"}) {
                assertThrows(ExecutionException.class, () -> session.eval(call), call);
            }
        }
    }

    @Test
    void writeImpliesRead(@TempDir Path dir) throws Exception {
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE, Capability.FILE_CREATE)
                .fileReadWriteRoot(dir)
                .build();
        assertTrue(policy.grantedCapabilities().contains(Capability.FILE_READ));
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.eval("file_write(\"out.txt\", \"written\")");
            assertEquals("written", session.eval("file_read(\"out.txt\")"));
        }
    }

    @Test
    void createIsARuntimeDemandOfWrite(@TempDir Path dir) throws Exception {
        // snippet file_create_demand
        Files.writeString(dir.resolve("existing.txt"), "old");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE) // no FILE_CREATE
                .fileReadWriteRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            // updating an existing file is fine
            session.eval("file_write(\"existing.txt\", \"new\")");
            assertEquals("new", session.eval("file_read(\"existing.txt\")"));
            // creating a new one demands FILE_CREATE at runtime
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_write(\"fresh.txt\", \"x\")"));
            assertTrue(e.getMessage().contains("FILE_CREATE"), e.getMessage());
        }
        // end snippet
    }

    @Test
    void copyDemandsTheTargetSideCapabilityAtRuntime(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("src.txt"), "data");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ, Capability.FILE_WRITE) // no FILE_CREATE
                .fileReadWriteRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_copy(\"src.txt\", \"dst.txt\")"));
            assertTrue(e.getMessage().contains("FILE_CREATE"), e.getMessage());
            // with an existing target and overwrite, FILE_WRITE suffices
            Files.writeString(dir.resolve("dst.txt"), "old");
            session.eval("file_copy(\"src.txt\", \"dst.txt\", overwrite=true)");
            assertEquals("data", Files.readString(dir.resolve("dst.txt")));
        }
    }

    @Test
    void importIsNotEnabledByFileRead(@TempDir Path dir) throws Exception {
        // D2: IMPORT is split from FILE_READ; a data-read grant does not enable imports
        Files.writeString(dir.resolve("lib.turi"), "let answer = 42\nexport_all()\n");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.set("root", dir.toString());
            assertThrows(Exception.class, () -> session.eval("global APPIA = [root]\nimport \"lib\"\nanswer"));
        }
    }

    // ---- root confinement ----------------------------------------------------------------------

    @Test
    void absolutePathOutsideTheRootsIsDenied(@TempDir Path dir, @TempDir Path outside) throws Exception {
        Files.writeString(outside.resolve("secret.txt"), "secret");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.set("secret", outside.resolve("secret.txt").toString());
            final var e = assertThrows(ExecutionException.class, () -> session.eval("file_read(secret)"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
    }

    @Test
    void dotDotCannotEscapeTheRoots(@TempDir Path workDir) throws Exception {
        final var root = Files.createDirectory(workDir.resolve("root"));
        Files.writeString(workDir.resolve("secret.txt"), "secret");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(root)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_read(\"../secret.txt\")"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
    }

    @Test
    void relativeReadsSearchTheRootsInDeclarationOrder(@TempDir Path workDir) throws Exception {
        final var first = Files.createDirectory(workDir.resolve("first"));
        final var second = Files.createDirectory(workDir.resolve("second"));
        Files.writeString(second.resolve("only-in-second.txt"), "from second");
        Files.writeString(first.resolve("in-both.txt"), "from first");
        Files.writeString(second.resolve("in-both.txt"), "from second");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(first, second)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            // the first existing candidate wins, roots searched in declaration order
            assertEquals("from first", session.eval("file_read(\"in-both.txt\")"));
            assertEquals("from second", session.eval("file_read(\"only-in-second.txt\")"));
            // a file existing nowhere reports all searched roots
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_read(\"nowhere.txt\")"));
            assertTrue(e.getMessage().contains("was not found under any"), e.getMessage());
        }
    }

    @Test
    void relativeWritesResolveAgainstTheFirstReadWriteRoot(@TempDir Path workDir) throws Exception {
        final var readOnly = Files.createDirectory(workDir.resolve("ro"));
        final var rw1 = Files.createDirectory(workDir.resolve("rw1"));
        final var rw2 = Files.createDirectory(workDir.resolve("rw2"));
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE, Capability.FILE_CREATE)
                .fileReadRoot(readOnly)
                .fileReadWriteRoot(rw1, rw2)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.eval("file_write(\"out.txt\", \"deterministic\")");
            assertEquals("deterministic", Files.readString(rw1.resolve("out.txt")));
            assertFalse(Files.exists(rw2.resolve("out.txt")));
        }
    }

    @Test
    void mutationsAreDeniedUnderReadOnlyRoots(@TempDir Path workDir) throws Exception {
        // snippet file_roots
        final var readOnly = Files.createDirectory(workDir.resolve("ro"));
        final var rw = Files.createDirectory(workDir.resolve("rw"));
        Files.writeString(readOnly.resolve("data.txt"), "protected");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_WRITE, Capability.FILE_CREATE, Capability.FILE_DELETE)
                .fileReadRoot(readOnly)      // data the script may only read
                .fileReadWriteRoot(rw)       // the tree it may modify
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.set("protected_file", readOnly.resolve("data.txt").toString());
            // reading from the read-only root works
            assertEquals("protected", session.eval("file_read(protected_file)"));
            // writing and deleting there is denied
            assertThrows(ExecutionException.class, () -> session.eval("file_write(protected_file, \"x\")"));
            assertThrows(ExecutionException.class, () -> session.eval("file_delete(protected_file)"));
            assertEquals("protected", Files.readString(readOnly.resolve("data.txt")));
        }
        // end snippet
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void symlinksCannotAliasFilesOutsideTheRoots(@TempDir Path workDir) throws Exception {
        final var root = Files.createDirectory(workDir.resolve("root"));
        final var secret = Files.writeString(workDir.resolve("secret.txt"), "secret");
        Files.createSymbolicLink(root.resolve("innocent.txt"), secret);
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(root)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("file_read(\"innocent.txt\")"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void symlinksInsideTheRootsAreFollowed(@TempDir Path workDir) throws Exception {
        final var root = Files.createDirectory(workDir.resolve("root"));
        Files.writeString(root.resolve("target.txt"), "linked data");
        Files.createSymbolicLink(root.resolve("link.txt"), root.resolve("target.txt"));
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(root)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            assertEquals("linked data", session.eval("file_read(\"link.txt\")"));
        }
    }

    // ---- temp files (D8) -----------------------------------------------------------------------

    @Test
    void tempFilesLiveInTheScratchAreaAndDieWithTheSession() throws Exception {
        // snippet file_temp
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_TEMP)
                .build(); // no roots needed: the scratch dir is the implicit read-write root
        assertTrue(policy.grantedCapabilities().containsAll(java.util.Set.of(
                Capability.FILE_READ, Capability.FILE_WRITE, Capability.FILE_CREATE, Capability.FILE_DELETE)));
        final Path tmpPath;
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            final var tmp = (String) session.eval("tmp_file(prefix=\"job-\", suffix=\".txt\")");
            tmpPath = Path.of(tmp);
            assertTrue(Files.exists(tmpPath));
            assertTrue(tmpPath.getFileName().toString().startsWith("job-"));
            session.set("p", tmp);
            session.eval("file_write(p, \"scratch\")");
            assertEquals("scratch", session.eval("file_read(p)"));
            // a directory, and a file created relative to the scratch root
            final var d = (String) session.eval("tmp_dir()");
            assertTrue(Files.isDirectory(Path.of(d)));
            session.eval("file_write(\"relative.txt\", \"under the scratch root\")");
            // absolute paths outside the scratch area stay denied
            session.set("outside", "/etc/passwd");
            assertThrows(ExecutionException.class, () -> session.eval("file_read(outside)"));
        }
        // the scratch area is deleted when the session closes
        assertFalse(Files.exists(tmpPath));
        assertFalse(Files.exists(tmpPath.getParent()));
        // end snippet
    }

    // ---- glob under the file roots (Phase D) ---------------------------------------------------

    @Test
    void globIsConfinedByTheFileRoots(@TempDir Path workDir) throws Exception {
        final var root1 = Files.createDirectory(workDir.resolve("root1"));
        final var root2 = Files.createDirectory(workDir.resolve("root2"));
        Files.writeString(root1.resolve("a.txt"), "");
        Files.writeString(root2.resolve("b.txt"), "");
        Files.writeString(workDir.resolve("outside.txt"), "");
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(root1, root2)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            // a relative pattern is globbed under every root; the results are the union
            assertEquals(2L, session.eval("len(glob(\"*.txt\"))"));
            // an absolute pattern outside the roots is a policy denial
            session.set("above", workDir.toString());
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("glob(above + \"/*.txt\")"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void globOmitsSymlinkEscapes(@TempDir Path workDir) throws Exception {
        final var root = Files.createDirectory(workDir.resolve("root"));
        Files.writeString(root.resolve("fine.txt"), "");
        Files.writeString(workDir.resolve("secret.txt"), "");
        Files.createSymbolicLink(root.resolve("sneaky.txt"), workDir.resolve("secret.txt"));
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .fileReadRoot(root)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            // the symlink pointing outside the root is silently omitted from the listing
            assertEquals("[\"fine.txt\"]", session.eval("jsonify(glob(\"*.txt\"))"));
        }
    }

    // ---- turi.io without reflection (Phase D) --------------------------------------------------

    @Test
    void turiIoWorksWithoutJavaReflection(@TempDir Path dir) throws Exception {
        // the turi.io system library delegates to the file built-ins, so it works in a
        // sandbox that denies JAVA_REFLECTION
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.IMPORT, Capability.FILE_WRITE, Capability.FILE_CREATE)
                .importRoot(dir)
                .fileReadWriteRoot(dir)
                .build();
        try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
            session.eval("""
                    sys_import "turi.io"
                    files.write("greeting.txt", "hello from turi.io")
                    """);
            assertEquals("hello from turi.io", Files.readString(dir.resolve("greeting.txt")));
            assertEquals("hello from turi.io",
                    session.eval("files.read_all_lines(\"greeting.txt\")[0]"));
        }
    }

    // ---- policy build rules --------------------------------------------------------------------

    @Test
    void trustedCannotDenyReadWhileKeepingWrite() {
        assertThrows(IllegalStateException.class,
                () -> SandboxPolicy.trusted().deny(Capability.FILE_READ).build());
        // denying the whole family is fine
        assertDoesNotThrow(() -> SandboxPolicy.trusted().deny(
                Capability.FILE_READ, Capability.FILE_WRITE, Capability.FILE_CREATE,
                Capability.FILE_DELETE, Capability.FILE_TEMP).build());
    }

    @Test
    void fileTempAloneIsAValidUntrustedPolicy() {
        assertDoesNotThrow(() -> SandboxPolicy.untrusted().allow(Capability.FILE_TEMP).build());
    }
}
