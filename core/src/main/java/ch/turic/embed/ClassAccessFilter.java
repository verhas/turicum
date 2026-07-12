package ch.turic.embed;

import java.util.List;
import java.util.function.Predicate;

/**
 * Decides which Java classes a running script may load through the reflective built-ins.
 * Installed on the {@code TuricumClassLoader} and consulted by {@code loadClassForScript};
 * it governs script-initiated loads only, never the interpreter's own class loading.
 * <p>
 * The filter enforces a mandatory deny floor in both trust modes (see the embedding
 * documentation, §7.4). The floor blocks the classes a script could use to escape the
 * sandbox — the interpreter's own internals, JDK internals, the reflection and method-handle
 * escape hatches, and the process/exit/raw-thread classes. In untrusted mode the floor is
 * absolute, and the allowlist is the only opener; in trusted mode the floor is pierceable
 * per pattern with {@code unsafeAllowJavaClasses} and the deny-list further closes access.
 */
final class ClassAccessFilter implements Predicate<String> {

    // Absolute floor, enforced in both modes.
    private static final List<String> FLOOR_PREFIXES = List.of(
            "ch.turic.", "jdk.internal.", "sun.", "java.lang.reflect.", "java.lang.invoke.");
    private static final List<String> FLOOR_CLASSES = List.of(
            java.lang.Runtime.class.getName(),
            java.lang.ProcessBuilder.class.getName(),
            java.lang.System.class.getName(),
            java.lang.Thread.class.getName(),
            // java.lang.Class and java.lang.ClassLoader are reflection vectors: without them on
            // the floor, a script that holds any Java object could reach an arbitrary class via
            // obj.getClass().getClassLoader().loadClass(...), bypassing the name-based filter.
            java.lang.Class.class.getName(),
            java.lang.ClassLoader.class.getName());
    // Extra floor in untrusted mode only: raw threads and executors would bypass the
    // maxThreads permit accounting. Trusted mode keeps java.util.concurrent available.
    private static final List<String> UNTRUSTED_FLOOR_PREFIXES = List.of("java.util.concurrent.");

    private final boolean denyByDefault;
    private final List<String> allow;
    private final List<String> deny;
    private final List<String> unsafeAllow;

    ClassAccessFilter(boolean denyByDefault, List<String> allow, List<String> deny, List<String> unsafeAllow) {
        this.denyByDefault = denyByDefault;
        this.allow = List.copyOf(allow);
        this.deny = List.copyOf(deny);
        this.unsafeAllow = List.copyOf(unsafeAllow);
    }

    @Override
    public boolean test(String className) {
        if (denyByDefault) {
            // untrusted: the floor is absolute, and only the allow-list opens access
            if (inFloor(className, true)) {
                return false;
            }
            return matchesAny(allow, className);
        }
        // trusted: the floor is on but pierceable, and the deny-list closes access
        if (inFloor(className, false) && !matchesAny(unsafeAllow, className)) {
            return false;
        }
        return !matchesAny(deny, className);
    }

    private static boolean inFloor(String name, boolean untrusted) {
        for (final var c : FLOOR_CLASSES) {
            if (name.equals(c)) {
                return true;
            }
        }
        for (final var p : FLOOR_PREFIXES) {
            if (name.startsWith(p)) {
                return true;
            }
        }
        if (untrusted) {
            for (final var p : UNTRUSTED_FLOOR_PREFIXES) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A pattern is either a fully qualified class name (exact match) or a package prefix
     * ending in {@code .*}, which matches that package and all its sub-packages.
     */
    private static boolean matchesAny(List<String> patterns, String name) {
        for (final var pattern : patterns) {
            if (pattern.endsWith(".*")) {
                if (name.startsWith(pattern.substring(0, pattern.length() - 1))) {
                    return true;
                }
            } else if (name.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}
