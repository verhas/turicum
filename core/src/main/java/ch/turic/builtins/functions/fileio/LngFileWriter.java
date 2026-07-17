package ch.turic.builtins.functions.fileio;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.HasFields;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * The sequential write handle returned by the {@code file_writer} built-in; the writing
 * counterpart of {@link LngFileReader}, with the same D3(b) design: immutable field map,
 * {@code with} protocol via {@code entry}/{@code exit}, session-end force-close through the
 * {@link GlobalContext} closeable registry.
 * <p>
 * In text mode {@code write} accepts anything and writes its string form encoded with the
 * requested charset; in binary mode it accepts {@code bin} content only (decision D11).
 */
public class LngFileWriter implements HasFields, AutoCloseable {
    private final GlobalContext globalContext;
    private final String scriptPath;
    private final Writer writer;        // text mode, null in binary mode
    private final OutputStream stream;  // binary mode, null in text mode
    private volatile boolean closed = false;
    private final Map<String, Object> fieldMap;
    private final FileIo fileIo = new FileIo("file_writer");

    LngFileWriter(GlobalContext globalContext, String scriptPath, Path file, boolean binary, Charset charset,
                  OpenOption[] options) throws ExecutionException {
        this.globalContext = globalContext;
        this.scriptPath = scriptPath;
        try {
            if (binary) {
                this.stream = Files.newOutputStream(file, options);
                this.writer = null;
            } else {
                this.writer = Files.newBufferedWriter(file, charset, options);
                this.stream = null;
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot open file '" + scriptPath + "': " + e.getMessage());
        }
        this.fieldMap = Map.of(
                "write", new TuriMethod<>(args -> {
                    write(args, false);
                    return null;
                }),
                "write_line", new TuriMethod<>(args -> {
                    write(args, true);
                    return null;
                }),
                "flush", new TuriMethod<>(args -> {
                    flush();
                    return null;
                }),
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
            throw new ExecutionException("The file_writer of '%s' is already closed", scriptPath);
        }
    }

    private void write(Object[] args, boolean line) throws ExecutionException {
        ensureOpen();
        if (args == null || args.length != 1) {
            throw new ExecutionException("%s needs exactly one argument", line ? "write_line(x)" : "write(x)");
        }
        try {
            if (stream != null) {
                if (line) {
                    throw new ExecutionException("write_line is not available on a binary file_writer");
                }
                stream.write(fileIo.binContent(args[0]));
            } else {
                writer.write(Cast.toString(args[0]));
                if (line) {
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot write '" + scriptPath + "': " + e.getMessage());
        }
    }

    private void flush() throws ExecutionException {
        ensureOpen();
        try {
            if (stream != null) {
                stream.flush();
            } else {
                writer.flush();
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot flush '" + scriptPath + "': " + e.getMessage());
        }
    }

    /**
     * Flushes and closes the handle; idempotent. Unregisters from the session's force-close
     * registry.
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
                writer.close();
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot close '" + scriptPath + "': " + e.getMessage());
        }
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on a file_writer");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var field = fieldMap.get(name);
        if (field == null) {
            throw new ExecutionException("Unknown file_writer field: " + name);
        }
        return field;
    }

    @Override
    public Set<String> fields() {
        return fieldMap.keySet();
    }

    @Override
    public String toString() {
        return "file_writer[" + scriptPath + (closed ? ",closed]" : "]");
    }
}
