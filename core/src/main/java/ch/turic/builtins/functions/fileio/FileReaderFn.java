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

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0570

=== `file_reader`

Opens a file for sequential reading and returns a read handle.

   fn file_reader(path: str, @binary: bool = false, @charset: str = "UTF-8") -> handle

The handle has the methods

* `read_line()` — the next line without its terminator, `none` at the end of the file
(text mode only);
* `read(n)` — up to `n` characters as a `str` (in binary mode: up to `n` bytes as a `bin`),
`none` at the end of the file;
* `read_all()` — the rest of the stream as one `str` (binary mode: `bin`);
* `close()` — closes the handle; idempotent;
* `entry()` / `exit(e)` — the `with` protocol, so the handle can be used in a
`with file_reader(...) as f { ... }` command, which closes it on every exit path.

Every operation on a closed handle is an error (except `close`). A handle that is never
closed is force-closed when the session ends. Path resolution and sandbox confinement are
the same as for `file_read`.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Opens a file for sequential reading and returns a {@link LngFileReader} handle. See the
 * {@code FILE_IO.md} design document.
 */
@Name("file_reader")
@RequiresCapability(Capability.FILE_READ)
public class FileReaderFn implements TuriFunction {
    private final FileIo fileIo = new FileIo(name());

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var binary = args.at(1).as(Boolean.class);
        final var charset = fileIo.charset(args.at(2).as(String.class));
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        return new LngFileReader(FileIo.global(ctx), path, file, binary, charset);
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileReaderFn() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("binary").bool().named().defaultValue(false),
                param("charset").str().named().defaultValue("UTF-8")
        ).done();
    }
}
