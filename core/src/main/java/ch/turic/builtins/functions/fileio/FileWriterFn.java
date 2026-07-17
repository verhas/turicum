package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.RequiresCapability;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.utils.parameter.Declare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0572

=== `file_writer`

Opens a file for sequential writing and returns a write handle.

   fn file_writer(path: str,
                  @append: bool = false, @overwrite: bool = true,
                  @mkdirs: bool = false,
                  @binary: bool = false, @charset: str = "UTF-8") -> handle

The options have the same meaning as for `file_write`. The handle has the methods

* `write(x)` — writes the string form of `x` encoded with `charset` (in binary mode `x`
must be a `bin`, written verbatim);
* `write_line(x)` — `write(x)` followed by a newline (text mode only);
* `flush()` — flushes the buffered output;
* `close()` — flushes and closes; idempotent;
* `entry()` / `exit(e)` — the `with` protocol:

   with file_writer("report.txt") as out {
       out.write_line("header")
       for each line in file_lines("data.txt") {
           out.write_line(line)
       }
   } // closed here, also on exception

Every operation on a closed handle is an error (except `close`). A handle that is never
closed is force-closed when the session ends.

The function requires the `FILE_WRITE` capability; creating a missing target file — or
parent directories with `mkdirs` — additionally requires `FILE_CREATE` at runtime.

end snippet */

/**
 * Opens a file for sequential writing and returns a {@link LngFileWriter} handle. See the
 * {@code FILE_IO.md} design document.
 */
@Name("file_writer")
@RequiresCapability(Capability.FILE_WRITE)
public class FileWriterFn implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var append = args.at(1).as(Boolean.class);
        final var overwrite = args.at(2).as(Boolean.class);
        final var mkdirs = args.at(3).as(Boolean.class);
        final var binary = args.at(4).as(Boolean.class);
        final var charset = fileIo.charset(args.at(5).as(String.class));
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        final var parent = target.getParent();
        if (mkdirs && parent != null && !Files.exists(parent)) {
            fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the parent directories of '" + path + "'");
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new ExecutionException(e, "Cannot create the parent directories of '" + path + "': " + e.getMessage());
            }
        }
        if (!Files.exists(target)) {
            fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the file '" + path + "'");
        }
        final var options = new ArrayList<OpenOption>();
        options.add(StandardOpenOption.WRITE);
        if (append) {
            options.add(StandardOpenOption.CREATE);
            options.add(StandardOpenOption.APPEND);
        } else if (overwrite) {
            options.add(StandardOpenOption.CREATE);
            options.add(StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            options.add(StandardOpenOption.CREATE_NEW);
        }
        return new LngFileWriter(FileIo.global(ctx), path, target, binary, charset,
                options.toArray(OpenOption[]::new));
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileWriterFn() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("append").bool().named().defaultValue(false),
                param("overwrite").bool().named().defaultValue(true),
                param("mkdirs").bool().named().defaultValue(false),
                param("binary").bool().named().defaultValue(false),
                param("charset").str().named().defaultValue("UTF-8")
        ).done();
    }
}
