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

/*snippet builtin0580

=== `file_map_editor`

Maps a file — or a region of it — into memory for reading and writing.

   fn file_map_editor(path: str, @offset: int = 0, @length: int|none = none) -> handle

`length=none` maps from `offset` to the end of the file; an explicit `length` may extend the
file to `offset+length` bytes. The handle has every method of the `file_map_reader` handle,
plus

* `put(pos, x)` — writes the `bin` `x` at offset `pos` within the mapping; it cannot grow
the mapping;
* `flush()` — pushes the changes to the file.

`exit` flushes and closes, so a `with file_map_editor(...) as m { ... }` block persists its
changes on every exit path. Writes accept `bin` values only. The Java 21 unmap caveat and
the `maxMappedBytes` cap of `file_map_reader` apply here as well. In a sandbox, the path
must lie under a read-write root.

The function requires the `FILE_WRITE` capability.

end snippet */

/**
 * Maps a file region into memory for reading and writing. See the {@code FILE_IO.md} design
 * document, §5.5 and decision D10.
 */
@Name("file_map_editor")
@RequiresCapability(Capability.FILE_WRITE)
public class FileMapEditor implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var offset = Cast.toLong(args.at(1).get());
        final var lengthArg = args.at(2).get();
        final var length = lengthArg == null ? null : Cast.toLong(lengthArg);
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        return new LngMappedFile(FileIo.global(ctx), path, target, true, offset, length);
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileMapEditor() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("offset").integer().named().defaultValue(0),
                param("length").integer().or().none().named().defaultNone()
        ).done();
    }
}
