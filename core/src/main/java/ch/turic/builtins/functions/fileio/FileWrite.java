package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.RequiresCapability;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.ParameterList;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.utils.parameter.Declare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0554

=== `file_write`

Writes the whole content of a file.

   fn file_write(path: str, content,
                 @append: bool = false,
                 @overwrite: bool = true,
                 @mkdirs: bool = false,
                 @binary: bool = false,
                 @charset: str = "UTF-8") -> none

* `path` is the file to write.
In a sandbox, a relative path resolves against the first declared read-write root, and the
resolved file must lie under a read-write root.

* `content` is the data to write.
In text mode anything is accepted and its string form is encoded using `charset`; in binary
mode the content must be a `bin`, written verbatim (convert a string explicitly, e.g. with
`s.bytes()`).

* `append` appends to the file instead of replacing its content.

* `overwrite` set to `false` makes writing an *existing* file an error; the file is created
atomically, so there is no race between the existence check and the creation.
It is ignored when `append` is set.

* `mkdirs` creates the missing parent directories of the target.

* `binary` selects binary mode.

The function requires the `FILE_WRITE` capability; when the target file does not exist — or
`mkdirs` has to create parent directories — it additionally requires `FILE_CREATE` at
runtime.

end snippet */

/**
 * Writes the whole content of a file, replacing, creating, or appending. See the
 * {@code FILE_IO.md} design document.
 */
@Name("file_write")
@RequiresCapability(Capability.FILE_WRITE)
public class FileWrite implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var content = args.at(1).get();
        final var append = args.at(2).as(Boolean.class);
        final var overwrite = args.at(3).as(Boolean.class);
        final var mkdirs = args.at(4).as(Boolean.class);
        final var binary = args.at(5).as(Boolean.class);
        final var charset = fileIo.charset(args.at(6).as(String.class));
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        final var bytes = binary
                ? fileIo.binContent(content)
                : Cast.toString(content).getBytes(charset);
        try {
            final var parent = target.getParent();
            if (mkdirs && parent != null && !Files.exists(parent)) {
                fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the parent directories of '" + path + "'");
                Files.createDirectories(parent);
            }
            // the runtime FILE_CREATE demand is made whenever the open options may create the
            // file; overwrite=false maps to CREATE_NEW, so the create is atomic and no
            // check-then-create race decides which capability applies
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
            Files.write(target, bytes, options.toArray(OpenOption[]::new));
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot write file '" + path + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileWrite() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("content").any().positional().mandatory(),
                param("append").bool().named().defaultValue(false),
                param("overwrite").bool().named().defaultValue(true),
                param("mkdirs").bool().named().defaultValue(false),
                param("binary").bool().named().defaultValue(false),
                param("charset").str().named().defaultValue("UTF-8")
        ).done();
    }
}
