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

/*snippet builtin0564

=== `file_copy`

Copies a file.

   fn file_copy(src: str, dst: str, @overwrite: bool = false) -> none

Copies the file `src` to `dst`. When `dst` exists, it is replaced only with
`overwrite=true`; without it, an existing target is an error. In a sandbox, `src` must lie
under a file root (read-only or read-write) and `dst` under a read-write root.

The function requires the `FILE_READ` capability; at runtime it additionally requires
`FILE_WRITE` when the target exists and `FILE_CREATE` when it does not.

end snippet */

/**
 * Copies a file. See the {@code FILE_IO.md} design document.
 */
@Name("file_copy")
@RequiresCapability(Capability.FILE_READ)
public class FileCopy implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var src = args.at(0).as(String.class);
        final var dst = args.at(1).as(String.class);
        final var overwrite = args.at(2).as(Boolean.class);
        final var source = SafePath.forRead(FileIo.global(ctx), src).requireExists();
        final var target = SafePath.forWrite(FileIo.global(ctx), dst);
        if (Files.exists(target)) {
            fileIo.demand(ctx, Capability.FILE_WRITE, "Copying over the existing file '" + dst + "'");
        } else {
            fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the file '" + dst + "'");
        }
        try {
            final var options = overwrite
                    ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                    : new CopyOption[0];
            Files.copy(source, target, options);
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e,
                    "Cannot copy '" + src + "' to '" + dst + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileCopy() {
        this.params = Declare.params(
                param("src").str().positional().mandatory(),
                param("dst").str().positional().mandatory(),
                param("overwrite").bool().named().defaultValue(false)
        ).done();
    }
}
