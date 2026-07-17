package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.RequiresCapability;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.utils.parameter.Declare;

import java.io.IOException;
import java.nio.file.Files;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0552

=== `file_lines`

Reads a text file as a list of lines.

   fn file_lines(path: str, @charset: str = "UTF-8") -> lst

The result is a `lst` of `str`, one element per line, without the line terminators. Path
resolution and sandbox confinement are the same as for `file_read`.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Reads a text file as a list of lines. See the {@code FILE_IO.md} design document.
 */
@Name("file_lines")
@RequiresCapability(Capability.FILE_READ)
public class FileLines implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var charset = fileIo.charset(args.at(1).as(String.class));
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        try {
            final var result = new LngList();
            result.addAll(Files.readAllLines(file, charset));
            return result;
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

    public FileLines() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("charset").str().named().defaultValue("UTF-8")
        ).done();
    }
}
