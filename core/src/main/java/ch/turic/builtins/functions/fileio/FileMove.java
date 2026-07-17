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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0566

=== `file_move`

Moves (renames) a file.

   fn file_move(src: str, dst: str, @overwrite: bool = false) -> none

Moves the file `src` to `dst`. When `dst` exists, it is replaced only with `overwrite=true`.
Moving always removes the source, so in a sandbox both `src` and `dst` must lie under a
read-write root.

The function requires the `FILE_DELETE` capability (it always removes the source); at
runtime it additionally requires `FILE_WRITE` when the target exists and `FILE_CREATE` when
it does not.

end snippet */

/**
 * Moves (renames) a file. See the {@code FILE_IO.md} design document.
 */
@Name("file_move")
@RequiresCapability(Capability.FILE_DELETE)
public class FileMove implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var src = args.at(0).as(String.class);
        final var dst = args.at(1).as(String.class);
        final var overwrite = args.at(2).as(Boolean.class);
        // removing the source is a mutation, so the source is confined like a write target
        final var source = SafePath.forWrite(FileIo.global(ctx), src);
        final var target = SafePath.forWrite(FileIo.global(ctx), dst);
        if (Files.exists(target)) {
            fileIo.demand(ctx, Capability.FILE_WRITE, "Moving over the existing file '" + dst + "'");
        } else {
            fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the file '" + dst + "'");
        }
        try {
            final var options = overwrite
                    ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                    : new CopyOption[0];
            Files.move(source, target, options);
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e,
                    "Cannot move '" + src + "' to '" + dst + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileMove() {
        this.params = Declare.params(
                param("src").str().positional().mandatory(),
                param("dst").str().positional().mandatory(),
                param("overwrite").bool().named().defaultValue(false)
        ).done();
    }
}
