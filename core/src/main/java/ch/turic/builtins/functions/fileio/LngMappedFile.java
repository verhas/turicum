package ch.turic.builtins.functions.fileio;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.HasFields;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The memory-mapped handle returned by the {@code file_map_reader} and
 * {@code file_map_editor} built-ins (decision D10(a) of {@code FILE_IO.md}: shipped on
 * Java 21 with the documented caveat and the {@code maxMappedBytes} policy cap).
 * <p>
 * <b>Java 21 caveat — no deterministic unmap.</b> {@code close()} invalidates the
 * <em>handle</em> (every later {@code get}/{@code put} is an error) and drops the buffer
 * reference, but the pages stay mapped until the buffer is garbage-collected; on Windows the
 * file cannot be deleted or truncated while mapped. The mapping length is charged against the
 * session's {@code maxMappedBytes} budget while the handle is live. When the project moves to
 * Java 22+, the implementation can switch to {@code MemorySegment}/{@code Arena} and
 * {@code close()} becomes a true unmap with no script-visible API change.
 */
public class LngMappedFile implements HasFields, AutoCloseable {
    private final GlobalContext globalContext;
    private final String scriptPath;
    private final String handleName;
    private final long length;
    private volatile MappedByteBuffer buffer; // null once closed
    private final Map<String, Object> fieldMap;
    private final FileIo fileIo;

    LngMappedFile(GlobalContext globalContext, String scriptPath, Path file, boolean editable,
                  long offset, Long lengthOrNull) throws ExecutionException {
        this.globalContext = globalContext;
        this.scriptPath = scriptPath;
        this.handleName = editable ? "file_map_editor" : "file_map_reader";
        this.fileIo = new FileIo(handleName);
        if (offset < 0) {
            throw new ExecutionException("The mapping offset must be non-negative, got %d", offset);
        }
        try (final var channel = editable
                ? FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)
                : FileChannel.open(file, StandardOpenOption.READ)) {
            final long mapLength;
            if (lengthOrNull == null) {
                final var size = channel.size();
                if (offset > size) {
                    throw new ExecutionException(
                            "The mapping offset %d is beyond the end of '%s' (%d bytes)", offset, scriptPath, size);
                }
                mapLength = size - offset;
            } else {
                mapLength = lengthOrNull;
            }
            if (mapLength < 0 || mapLength > Integer.MAX_VALUE) {
                throw new ExecutionException(
                        "The mapping length must be between 0 and %d, got %d", Integer.MAX_VALUE, mapLength);
            }
            this.length = mapLength;
            // charge the mapping against the session budget before creating it; released on close
            globalContext.reserveMappedBytes(mapLength);
            try {
                this.buffer = channel.map(
                        editable ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY,
                        offset, mapLength);
            } catch (IOException | RuntimeException e) {
                globalContext.releaseMappedBytes(mapLength);
                throw e;
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot map file '" + scriptPath + "': " + e.getMessage());
        }
        final var fields = new HashMap<String, Object>();
        fields.put("length", new TuriMethod<>(args -> length));
        fields.put("get", new TuriMethod<>(this::get));
        fields.put("close", new TuriMethod<>(args -> {
            close();
            return null;
        }));
        fields.put("entry", new TuriMethod<>(args -> this));
        fields.put("exit", new TuriMethod<>(args -> {
            if (editable && buffer != null) {
                buffer.force();
            }
            close();
            return false;
        }));
        if (editable) {
            fields.put("put", new TuriMethod<>(this::put));
            fields.put("flush", new TuriMethod<>(args -> {
                ensureOpen().force();
                return null;
            }));
        }
        this.fieldMap = Map.copyOf(fields);
        globalContext.registerCloseable(this);
    }

    private MappedByteBuffer ensureOpen() throws ExecutionException {
        final var b = buffer;
        if (b == null) {
            throw new ExecutionException("The %s of '%s' is already closed", handleName, scriptPath);
        }
        return b;
    }

    private int position(Object[] args, int index, String method, int payload) throws ExecutionException {
        if (args == null || index >= args.length) {
            throw new ExecutionException("Missing argument %d of %s", index + 1, method);
        }
        final long pos = Cast.toLong(args[index]);
        if (pos < 0 || pos + payload > length) {
            throw new ExecutionException(
                    "Position %d (+%d bytes) is outside the mapped region of %d bytes", pos, payload, length);
        }
        return (int) pos;
    }

    private Object get(Object[] args) throws ExecutionException {
        final var b = ensureOpen();
        if (args == null || args.length != 2) {
            throw new ExecutionException("get(pos, n) needs exactly two arguments");
        }
        final long n = Cast.toLong(args[1]);
        if (n < 0 || n > length) {
            throw new ExecutionException("The get count must be between 0 and %d, got %d", length, n);
        }
        final var pos = position(args, 0, "get(pos, n)", (int) n);
        final var bytes = new byte[(int) n];
        b.get(pos, bytes);
        return bytes;
    }

    private Object put(Object[] args) throws ExecutionException {
        final var b = ensureOpen();
        if (args == null || args.length != 2) {
            throw new ExecutionException("put(pos, x) needs exactly two arguments");
        }
        final var bytes = fileIo.binContent(args[1]);
        final var pos = position(args, 0, "put(pos, x)", bytes.length);
        b.put(pos, bytes);
        return null;
    }

    /**
     * Invalidates the handle and returns the mapping budget; idempotent. The pages themselves
     * stay mapped until the buffer is garbage-collected — see the class comment.
     */
    @Override
    public void close() {
        if (buffer == null) {
            return;
        }
        buffer = null;
        globalContext.unregisterCloseable(this);
        globalContext.releaseMappedBytes(length);
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on a " + handleName);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var field = fieldMap.get(name);
        if (field == null) {
            throw new ExecutionException("Unknown " + handleName + " field: " + name);
        }
        return field;
    }

    @Override
    public Set<String> fields() {
        return fieldMap.keySet();
    }

    @Override
    public String toString() {
        return handleName + "[" + scriptPath + (buffer == null ? ",closed]" : "]");
    }
}
