package com.example.host;

/**
 * A stand-in for a host object an embedder injects into a script with {@code session.set(...)}.
 * It lives outside the {@code ch.turic.*} namespace, exactly like a real embedder's facade, so
 * the deny floor does not block it and an allowlist entry can make it reachable.
 */
public class HostFacade {
    public String greet() {
        return "hello from the host";
    }
}
