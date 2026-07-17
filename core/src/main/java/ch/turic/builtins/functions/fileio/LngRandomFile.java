package ch.turic.builtins.functions.fileio;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.HasFields;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The random-access handle returned by the {@code file_random_reader} and
 * {@code file_random_editor} built-ins, backed by a {@link FileChannel}. Positions and sizes
 * are byte offsets; the handles have no text mode, so reads return {@code bin} values and
 * writes accept {@code bin} values only (decisions D6/D9/D11 of {@code FILE_IO.md}).
 * <p>
 * The editor form adds the mutating methods; the reader form does not even carry them, so a
 * script holding a reader handle cannot write through it. Same D3(b) design as
 * {@link LngFileReader}: immutable field map, {@code with} protocol, session-end force-close.
 */
public class LngRandomFile implements HasFields, AutoCloseable {
    private final GlobalContext globalContext;
    private final String scriptPath;
    private final String handleName;
    private final FileChannel channel;
    private volatile boolean closed = false;
    private final Map<String, Object> fieldMap;
    private final FileIo fileIo;

    LngRandomFile(GlobalContext globalContext, String scriptPath, Path file, boolean editable,
                  OpenOption[] options) throws ExecutionException {
        this.globalContext = globalContext;
        this.scriptPath = scriptPath;
        this.handleName = editable ? "file_random_editor" : "file_random_reader";
        this.fileIo = new FileIo(handleName);
        try {
            this.channel = FileChannel.open(file, options);
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot open file '" + scriptPath + "': " + e.getMessage());
        }
        final var fields = new HashMap<String, Object>();
        fields.put("position", new TuriMethod<>(args -> io(channel::position)));
        fields.put("seek", new TuriMethod<>(this::seek));
        fields.put("size", new TuriMethod<>(args -> io(channel::size)));
        fields.put("read", new TuriMethod<>(this::read));
        fields.put("read_at", new TuriMethod<>(this::readAt));
        fields.put("close", new TuriMethod<>(args -> {
            close();
            return null;
        }));
        fields.put("entry", new TuriMethod<>(args -> this));
        fields.put("exit", new TuriMethod<>(args -> {
            close();
            return false;
        }));
        if (editable) {
            fields.put("write", new TuriMethod<>(this::write));
            fields.put("write_at", new TuriMethod<>(this::writeAt));
            fields.put("truncate", new TuriMethod<>(this::truncate));
            fields.put("flush", new TuriMethod<>(args -> io(() -> {
                channel.force(false);
                return null;
            })));
        }
        this.fieldMap = Map.copyOf(fields);
        globalContext.registerCloseable(this);
    }

    private interface IoCall {
        Object call() throws IOException;
    }

    private Object io(IoCall call) throws ExecutionException {
        ensureOpen();
        try {
            return call.call();
        } catch (IOException e) {
            throw new ExecutionException(e, "I/O error on '" + scriptPath + "': " + e.getMessage());
        }
    }

    private void ensureOpen() throws ExecutionException {
        if (closed) {
            throw new ExecutionException("The %s of '%s' is already closed", handleName, scriptPath);
        }
    }

    private static long longArg(Object[] args, int index, String method) throws ExecutionException {
        if (args == null || index >= args.length) {
            throw new ExecutionException("Missing argument %d of %s", index + 1, method);
        }
        return Cast.toLong(args[index]);
    }

    private Object seek(Object[] args) throws ExecutionException {
        final var pos = longArg(args, 0, "seek(pos)");
        if (pos < 0) {
            throw new ExecutionException("seek(pos) needs a non-negative position, got %d", pos);
        }
        return io(() -> {
            channel.position(pos);
            return null;
        });
    }

    private Object read(Object[] args) throws ExecutionException {
        final var n = count(longArg(args, 0, "read(n)"));
        return io(() -> {
            final var buffer = ByteBuffer.allocate(n);
            final var read = channel.read(buffer);
            return result(buffer, read, n);
        });
    }

    private Object readAt(Object[] args) throws ExecutionException {
        final var pos = longArg(args, 0, "read_at(pos, n)");
        final var n = count(longArg(args, 1, "read_at(pos, n)"));
        return io(() -> {
            final var buffer = ByteBuffer.allocate(n);
            final var read = channel.read(buffer, pos);
            return result(buffer, read, n);
        });
    }

    private static int count(long n) throws ExecutionException {
        if (n < 0 || n > Integer.MAX_VALUE) {
            throw new ExecutionException("The read count must be between 0 and %d, got %d", Integer.MAX_VALUE, n);
        }
        return (int) n;
    }

    private static Object result(ByteBuffer buffer, int read, int requested) {
        if (read < 0 && requested > 0) {
            return null; // end of file
        }
        return read <= 0 ? new byte[0] : Arrays.copyOf(buffer.array(), read);
    }

    private Object write(Object[] args) throws ExecutionException {
        final var bytes = fileIo.binContent(args != null && args.length > 0 ? args[0] : null);
        return io(() -> {
            channel.write(ByteBuffer.wrap(bytes));
            return null;
        });
    }

    private Object writeAt(Object[] args) throws ExecutionException {
        final var pos = longArg(args, 0, "write_at(pos, x)");
        if (args.length < 2) {
            throw new ExecutionException("Missing argument 2 of write_at(pos, x)");
        }
        final var bytes = fileIo.binContent(args[1]);
        return io(() -> {
            channel.write(ByteBuffer.wrap(bytes), pos);
            return null;
        });
    }

    private Object truncate(Object[] args) throws ExecutionException {
        final var size = longArg(args, 0, "truncate(size)");
        if (size < 0) {
            throw new ExecutionException("truncate(size) needs a non-negative size, got %d", size);
        }
        return io(() -> {
            channel.truncate(size);
            return null;
        });
    }

    /**
     * Closes the handle; idempotent. Unregisters from the session's force-close registry.
     */
    @Override
    public void close() throws ExecutionException {
        if (closed) {
            return;
        }
        closed = true;
        globalContext.unregisterCloseable(this);
        try {
            channel.close();
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot close '" + scriptPath + "': " + e.getMessage());
        }
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
        return handleName + "[" + scriptPath + (closed ? ",closed]" : "]");
    }
}
