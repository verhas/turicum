package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.LocalContext;

import java.nio.charset.Charset;
import java.util.Set;

/**
 * Shared helper of the file I/O built-ins: runtime capability demands for the operations
 * whose capability need depends on arguments or file-system state (create-on-write,
 * copy/move), charset resolution, and the D11 rule that binary writes accept {@code bin}
 * content only.
 * <p>
 * Every built-in (and every handle class) owns its own instance, created with the name it
 * reports in error messages. The name is a field, not a parameter of each call, so the
 * debug/log information stays separated from the functionality: the methods take only the
 * arguments the operation itself needs.
 */
final class FileIo {

    private final String functionName;

    /**
     * @param functionName the name the owning built-in or handle reports in error messages
     */
    FileIo(String functionName) {
        this.functionName = functionName;
    }

    /**
     * @param context the call context of the built-in
     * @return the local context of the call
     */
    static LocalContext ctx(Context context) {
        return FunUtils.ctx(context);
    }

    /**
     * @param context the call context of the built-in
     * @return the global context carrying the sandbox root sets and capability grants
     */
    static GlobalContext global(Context context) {
        return FunUtils.ctx(context).globalContext;
    }

    /**
     * Demands a capability at runtime, for the operations whose capability need depends on
     * arguments or file-system state. Registration-time gating hides a built-in whose
     * always-needed capability is missing; this check covers the situational ones, failing
     * with a policy denial that names the missing capability and the demanding built-in.
     *
     * @param context    the call context
     * @param capability the situationally needed capability
     * @param operation  what the script attempted, for the error message
     * @throws ExecutionException if the capability is not granted
     */
    void demand(Context context, Capability capability, String operation) throws ExecutionException {
        if (!global(context).capabilitiesGranted(Set.of(capability))) {
            throw new ExecutionException(
                    "%s requires the %s capability, which the sandbox policy does not grant (in '%s')",
                    operation, capability, functionName);
        }
    }

    /**
     * Resolves a charset name to a {@link Charset}, reporting a script-level error for an
     * unknown or illegal name.
     *
     * @param name the charset name, e.g. {@code "UTF-8"}
     * @return the charset
     */
    Charset charset(String name) throws ExecutionException {
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            throw new ExecutionException(e, "Unknown charset '" + name + "' in '" + functionName + "'");
        }
    }

    /**
     * Enforces decision D11: a binary write accepts {@code bin} content only; a string must be
     * converted explicitly (e.g. {@code s.bytes(charset)}) so the encoding stays explicit.
     *
     * @param content the content argument of the write
     * @return the content bytes
     */
    byte[] binContent(Object content) throws ExecutionException {
        if (content instanceof byte[] bytes) {
            return bytes;
        }
        throw new ExecutionException(
                "Binary '%s' accepts a bin content only; convert explicitly, e.g. with .bytes(charset)",
                functionName);
    }
}
