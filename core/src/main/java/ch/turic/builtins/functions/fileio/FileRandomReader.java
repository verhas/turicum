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

import java.nio.file.StandardOpenOption;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0574

=== `file_random_reader`

Opens a file for random-access reading and returns a read handle.

   fn file_random_reader(path: str) -> handle

Random access is inherently byte-oriented: positions and sizes are byte offsets, there is no
text mode, and reads return `bin` values. The handle has the methods

* `position()` — the current position;
* `seek(pos)` — sets the position; a position past the end of the file is legal, reads
there report the end of the file;
* `size()` — the current file size in bytes;
* `read(n)` — up to `n` bytes as a `bin` from the current position, advancing it; `none` at
the end of the file;
* `read_at(pos, n)` — absolute-positioned read; does *not* move the position;
* `close()` — idempotent;
* `entry()` / `exit(e)` — the `with` protocol.

A handle that is never closed is force-closed when the session ends. Path resolution and
sandbox confinement are the same as for `file_read`.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Opens a file for random-access reading. See the {@code FILE_IO.md} design document, §5.4.
 */
@Name("file_random_reader")
@RequiresCapability(Capability.FILE_READ)
public class FileRandomReader implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        return new LngRandomFile(FileIo.global(ctx), path, file, false,
                new java.nio.file.OpenOption[]{StandardOpenOption.READ});
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileRandomReader() {
        this.params = Declare.params(
                param("path").str().positional().mandatory()
        ).done();
    }
}
