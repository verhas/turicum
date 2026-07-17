package ch.turic.builtins.functions.fileio;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.HasFields;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * The sequential read handle returned by the {@code file_reader} built-in. A Java-implemented
 * {@code with}-compatible resource (decision D3(b) of {@code FILE_IO.md}): it implements
 * {@link HasFields} with an immutable field map, so the script cannot reassign {@code exit}.
 * <p>
 * In text mode the handle wraps a {@link BufferedReader} with the requested charset; in
 * binary mode an {@link InputStream}, and the read methods return {@code bin} values. The
 * handle registers itself with the {@link GlobalContext} closeable registry, so a handle the
 * script never closes is force-closed when the session ends.
 */
public class LngFileReader implements HasFields, AutoCloseable {
    private final GlobalContext globalContext;
    private final String scriptPath;
    private final BufferedReader reader; // text mode, null in binary mode
    private final InputStream stream;    // binary mode, null in text mode
    private volatile boolean closed = false;
    private final Map<String, Object> fieldMap;

    LngFileReader(GlobalContext globalContext, String scriptPath, Path file, boolean binary, Charset charset)
            throws ExecutionException {
        this.globalContext = globalContext;
        this.scriptPath = scriptPath;
        try {
            if (binary) {
                this.stream = Files.newInputStream(file);
                this.reader = null;
            } else {
                this.reader = Files.newBufferedReader(file, charset);
                this.stream = null;
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot open file '" + scriptPath + "': " + e.getMessage());
        }
        this.fieldMap = Map.of(
                "read_line", new TuriMethod<>(this::readLine),
                "read", new TuriMethod<>(this::read),
                "read_all", new TuriMethod<>(args -> readAll()),
                "close", new TuriMethod<>(args -> {
                    close();
                    return null;
                }),
                "entry", new TuriMethod<>(args -> this),
                "exit", new TuriMethod<>(args -> {
                    close();
                    return false;
                })
        );
        globalContext.registerCloseable(this);
    }

    private void ensureOpen() throws ExecutionException {
        if (closed) {
            throw new ExecutionException("The file_reader of '%s' is already closed", scriptPath);
        }
    }

    private Object readLine(Object[] args) throws ExecutionException {
        ensureOpen();
        if (reader == null) {
            throw new ExecutionException("read_line is not available on a binary file_reader");
        }
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot read '" + scriptPath + "': " + e.getMessage());
        }
    }

    private Object read(Object[] args) throws ExecutionException {
        ensureOpen();
        if (args == null || args.length != 1) {
            throw new ExecutionException("read(n) needs exactly one argument");
        }
        final long n = Cast.toLong(args[0]);
        if (n < 0 || n > Integer.MAX_VALUE) {
            throw new ExecutionException("read(n) needs a non-negative count, got %d", n);
        }
        try {
            if (stream != null) {
                final var bytes = stream.readNBytes((int) n);
                return bytes.length == 0 && n > 0 ? null : bytes;
            }
            final var buffer = new char[(int) n];
            final var count = reader.read(buffer, 0, (int) n);
            return count < 0 ? null : new String(buffer, 0, count);
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot read '" + scriptPath + "': " + e.getMessage());
        }
    }

    private Object readAll() throws ExecutionException {
        ensureOpen();
        try {
            if (stream != null) {
                return stream.readAllBytes();
            }
            final var writer = new StringWriter();
            reader.transferTo(writer);
            return writer.toString();
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot read '" + scriptPath + "': " + e.getMessage());
        }
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
            if (stream != null) {
                stream.close();
            } else {
                reader.close();
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot close '" + scriptPath + "': " + e.getMessage());
        }
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on a file_reader");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var field = fieldMap.get(name);
        if (field == null) {
            throw new ExecutionException("Unknown file_reader field: " + name);
        }
        return field;
    }

    @Override
    public Set<String> fields() {
        return fieldMap.keySet();
    }

    @Override
    public String toString() {
        return "file_reader[" + scriptPath + (closed ? ",closed]" : "]");
    }
}
