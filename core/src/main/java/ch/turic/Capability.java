package ch.turic;

/**
 * A coarse permission that gates whole families of built-in functions, macros, and classes
 * when a script runs under a sandbox policy.
 * <p>
 * A built-in declares the capabilities it needs with {@link RequiresCapability}; a built-in
 * with no declared capability is always available. The sandbox grants a set of capabilities
 * (see {@code ch.turic.embed.SandboxPolicy}); a built-in is registered for a session only
 * when every capability it requires is granted. A script that calls a built-in that was not
 * registered gets an ordinary "undefined symbol" error — hiding the built-in is exactly the
 * point.
 * <p>
 * Capabilities decide <em>whether</em> a family of built-ins exists. Which concrete Java
 * classes the reflection built-ins may touch is a finer decision made by the class-access
 * filter, not by this enum.
 */
public enum Capability {
    /**
     * Reflective access to Java classes: {@code java_class}, {@code java_call},
     * {@code java_object}, {@code java_import}, {@code java_callback},
     * {@code add_java_classes}, {@code java_resources}, {@code java_type}, and
     * {@code as_object}.
     */
    JAVA_REFLECTION,

    /**
     * Reading from the file system: {@code import}, {@code sys_import}, {@code glob},
     * {@code source_directory}, and the input-stream classes.
     */
    FILE_READ,

    /**
     * Reading host environment variables: {@code env}.
     */
    ENV,

    /**
     * Network access: {@code http_client} and {@code http_server}.
     */
    NETWORK
}
