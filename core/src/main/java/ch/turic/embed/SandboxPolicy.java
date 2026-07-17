package ch.turic.embed;

import ch.turic.Capability;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable description of the resource limits, capability grants, and redirections applied to
 * every {@link TuriSession} of a {@link TuriEngine}.
 * <p>
 * A policy is created through one of two entry points that declare, in the first line, how much
 * the embedder trusts the script:
 * <ul>
 *   <li>{@link #trusted()} — for in-house scripts, build steps, configuration. All capabilities
 *       are granted by default and the {@code deny…} methods trim them; the mandatory class
 *       deny floor is on but pierceable. Guardrails, not a security boundary.</li>
 *   <li>{@link #untrusted()} — for user-supplied or multi-tenant scripts. Nothing is granted by
 *       default and the {@code allow…} methods add capabilities; the class deny floor is
 *       absolute. A security posture (within the in-JVM limits described in the embedding
 *       documentation).</li>
 * </ul>
 * The two entry points return different builder types, so {@code allow…} and {@code deny…}
 * cannot be mixed: calling the wrong family is a compile-time error.
 * <p>
 * {@link #UNRESTRICTED} is a third, distinct option: no capability gating and no class filter at
 * all, i.e. exactly the behavior of the plain {@link ch.turic.Interpreter}. It is not a sandbox;
 * unlike {@code trusted().build()} it does not install the deny floor.
 *
 * <!-- the example is Java, not Turicum; the pre tag and the opening code inline tag are kept
 *      on separate lines so that TestJavaDocSnippets does not run it as a Turicum program -->
 * <pre>
 * {@code
 * final var policy = SandboxPolicy.untrusted()
 *         .stepLimit(1_000_000)
 *         .timeout(Duration.ofSeconds(5))
 *         .allow(Capability.JAVA_REFLECTION)
 *         .allowJavaClasses("com.mycorp.scripting.api.*")
 *         .build();
 * }</pre>
 */
public final class SandboxPolicy {
    private final int stepLimit;
    private final int graceSteps;
    private final Duration timeout;
    private final int maxThreads;
    private final PrintStream out;
    private final PrintStream err;
    private final boolean denyByDefault;
    private final Set<Capability> grantedCapabilities; // null == UNRESTRICTED (no gating)
    private final ClassAccessFilter classFilter;       // null == UNRESTRICTED (no filtering)
    private final String modeLabel;
    private final Path importRoot;
    private final List<Path> fileReadRoots;
    private final List<Path> fileReadWriteRoots;
    private final long maxMappedBytes;

    private SandboxPolicy(Builder<?> builder,
                          boolean denyByDefault,
                          Set<Capability> grantedCapabilities,
                          ClassAccessFilter classFilter,
                          String modeLabel,
                          long maxMappedBytes) {
        this.stepLimit = builder.stepLimit;
        this.graceSteps = builder.graceSteps;
        this.timeout = builder.timeout;
        this.maxThreads = builder.maxThreads;
        this.out = builder.out;
        this.err = builder.err;
        this.importRoot = builder.importRoot;
        this.fileReadRoots = List.copyOf(builder.fileReadRoots);
        this.fileReadWriteRoots = List.copyOf(builder.fileReadWriteRoots);
        this.maxMappedBytes = maxMappedBytes;
        this.denyByDefault = denyByDefault;
        this.grantedCapabilities = grantedCapabilities;
        this.classFilter = classFilter;
        this.modeLabel = modeLabel;
    }

    // canonical constructor for the UNRESTRICTED sentinel: all defaults, no gating, no filter
    private SandboxPolicy() {
        this.stepLimit = -1;
        this.graceSteps = 0;
        this.timeout = null;
        this.maxThreads = -1;
        this.out = null;
        this.err = null;
        this.importRoot = null;
        this.fileReadRoots = List.of();
        this.fileReadWriteRoots = List.of();
        this.maxMappedBytes = -1;
        this.denyByDefault = false;
        this.grantedCapabilities = null;
        this.classFilter = null;
        this.modeLabel = "unrestricted";
    }

    /**
     * A policy without any capability restriction or class filter; scripts run exactly as with
     * the plain {@link ch.turic.Interpreter}. This is <em>not</em> a sandbox and, unlike
     * {@link #trusted()}, does not install the deny floor. It is the default of
     * {@link TuriEngine#create()}.
     */
    public static final SandboxPolicy UNRESTRICTED = new SandboxPolicy();

    /**
     * Starts a policy for trusted scripts: all capabilities granted, {@code deny…} methods trim
     * them, the class deny floor on but pierceable with {@code unsafeAllowJavaClasses}.
     *
     * @return a new trusted-mode builder
     */
    public static TrustedBuilder trusted() {
        return new TrustedBuilder();
    }

    /**
     * Starts a policy for untrusted scripts: nothing granted, {@code allow…} methods add
     * capabilities, the class deny floor absolute.
     *
     * @return a new untrusted-mode builder
     */
    public static UntrustedBuilder untrusted() {
        return new UntrustedBuilder();
    }

    /**
     * @return the maximum number of interpreter steps for one session, or {@code -1} for no limit
     */
    public int stepLimit() {
        return stepLimit;
    }

    /**
     * @return the extra steps granted to {@code finally}/exit blocks after a halt, or {@code 0}
     * when cleanup grace is disabled
     */
    public int graceSteps() {
        return graceSteps;
    }

    /**
     * @return the wall-clock limit for a single {@link TuriSession#eval} call, or {@code null}
     * for no limit
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * @return the maximum number of concurrently running <em>additional</em> interpreter
     * threads (async blocks and flow cells) across all sessions of one engine; {@code 0}
     * means single-threaded scripts (no async/flow at all), a negative value means no limit.
     * The main interpreter thread of a session never counts against this limit.
     */
    public int maxThreads() {
        return maxThreads;
    }

    /**
     * @return the stream receiving {@code print}/{@code println} output, or {@code null} for
     * {@link System#out}
     */
    public PrintStream out() {
        return out;
    }

    /**
     * @return the stream receiving error output, or {@code null} for {@link System#err}
     */
    public PrintStream err() {
        return err;
    }

    /**
     * @return {@code true} for an untrusted policy (nothing granted unless allowed),
     * {@code false} for a trusted or unrestricted policy (granted unless denied)
     */
    public boolean isDenyByDefault() {
        return denyByDefault;
    }

    /**
     * @return the capabilities granted to scripts, or {@code null} for {@link #UNRESTRICTED},
     * which does not gate built-ins at all
     */
    public Set<Capability> grantedCapabilities() {
        return grantedCapabilities;
    }

    /**
     * @return the root directory under which the import built-ins must resolve sources, or
     * {@code null} when imports are not restricted to a root
     */
    public Path importRoot() {
        return importRoot;
    }

    /**
     * @return the read-only file root directories under which {@code FILE_READ} built-ins may
     * operate; possibly empty
     */
    public List<Path> fileReadRoots() {
        return fileReadRoots;
    }

    /**
     * @return the read-write file root directories under which every file built-in — read,
     * write, create, delete — may operate; possibly empty
     */
    public List<Path> fileReadWriteRoots() {
        return fileReadWriteRoots;
    }

    /**
     * @return the maximum running total of memory-mapped bytes over all live mappings of one
     * session; {@code 0} makes the mmap built-ins effectively unusable, a negative value means
     * no limit (the default of trusted and unrestricted policies; untrusted defaults to 0)
     */
    public long maxMappedBytes() {
        return maxMappedBytes;
    }

    /**
     * @return the class-access filter to install on the script class loader, or {@code null}
     * for {@link #UNRESTRICTED}, which installs none
     */
    ClassAccessFilter classFilter() {
        return classFilter;
    }

    /**
     * @return a short label naming the trust mode, used in denial messages
     */
    String modeLabel() {
        return modeLabel;
    }

    /**
     * Shared, mode-independent part of the two builders: the resource limits, I/O redirection,
     * and import root. The trust-specific capability and class-list methods live on
     * {@link TrustedBuilder} and {@link UntrustedBuilder}.
     *
     * @param <B> the concrete builder type, for fluent chaining
     */
    public abstract static sealed class Builder<B extends Builder<B>>
            permits TrustedBuilder, UntrustedBuilder {
        int stepLimit = -1;
        int graceSteps = 0;
        Duration timeout = null;
        int maxThreads = -1;
        PrintStream out = null;
        PrintStream err = null;
        Path importRoot = null;
        final List<Path> fileReadRoots = new ArrayList<>();
        final List<Path> fileReadWriteRoots = new ArrayList<>();
        Long maxMappedBytes = null; // null: mode default (untrusted 0, trusted/unrestricted unlimited)

        Builder() {
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }

        /**
         * Limits the total number of interpreter steps (commands executed) in one session.
         * When the limit is reached, the evaluation throws and Turicum code cannot catch or
         * suppress it.
         *
         * @param stepLimit the maximum permitted steps, or a negative value for no limit
         * @return this builder
         */
        public B stepLimit(int stepLimit) {
            this.stepLimit = stepLimit;
            return self();
        }

        /**
         * Grants {@code finally}/exit blocks a bounded number of extra steps to release
         * resources after a halt (step limit, timeout, or abort) has fired. The cleanup can
         * never suppress the halt and never runs longer than this allowance.
         *
         * @param graceSteps the extra steps for cleanup code, or {@code 0} to disable
         * @return this builder
         */
        public B graceSteps(int graceSteps) {
            if (graceSteps < 0) {
                throw new IllegalArgumentException("graceSteps must not be negative");
            }
            this.graceSteps = graceSteps;
            return self();
        }

        /**
         * Limits the wall-clock time of a single {@link TuriSession#eval} call. When the
         * timeout fires, every thread of the session is aborted — including threads blocked in
         * {@code sleep()}, channel operations, or I/O — and the evaluation throws a
         * {@link TuriTimeoutException}. A timed-out session cannot be used again.
         *
         * @param timeout the maximum evaluation time; {@code null} for no limit
         * @return this builder
         */
        public B timeout(Duration timeout) {
            if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return self();
        }

        /**
         * Caps the number of concurrently running <em>additional</em> interpreter threads
         * (async blocks and flow cells) across all sessions of the engine. The main
         * interpreter thread of a session never counts against this limit; {@code 0} therefore
         * makes scripts strictly single-threaded, and {@code maxThreads(1)} allows one task
         * next to the main thread.
         * <p>
         * The cap is a hard limit, not a throttle: when it is reached, starting a further
         * thread fails immediately with an execution error instead of waiting for a permit.
         * Waiting would allow a script to deadlock itself — a task holding a permit that
         * {@code await}s a child which is blocked waiting for that same permit — and a thread
         * blocked on a permit executes no steps, so the step limit could never fire; see the
         * embedding documentation for the full reasoning.
         *
         * @param maxThreads the maximum concurrent additional interpreter threads, {@code 0}
         *                   for none, or a negative value for no limit
         * @return this builder
         */
        public B maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return self();
        }

        /**
         * Restricts scripts to the main interpreter thread: {@code async} and {@code flow}
         * cannot start any task. An alias for {@code maxThreads(0)}.
         *
         * @return this builder
         */
        public B singleThread() {
            return maxThreads(0);
        }

        /**
         * Redirects the output of {@code print} and {@code println}.
         *
         * @param out the stream to write to
         * @return this builder
         */
        public B stdout(PrintStream out) {
            this.out = Objects.requireNonNull(out);
            return self();
        }

        /**
         * Redirects the output of {@code print} and {@code println} wrapping the stream into an
         * auto-flushing UTF-8 {@link PrintStream}.
         *
         * @param out the stream to write to
         * @return this builder
         */
        public B stdout(OutputStream out) {
            return stdout(new PrintStream(Objects.requireNonNull(out), true, StandardCharsets.UTF_8));
        }

        /**
         * Redirects the interpreter's error output.
         *
         * @param err the stream to write to
         * @return this builder
         */
        public B stderr(PrintStream err) {
            this.err = Objects.requireNonNull(err);
            return self();
        }

        /**
         * Redirects the interpreter's error output wrapping the stream into an auto-flushing
         * UTF-8 {@link PrintStream}.
         *
         * @param err the stream to write to
         * @return this builder
         */
        public B stderr(OutputStream err) {
            return stderr(new PrintStream(Objects.requireNonNull(err), true, StandardCharsets.UTF_8));
        }

        /**
         * Restricts {@code import}/{@code sys_import} resolution to this root directory; paths
         * escaping it (via {@code ..}) are rejected. Required in untrusted mode when
         * {@link Capability#IMPORT} is granted; optional and merely convenient in trusted
         * mode. Data-file access is confined separately by {@link #fileReadRoot(Path...)} and
         * {@link #fileReadWriteRoot(Path...)}.
         *
         * @param importRoot the root directory scripts may import from
         * @return this builder
         */
        public B importRoot(Path importRoot) {
            this.importRoot = Objects.requireNonNull(importRoot).toAbsolutePath().normalize();
            return self();
        }

        /**
         * Adds read-only file root directories: trees where the {@link Capability#FILE_READ}
         * built-ins ({@code file_read}, {@code glob}, {@code file_stat}, …) may operate. The
         * method is repeatable; the built-ins confine against the whole accumulated set.
         * Relative script paths resolve against the roots in declaration order (read-only
         * roots first, then read-write roots), taking the first candidate that exists.
         * <p>
         * Required (or a {@link #fileReadWriteRoot(Path...)}) in untrusted mode when
         * {@code FILE_READ} is granted — unless {@link Capability#FILE_TEMP} is granted, whose
         * scratch directory is an implicit read-write root. Optional guardrail in trusted mode.
         *
         * @param roots the root directories scripts may read under
         * @return this builder
         */
        public B fileReadRoot(Path... roots) {
            for (final var root : roots) {
                fileReadRoots.add(Objects.requireNonNull(root).toAbsolutePath().normalize());
            }
            return self();
        }

        /**
         * Adds read-write file root directories: trees where every file built-in may operate —
         * read, write, create, and delete alike. The method is repeatable. Relative script
         * paths of mutating operations resolve against the <em>first</em> declared read-write
         * root; a write target must be deterministic, never search-dependent.
         * <p>
         * At least one read-write root is required in untrusted mode when any of
         * {@link Capability#FILE_WRITE}/{@link Capability#FILE_CREATE}/
         * {@link Capability#FILE_DELETE} is granted — unless {@link Capability#FILE_TEMP} is
         * granted, whose scratch directory is an implicit read-write root. Optional guardrail
         * in trusted mode.
         *
         * @param roots the root directories scripts may read and modify under
         * @return this builder
         */
        public B fileReadWriteRoot(Path... roots) {
            for (final var root : roots) {
                fileReadWriteRoots.add(Objects.requireNonNull(root).toAbsolutePath().normalize());
            }
            return self();
        }

        /**
         * Caps the running total of memory-mapped bytes over all live mappings
         * ({@code file_map_reader}/{@code file_map_editor}) of one session. On Java 21 a
         * mapping's pages are released only at garbage collection, so a mapping is host
         * virtual address space held on the sandbox's behalf; the cap bounds it.
         *
         * @param maxMappedBytes the maximum total mapped bytes; {@code 0} makes the mmap
         *                       built-ins effectively unusable, a negative value means no
         *                       limit. Untrusted policies default to {@code 0}, trusted and
         *                       unrestricted policies to no limit.
         * @return this builder
         */
        public B maxMappedBytes(long maxMappedBytes) {
            this.maxMappedBytes = maxMappedBytes;
            return self();
        }

        /**
         * Builds the immutable policy.
         *
         * @return the policy
         */
        public abstract SandboxPolicy build();
    }

    /**
     * Builder for {@link #trusted()} policies: all capabilities granted, the {@code deny…}
     * methods trim capabilities and Java classes, and {@code unsafeAllowJavaClasses} pierces
     * the otherwise-mandatory deny floor.
     */
    public static final class TrustedBuilder extends Builder<TrustedBuilder> {
        private final EnumSet<Capability> denied = EnumSet.noneOf(Capability.class);
        private final List<String> denyClasses = new ArrayList<>();
        private final List<String> unsafeAllowClasses = new ArrayList<>();

        private TrustedBuilder() {
        }

        /**
         * Denies the given capabilities that would otherwise be granted, hiding their
         * built-ins from the script.
         *
         * @param capabilities the capabilities to remove
         * @return this builder
         */
        public TrustedBuilder deny(Capability... capabilities) {
            denied.addAll(List.of(capabilities));
            return this;
        }

        /**
         * Denies script access to the given Java classes even though reflection is granted.
         * A pattern is a fully qualified class name or a package prefix ending in {@code .*}.
         *
         * @param patterns class names or package prefixes to deny
         * @return this builder
         */
        public TrustedBuilder denyJavaClasses(String... patterns) {
            denyClasses.addAll(List.of(patterns));
            return this;
        }

        /**
         * Pierces the mandatory deny floor for the given classes — for example, to let a trusted
         * script use {@code java.lang.System}. The {@code unsafe} prefix is deliberate: these
         * classes can defeat the sandbox, so the call should stand out in a code review.
         *
         * @param patterns class names or package prefixes to allow through the floor
         * @return this builder
         */
        public TrustedBuilder unsafeAllowJavaClasses(String... patterns) {
            unsafeAllowClasses.addAll(List.of(patterns));
            return this;
        }

        @Override
        public SandboxPolicy build() {
            final var granted = EnumSet.allOf(Capability.class);
            granted.removeAll(denied);
            // write implies read: a policy that denies FILE_READ but leaves a write-family
            // capability granted would be a half-tested combination; deny the whole family
            if (!granted.contains(Capability.FILE_READ)
                    && (granted.contains(Capability.FILE_WRITE)
                    || granted.contains(Capability.FILE_CREATE)
                    || granted.contains(Capability.FILE_DELETE)
                    || granted.contains(Capability.FILE_TEMP))) {
                throw new IllegalStateException(
                        "trusted policy denies FILE_READ but leaves a write-family capability " +
                                "(FILE_WRITE/FILE_CREATE/FILE_DELETE/FILE_TEMP) granted; " +
                                "write implies read, deny the whole file family instead");
            }
            final var filter = new ClassAccessFilter(false, List.of(), denyClasses, unsafeAllowClasses);
            return new SandboxPolicy(this, false, Set.copyOf(granted), filter, "trusted mode",
                    maxMappedBytes == null ? -1 : maxMappedBytes);
        }
    }

    /**
     * Builder for {@link #untrusted()} policies: nothing granted, the {@code allow…} methods
     * add capabilities and Java classes, and the deny floor is absolute (no piercing).
     */
    public static final class UntrustedBuilder extends Builder<UntrustedBuilder> {
        private final EnumSet<Capability> allowed = EnumSet.noneOf(Capability.class);
        private final List<String> allowClasses = new ArrayList<>();

        private UntrustedBuilder() {
        }

        /**
         * Grants the given capabilities, registering their built-ins for the script.
         *
         * @param capabilities the capabilities to grant
         * @return this builder
         */
        public UntrustedBuilder allow(Capability... capabilities) {
            allowed.addAll(List.of(capabilities));
            return this;
        }

        /**
         * Allows script access to the given Java classes (reflection must also be granted with
         * {@link #allow(Capability...)}). A pattern is a fully qualified class name or a package
         * prefix ending in {@code .*}. Prefer allowlisting a purpose-built facade class over a
         * broad utility package: an allowed class exposes all of its public members.
         *
         * @param patterns class names or package prefixes to allow
         * @return this builder
         */
        public UntrustedBuilder allowJavaClasses(String... patterns) {
            allowClasses.addAll(List.of(patterns));
            return this;
        }

        @Override
        public SandboxPolicy build() {
            // capability implications, normalized here so the granted set is explicit:
            // FILE_TEMP implies the whole file family (temp files are read-write by nature),
            // and any write-family capability implies FILE_READ (write implies read)
            if (allowed.contains(Capability.FILE_TEMP)) {
                allowed.add(Capability.FILE_WRITE);
                allowed.add(Capability.FILE_CREATE);
                allowed.add(Capability.FILE_DELETE);
            }
            if (allowed.contains(Capability.FILE_WRITE)
                    || allowed.contains(Capability.FILE_CREATE)
                    || allowed.contains(Capability.FILE_DELETE)) {
                allowed.add(Capability.FILE_READ);
            }
            if (allowed.contains(Capability.IMPORT) && importRoot == null) {
                throw new IllegalStateException(
                        "untrusted policy grants IMPORT but no importRoot() is set; imports must be scoped to a root");
            }
            final var hasTempRoot = allowed.contains(Capability.FILE_TEMP);
            if (allowed.contains(Capability.FILE_READ)
                    && fileReadRoots.isEmpty() && fileReadWriteRoots.isEmpty() && !hasTempRoot) {
                throw new IllegalStateException(
                        "untrusted policy grants FILE_READ but no fileReadRoot() or fileReadWriteRoot() is set; " +
                                "file reads must be scoped to a root");
            }
            if ((allowed.contains(Capability.FILE_WRITE)
                    || allowed.contains(Capability.FILE_CREATE)
                    || allowed.contains(Capability.FILE_DELETE))
                    && fileReadWriteRoots.isEmpty() && !hasTempRoot) {
                throw new IllegalStateException(
                        "untrusted policy grants FILE_WRITE/FILE_CREATE/FILE_DELETE but no fileReadWriteRoot() is set; " +
                                "file mutations must be scoped to a root");
            }
            final var filter = new ClassAccessFilter(true, allowClasses, List.of(), List.of());
            return new SandboxPolicy(this, true, Set.copyOf(allowed), filter, "untrusted mode",
                    maxMappedBytes == null ? 0 : maxMappedBytes);
        }
    }
}
