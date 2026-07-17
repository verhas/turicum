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

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0550

=== `file_read`

Reads the whole content of a file.

   fn file_read(path: str, @binary: bool = false, @charset: str = "UTF-8") -> str | bin

* `path` is the file to read.
In a sandbox with configured file roots, a relative path is resolved against the roots in
declaration order, taking the first existing candidate; the resolved file must lie under one
of the roots.

* `binary` selects binary mode: the result is a `bin` with the exact bytes of the file.
In text mode (the default) the result is a `str` decoded using `charset`.

* `charset` is the character set used to decode the file in text mode.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Reads the whole content of a file, as a string (text mode) or as a {@code bin} (binary
 * mode). See the {@code FILE_IO.md} design document.
 */
@Name("file_read")
@RequiresCapability(Capability.FILE_READ)
public class FileRead implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var binary = args.at(1).as(Boolean.class);
        final var charset = fileIo.charset(args.at(2).as(String.class));
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        try {
            final var bytes = Files.readAllBytes(file);
            return binary ? bytes : new String(bytes, charset);
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot read file '" + path + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileRead() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("binary").bool().named().defaultValue(false),
                param("charset").str().named().defaultValue("UTF-8")
        ).done();
    }
}
