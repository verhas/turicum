package ch.turic.builtins.functions.fileio;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared path-confinement helper used by every file I/O built-in, mirroring the confinement of
 * {@code AppiaHandler.confineToImportRoot()} for imports.
 * <p>
 * The sandbox policy may configure two sets of file roots: read-only roots and read-write
 * roots (see {@code SandboxPolicy.Builder#fileReadRoot}/{@code #fileReadWriteRoot}); the
 * session's temp scratch directory, when it exists, counts as an additional read-write root.
 * When no root is configured at all, file access is unconfined (trusted/unrestricted default)
 * and relative paths resolve against the process working directory. When roots are configured:
 * <ul>
 * <li><b>read/metadata operations</b> must resolve under <em>any</em> root of either set;
 *     relative paths are searched against the roots in declaration order (read-only roots
 *     first, then read-write roots, then the scratch directory), taking the first candidate
 *     that exists — the APPIA-search flavor;</li>
 * <li><b>write/create/delete operations</b> must resolve under a read-write root; relative
 *     paths resolve against the <em>first</em> declared read-write root — a write target is
 *     deterministic, never search-dependent.</li>
 * </ul>
 * Symlinks are followed, but the confinement check runs on the <em>real</em> path (of the
 * deepest existing ancestor, for paths about to be created), so a symlink inside a root cannot
 * alias a file outside it. A path outside every applicable root fails with a policy-denial
 * {@link ExecutionException}, never a plain "not found".
 */
public final class SafePath {

    private SafePath() {
    }

    /**
     * The result of resolving a script-supplied path for a read or metadata operation. The
     * {@link #path()} is absolutized and normalized but <em>not</em> symlink-resolved, so
     * metadata operations (e.g. {@code file_stat}'s {@code is_symlink}) see the entry the
     * script named; the confinement check has already run on the real path.
     */
    public record Resolved(Path path, boolean exists, String script, List<Path> searched) {

        /**
         * Returns the resolved path, failing when the file does not exist. Used by the
         * operations that need an existing file ({@code file_read}, {@code file_lines}, the
         * reader handles, a copy source); pure metadata operations use {@link #path()} and
         * {@link #exists()} directly.
         *
         * @return the resolved path of an existing file
         * @throws ExecutionException if the file does not exist; when the path was searched
         *                            against sandbox roots, the error names all of them
         */
        public Path requireExists() throws ExecutionException {
            if (exists) {
                return path;
            }
            if (searched.isEmpty()) {
                throw new ExecutionException("File '%s' does not exist ('%s')", script, path);
            }
            throw new ExecutionException("File '%s' was not found under any of the sandbox file roots [%s]",
                    script, joined(searched));
        }
    }

    /**
     * Resolves and confines a script-supplied path for a read or metadata operation.
     *
     * @param globalContext the interpreter's global context carrying the root sets
     * @param script        the path as the script supplied it
     * @return the resolution result; never {@code null}
     * @throws ExecutionException if the path resolves outside every applicable root
     */
    public static Resolved forRead(GlobalContext globalContext, String script) throws ExecutionException {
        final var roots = readRoots(globalContext);
        final var p = Path.of(script);
        if (roots.isEmpty()) {
            final var abs = p.toAbsolutePath().normalize();
            return new Resolved(abs, Files.exists(abs), script, List.of());
        }
        if (p.isAbsolute()) {
            final var abs = p.normalize();
            confine(script, abs, roots);
            return new Resolved(abs, Files.exists(abs), script, List.of());
        }
        Path first = null;
        for (final var root : roots) {
            final var candidate = root.resolve(p).normalize();
            confine(script, candidate, roots);
            if (first == null) {
                first = candidate;
            }
            if (Files.exists(candidate)) {
                return new Resolved(candidate, true, script, roots);
            }
        }
        return new Resolved(first, false, script, roots);
    }

    /**
     * Resolves and confines a script-supplied path for a mutating operation (write, create,
     * delete, move — both sides, copy target).
     *
     * @param globalContext the interpreter's global context carrying the root sets
     * @param script        the path as the script supplied it
     * @return the absolutized, normalized target path
     * @throws ExecutionException if the path resolves outside every read-write root, or file
     *                            roots are configured but none of them is read-write
     */
    public static Path forWrite(GlobalContext globalContext, String script) throws ExecutionException {
        final var writeRoots = writeRoots(globalContext);
        final var confined = !readRoots(globalContext).isEmpty();
        final var p = Path.of(script);
        if (!confined) {
            return p.toAbsolutePath().normalize();
        }
        if (writeRoots.isEmpty()) {
            throw new ExecutionException(
                    "File path '%s' cannot be modified: the sandbox policy configures no read-write file root",
                    script);
        }
        final var abs = p.isAbsolute() ? p.normalize() : writeRoots.getFirst().resolve(p).normalize();
        confine(script, abs, writeRoots);
        return abs;
    }

    /**
     * Tests whether a path is readable under the sandbox file roots, without throwing. Used
     * by {@code glob}, which silently drops results a symlink would carry outside the roots
     * instead of failing the whole listing.
     *
     * @param globalContext the interpreter's global context carrying the root sets
     * @param abs           the absolutized path of a result candidate
     * @return whether the (real) path falls under a root, or no roots are configured
     */
    static boolean isReadable(GlobalContext globalContext, Path abs) {
        final var roots = readRoots(globalContext);
        if (roots.isEmpty()) {
            return true;
        }
        try {
            confine(abs.toString(), abs, roots);
            return true;
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * @param globalContext the interpreter's global context
     * @return whether any file root (including the temp scratch directory) is configured,
     * i.e. whether file access is confined at all
     */
    static boolean isConfined(GlobalContext globalContext) {
        return !readRoots(globalContext).isEmpty();
    }

    /**
     * The read-resolution root list in search order, for the built-ins that need to iterate
     * the roots themselves (e.g. {@code glob} unioning its results over the roots).
     */
    static List<Path> readRootList(GlobalContext globalContext) {
        return readRoots(globalContext);
    }

    /**
     * The roots a read/metadata operation may resolve under, in relative-resolution search
     * order: read-only roots, then read-write roots, then the temp scratch directory.
     */
    private static List<Path> readRoots(GlobalContext globalContext) {
        final var roots = new ArrayList<>(globalContext.fileReadRoots());
        roots.addAll(globalContext.fileReadWriteRoots());
        final var temp = globalContext.tempRootIfCreated();
        if (temp != null) {
            roots.add(temp);
        }
        return roots;
    }

    /**
     * The roots a mutating operation may resolve under: the read-write roots, then the temp
     * scratch directory. The first element is the resolution base of relative targets.
     */
    private static List<Path> writeRoots(GlobalContext globalContext) {
        final var roots = new ArrayList<>(globalContext.fileReadWriteRoots());
        final var temp = globalContext.tempRootIfCreated();
        if (temp != null) {
            roots.add(temp);
        }
        return roots;
    }

    /**
     * Requires the resolved path to fall under one of the given roots, checking the
     * <em>real</em> path so that neither {@code ..} segments nor symlinks can escape a root.
     *
     * @param script   the path as the script supplied it, for the error message
     * @param resolved the absolutized, normalized path to check
     * @param roots    the applicable roots
     * @throws ExecutionException if the real path is outside every root
     */
    private static void confine(String script, Path resolved, List<Path> roots) throws ExecutionException {
        final var probe = realProbe(resolved);
        for (final var root : roots) {
            if (probe.startsWith(realRoot(root))) {
                return;
            }
        }
        throw new ExecutionException(
                "File path '%s' resolves to '%s', outside the sandbox file roots [%s], and is denied by the sandbox policy",
                script, probe, joined(roots));
    }

    /**
     * Canonicalizes a path for the confinement check: the deepest existing ancestor is
     * resolved with {@link Path#toRealPath} (following symlinks), and the not-yet-existing
     * remainder — relevant for paths about to be created — is appended verbatim.
     */
    private static Path realProbe(Path abs) {
        var existing = abs;
        final var remaining = new ArrayDeque<Path>();
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            remaining.push(existing.getFileName());
            existing = existing.getParent();
        }
        if (existing == null) {
            return abs;
        }
        var real = existing;
        try {
            real = existing.toRealPath();
        } catch (IOException ignored) {
            // a dangling symlink or an unreadable ancestor: fall back to the normalized form
        }
        for (final var segment : remaining) {
            real = real.resolve(segment);
        }
        return real;
    }

    /**
     * Canonicalizes a configured root the same way the probe is canonicalized, so that a root
     * given through a symlinked location (e.g. {@code /var} vs {@code /private/var}) still
     * matches the real paths of the files inside it.
     */
    private static Path realRoot(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException e) {
            return root.toAbsolutePath().normalize();
        }
    }

    private static String joined(List<Path> roots) {
        return roots.stream().map(Path::toString).collect(Collectors.joining("|"));
    }
}
