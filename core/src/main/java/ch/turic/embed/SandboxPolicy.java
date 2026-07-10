package ch.turic.embed;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable description of the resource limits and redirections applied to every
 * {@link TuriSession} of a {@link TuriEngine}.
 * <p>
 * All limits are optional; {@link #UNRESTRICTED} is a policy without any limit, which
 * behaves like the plain {@link ch.turic.Interpreter}. A policy is created with the
 * {@link #builder()}:
 *
 * <!-- the example is Java, not Turicum; the pre tag and the opening code inline tag are kept
 *      on separate lines so that TestJavaDocSnippets does not run it as a Turicum program -->
 * <pre>
 * {@code
 * final var policy = SandboxPolicy.builder()
 *         .stepLimit(1_000_000)
 *         .graceSteps(1_000)
 *         .timeout(Duration.ofSeconds(5))
 *         .maxThreads(8)
 *         .stdout(myStream)
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

    private SandboxPolicy(Builder builder) {
        this.stepLimit = builder.stepLimit;
        this.graceSteps = builder.graceSteps;
        this.timeout = builder.timeout;
        this.maxThreads = builder.maxThreads;
        this.out = builder.out;
        this.err = builder.err;
    }

    /**
     * A policy without any limit or redirection; scripts run exactly as with the plain
     * {@link ch.turic.Interpreter}.
     */
    public static final SandboxPolicy UNRESTRICTED = builder().build();

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {
        private int stepLimit = -1;
        private int graceSteps = 0;
        private Duration timeout = null;
        private int maxThreads = -1;
        private PrintStream out = null;
        private PrintStream err = null;

        private Builder() {
        }

        /**
         * Limits the total number of interpreter steps (commands executed) in one session.
         * When the limit is reached, the evaluation throws and Turicum code cannot catch or
         * suppress it.
         *
         * @param stepLimit the maximum permitted steps, or a negative value for no limit
         * @return this builder
         */
        public Builder stepLimit(int stepLimit) {
            this.stepLimit = stepLimit;
            return this;
        }

        /**
         * Grants {@code finally}/exit blocks a bounded number of extra steps to release
         * resources after a halt (step limit, timeout, or abort) has fired. The cleanup can
         * never suppress the halt and never runs longer than this allowance.
         *
         * @param graceSteps the extra steps for cleanup code, or {@code 0} to disable
         * @return this builder
         */
        public Builder graceSteps(int graceSteps) {
            if (graceSteps < 0) {
                throw new IllegalArgumentException("graceSteps must not be negative");
            }
            this.graceSteps = graceSteps;
            return this;
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
        public Builder timeout(Duration timeout) {
            if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return this;
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
        public Builder maxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        /**
         * Restricts scripts to the main interpreter thread: {@code async} and {@code flow}
         * cannot start any task. An alias for {@code maxThreads(0)}.
         *
         * @return this builder
         */
        public Builder singleThread() {
            return maxThreads(0);
        }

        /**
         * Redirects the output of {@code print} and {@code println}.
         *
         * @param out the stream to write to
         * @return this builder
         */
        public Builder stdout(PrintStream out) {
            this.out = Objects.requireNonNull(out);
            return this;
        }

        /**
         * Redirects the output of {@code print} and {@code println} wrapping the stream into an
         * auto-flushing UTF-8 {@link PrintStream}.
         *
         * @param out the stream to write to
         * @return this builder
         */
        public Builder stdout(OutputStream out) {
            return stdout(new PrintStream(Objects.requireNonNull(out), true, StandardCharsets.UTF_8));
        }

        /**
         * Redirects the interpreter's error output.
         *
         * @param err the stream to write to
         * @return this builder
         */
        public Builder stderr(PrintStream err) {
            this.err = Objects.requireNonNull(err);
            return this;
        }

        /**
         * Redirects the interpreter's error output wrapping the stream into an auto-flushing
         * UTF-8 {@link PrintStream}.
         *
         * @param err the stream to write to
         * @return this builder
         */
        public Builder stderr(OutputStream err) {
            return stderr(new PrintStream(Objects.requireNonNull(err), true, StandardCharsets.UTF_8));
        }

        public SandboxPolicy build() {
            return new SandboxPolicy(this);
        }
    }
}
