package ch.turic.embed;

import ch.turic.exceptions.ExecutionException;

import java.time.Duration;

/**
 * Thrown by {@link TuriSession#eval} when the {@link SandboxPolicy#timeout() wall-clock
 * timeout} fires. The session is aborted and cannot be used afterwards.
 */
public class TuriTimeoutException extends ExecutionException {
    private final Duration timeout;

    TuriTimeoutException(Duration timeout, Throwable cause) {
        super(cause, "Evaluation did not finish within %s", timeout);
        this.timeout = timeout;
    }

    /**
     * @return the configured timeout that fired
     */
    public Duration timeout() {
        return timeout;
    }
}
