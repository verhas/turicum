package ch.turic.embed;

import ch.turic.Capability;
import ch.turic.exceptions.ExecutionException;
import com.example.host.HostFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the Phase 2 capability handling of the {@code ch.turic.embed} API: the two trust
 * modes, capability-gated built-ins, the Java class-access filter with its mandatory deny floor,
 * and import-root scoping. The snippet markers in this file are referenced from
 * {@code EMBEDDING.md}; after changing a snippet, regenerate the document with
 * {@code mdship update EMBEDDING.md}.
 */
class TestEmbeddingCapabilities {

    // ---- trust modes -------------------------------------------------------------------------

    @Test
    void trustedGrantsEverythingUntrustedGrantsNothing() {
        // snippet trust_modes
        // trusted mode: all capabilities granted; java_class is available
        try (final var engine = TuriEngine.create(SandboxPolicy.trusted().build());
             final var session = engine.newSession()) {
            assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Math\")"));
        }

        // untrusted mode: nothing granted; java_class is not even registered, so the script
        // gets an ordinary "undefined symbol" error - the built-in is simply absent
        try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class, () -> session.eval("java_class(\"java.lang.Math\")"));
        }
        // end snippet
    }

    @Test
    void isDenyByDefaultReportsTheStance() {
        assertFalse(SandboxPolicy.trusted().build().isDenyByDefault());
        assertFalse(SandboxPolicy.UNRESTRICTED.isDenyByDefault());
        assertTrue(SandboxPolicy.untrusted().build().isDenyByDefault());
    }

    // ---- capability gating -------------------------------------------------------------------

    @Test
    void grantingACapabilityRegistersItsBuiltins() {
        // snippet grant_capability
        // env is gated by the ENV capability; without it the built-in is absent
        try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class, () -> session.eval("env(\"PATH\")"));
        }

        // granting ENV registers it
        final var policy = SandboxPolicy.untrusted().allow(Capability.ENV).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertDoesNotThrow(() -> session.eval("env(\"PATH\")"));
        }
        // end snippet
    }

    @Test
    void networkCapabilityGatesTheHttpBuiltins() {
        // snippet network_capability
        // The registration is checked with session.get(name) rather than by calling the
        // built-ins, so the test never opens a socket. A registered built-in resolves to its
        // function value; an ungranted one is undefined.
        try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
             final var session = engine.newSession()) {
            // without NETWORK the http built-ins are absent
            assertThrows(ExecutionException.class, () -> session.get("http_client"));
            assertThrows(ExecutionException.class, () -> session.get("server"));
        }

        final var policy = SandboxPolicy.untrusted().allow(Capability.NETWORK).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // granting NETWORK registers them
            assertNotNull(session.get("http_client"));
            assertNotNull(session.get("server"));
        }
        // end snippet
    }

    @Test
    void trustedModeCanDenyACapability() {
        // trusted grants everything, then deny() trims a family away
        final var policy = SandboxPolicy.trusted().deny(Capability.JAVA_REFLECTION).build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class, () -> session.eval("java_class(\"java.lang.Math\")"));
            // a non-reflection built-in is unaffected
            assertEquals(3L, session.eval("1 + 2"));
        }
    }

    // ---- class-access filter and deny floor --------------------------------------------------

    @Test
    void untrustedAllowlistsJavaClasses() {
        // snippet class_allowlist
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.JAVA_REFLECTION)
                .allowJavaClasses("java.lang.Math")   // only this class
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // the allowlisted class loads
            assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Math\")"));

            // any other class is denied, with an attributable message
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("java_class(\"java.util.ArrayList\")"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
        // end snippet
    }

    @Test
    void theDenyFloorBlocksDangerousClassesEvenWhenAllowlisted() {
        // snippet deny_floor
        // even explicitly allowlisting Runtime cannot open it in untrusted mode:
        // the deny floor is absolute
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.JAVA_REFLECTION)
                .allowJavaClasses("java.lang.Runtime")
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("java_class(\"java.lang.Runtime\")"));
            assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
        // end snippet
    }

    @Test
    void theFloorBlocksTheInterpretersOwnInternals() {
        // a script must never reach ch.turic.* - it could otherwise lift its own limits
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.JAVA_REFLECTION)
                .allowJavaClasses("ch.turic.*")
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class,
                    () -> session.eval("java_class(\"ch.turic.memory.GlobalContext\")"));
        }
    }

    @Test
    void injectedObjectsAreReachableOnlyWhenAllowlisted() {
        // snippet injected_object
        final var facade = new HostFacade();

        // untrusted with the facade's class NOT allowlisted: even a method on the injected
        // object is denied - the allowlist governs which classes a script may touch reflectively
        try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
             final var session = engine.newSession()) {
            session.set("host", facade);
            assertThrows(ExecutionException.class, () -> session.eval("host.greet()"));
        }

        // allowlisting the facade class makes exactly its API reachable
        final var policy = SandboxPolicy.untrusted()
                .allowJavaClasses("com.example.host.HostFacade")
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            session.set("host", facade);
            assertEquals("hello from the host", session.eval("host.greet()"));
        }
        // end snippet
    }

    @Test
    void anInjectedObjectCannotBePivotedIntoAReflectiveEscape() {
        // snippet reflective_escape_blocked
        // The classic escape from a held Java object is
        //   host.getClass().getClassLoader().loadClass("java.lang.Runtime")
        // Method dispatch on a Java object is filtered too (not only class lookups by name), and
        // java.lang.Class / java.lang.ClassLoader are on the deny floor, so the pivot is blocked
        // even though the facade itself is allowlisted.
        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.JAVA_REFLECTION)
                .allowJavaClasses("com.example.host.HostFacade")
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            session.set("host", new HostFacade());

            // the facade's own method works
            assertEquals("hello from the host", session.eval("host.greet()"));

            // getClass() returns an inert Class, but any reflective method on it is denied
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("host.getClass().getClassLoader()"));
            assertTrue(e.getMessage().contains("java.lang.Class")
                    && e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
        }
        // end snippet
    }

    @Test
    void trustedModeFloorIsPierceableWithUnsafeAllow() {
        // snippet unsafe_allow
        // trusted mode also installs the floor, so a plain trusted policy denies Runtime ...
        try (final var engine = TuriEngine.create(SandboxPolicy.trusted().build());
             final var session = engine.newSession()) {
            assertThrows(ExecutionException.class, () -> session.eval("java_class(\"java.lang.Runtime\")"));
        }

        // ... but a trusted embedder can deliberately pierce it; the 'unsafe' prefix is a
        // signal to reviewers that this class can defeat the sandbox
        final var policy = SandboxPolicy.trusted()
                .unsafeAllowJavaClasses("java.lang.Runtime")
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Runtime\")"));
        }
        // end snippet
    }

    @Test
    void unrestrictedInstallsNoFloor() {
        // UNRESTRICTED is not a sandbox: even Runtime resolves, exactly like the plain
        // interpreter used by the CLI
        try (final var engine = TuriEngine.create(SandboxPolicy.UNRESTRICTED);
             final var session = engine.newSession()) {
            assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Runtime\")"));
        }
    }

    // ---- import-root scoping -----------------------------------------------------------------

    @Test
    void untrustedFileReadRequiresAnImportRoot() {
        // snippet import_root_required
        // granting FILE_READ in untrusted mode without scoping it to a root is a
        // configuration error, caught when the policy is built
        assertThrows(IllegalStateException.class,
                () -> SandboxPolicy.untrusted().allow(Capability.FILE_READ).build());
        // end snippet
    }

    @Test
    void importRootConfinesTheResultButLeavesTheSearchPathUnchanged(@TempDir Path workDir) throws Exception {
        // snippet import_root
        final var importRoot = Files.createDirectory(workDir.resolve("scripts"));
        // a library under the root ...
        Files.writeString(importRoot.resolve("lib.turi"), "let answer = 42\nexport_all()\n");
        // ... and a file OUTSIDE the root, one directory above it
        Files.writeString(workDir.resolve("secret.turi"), "let stolen = 1\nexport_all()\n");

        final var policy = SandboxPolicy.untrusted()
                .allow(Capability.FILE_READ)
                .importRoot(importRoot)
                .build();
        try (final var engine = TuriEngine.create(policy);
             final var session = engine.newSession()) {
            // The script drives resolution with APPIA exactly as usual; importRoot does not
            // replace the search path, it only caps the result. An import that resolves under
            // the root works:
            session.set("root", importRoot.toString());
            assertEquals(42L, session.eval("global APPIA = [root]\nimport \"lib\"\nanswer"));

            // Pointing APPIA at a directory outside the root cannot widen the reach: the file
            // is found there, but denied because it resolves above the ceiling.
            session.set("above", workDir.toString());
            final var e = assertThrows(ExecutionException.class,
                    () -> session.eval("global APPIA = [above]\nimport \"secret\"\nstolen"));
            assertTrue(e.getMessage().contains("outside the sandbox import root"), e.getMessage());
        }
        // end snippet
    }
}
