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

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0576

=== `file_random_editor`

Opens a file for random-access reading and in-place updating.

   fn file_random_editor(path: str, @create: bool = false, @truncate: bool = false) -> handle

* `create` creates the file when it is missing (this needs the `FILE_CREATE` capability at
runtime); without it, a missing file is an error.

* `truncate` empties the file when it is opened.

The handle has every method of the `file_random_reader` handle — the editor can also read;
write implies read — plus

* `write(x)` — writes the `bin` `x` at the current position, advancing it; writing past the
end of the file extends it;
* `write_at(pos, x)` — absolute-positioned write; does *not* move the position;
* `truncate(size)` — cuts the file to `size` bytes (the position clamps to the new end);
* `flush()` — forces pending writes to the file.

Writes accept `bin` values only; convert a string explicitly, e.g. with `s.bytes()`. In a
sandbox, the path must lie under a read-write root.

The function requires the `FILE_WRITE` capability.

end snippet */

/**
 * Opens a file for random-access reading and writing. See the {@code FILE_IO.md} design
 * document, §5.4.
 */
@Name("file_random_editor")
@RequiresCapability(Capability.FILE_WRITE)
public class FileRandomEditor implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var create = args.at(1).as(Boolean.class);
        final var truncate = args.at(2).as(Boolean.class);
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        if (create && !Files.exists(target)) {
            fileIo.demand(ctx, Capability.FILE_CREATE, "Creating the file '" + path + "'");
        }
        final var options = new ArrayList<OpenOption>();
        options.add(StandardOpenOption.READ);
        options.add(StandardOpenOption.WRITE);
        if (create) {
            options.add(StandardOpenOption.CREATE);
        }
        if (truncate) {
            options.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        return new LngRandomFile(FileIo.global(ctx), path, target, true,
                options.toArray(OpenOption[]::new));
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;
    private final FileIo fileIo = new FileIo(name());

    public FileRandomEditor() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("create").bool().named().defaultValue(false),
                param("truncate").bool().named().defaultValue(false)
        ).done();
    }
}
