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

/*snippet builtin0560

=== `mkdir`

Creates a directory.

   fn mkdir(path: str, @recurse: bool = true) -> none

With `recurse` (the default), missing parent directories are created as well, and an already
existing directory is not an error. With `recurse=false`, exactly one directory is created;
a missing parent or an existing target is an error. In a sandbox, the path must lie under a
read-write root.

The function requires the `FILE_CREATE` capability.

end snippet */

/**
 * Creates a directory, by default with all its missing parents. See the {@code FILE_IO.md}
 * design document.
 */
@Name("mkdir")
@RequiresCapability(Capability.FILE_CREATE)
public class Mkdir implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var recurse = args.at(1).as(Boolean.class);
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        try {
            if (recurse) {
                Files.createDirectories(target);
            } else {
                Files.createDirectory(target);
            }
            return null;
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot create directory '" + path + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public Mkdir() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("recurse").bool().named().defaultValue(true)
        ).done();
    }
}
