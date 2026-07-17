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
     * Importing Turicum source code: {@code import}, {@code sys_import}, and
     * {@code source_directory}. Confined by {@code SandboxPolicy.Builder#importRoot(Path)}.
     * Split from {@link #FILE_READ} so that a host can grant imports from a curated library
     * directory without granting data-file reads, and vice versa.
     */
    IMPORT,

    /**
     * Reading file content and metadata: {@code file_read}, {@code file_lines},
     * {@code file_reader}, {@code file_random_reader}, {@code file_map_reader},
     * {@code file_exists}, {@code is_file}, {@code is_dir}, {@code file_stat}, {@code glob},
     * and the input-stream classes. Confined by the sandbox file roots
     * ({@code fileReadRoot}/{@code fileReadWriteRoot}).
     */
    FILE_READ,

    /**
     * Modifying the content of <em>existing</em> files: {@code file_write},
     * {@code file_writer}, {@code file_random_editor}, and {@code file_map_editor}. When one
     * of these would bring a new file into existence, it additionally demands
     * {@link #FILE_CREATE} at runtime. Granting this capability implies {@link #FILE_READ}.
     * Confined to the read-write file roots.
     */
    FILE_WRITE,

    /**
     * Bringing new file-system entries into existence: {@code mkdir} and the
     * create-a-new-file half of {@code file_write}/{@code file_writer}/{@code file_copy}/
     * {@code file_move}. Granting this capability implies {@link #FILE_READ}.
     * Confined to the read-write file roots.
     */
    FILE_CREATE,

    /**
     * Removing file-system entries: {@code file_delete} and the remove-the-source half of
     * {@code file_move}. Granting this capability implies {@link #FILE_READ}.
     * Confined to the read-write file roots.
     */
    FILE_DELETE,

    /**
     * Temporary file use: {@code tmp_file} and {@code tmp_dir}, which create entries in a
     * per-session scratch directory. The scratch directory acts as an additional read-write
     * file root, and granting this capability implies the whole file family
     * ({@link #FILE_READ}, {@link #FILE_WRITE}, {@link #FILE_CREATE}, {@link #FILE_DELETE}) —
     * there is no use case for temp files that can be created but not read and written. The
     * scratch directory is deleted when the session ends.
     */
    FILE_TEMP,

    /**
     * Reading host environment variables: {@code env}.
     */
    ENV,

    /**
     * Network access: {@code http_client} and {@code http_server}.
     */
    NETWORK
}
