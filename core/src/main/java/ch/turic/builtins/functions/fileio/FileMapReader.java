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

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0578

=== `file_map_reader`

Maps a file — or a region of it — into memory for reading.

   fn file_map_reader(path: str, @offset: int = 0, @length: int|none = none) -> handle

`length=none` maps from `offset` to the end of the file. The handle has the methods

* `length()` — the length of the mapped region in bytes;
* `get(pos, n)` — `n` bytes at offset `pos` within the mapping, as a `bin`;
* `close()` — invalidates the handle;
* `entry()` / `exit(e)` — the `with` protocol.

CAUTION: On Java 21 there is no deterministic unmap: `close()` invalidates the handle, but
the mapped pages are released only when the buffer is garbage collected. On Windows, the
file cannot be deleted or truncated while it is mapped. The total length of all live
mappings of a session is capped by the sandbox policy's `maxMappedBytes` — the untrusted
default is `0`, so an untrusted script can map files only when the host opts in.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Maps a file region into memory for reading. See the {@code FILE_IO.md} design document,
 * §5.5 and decision D10.
 */
@Name("file_map_reader")
@RequiresCapability(Capability.FILE_READ)
public class FileMapReader implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var offset = Cast.toLong(args.at(1).get());
        final var lengthArg = args.at(2).get();
        final var length = lengthArg == null ? null : (Long) Cast.toLong(lengthArg);
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        return new LngMappedFile(FileIo.global(ctx), path, file, false, offset, length);
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileMapReader() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("offset").integer().named().defaultValue(0),
                param("length").integer().or().none().named().defaultNone()
        ).done();
    }
}
