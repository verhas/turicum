package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.RequiresCapability;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngObject;
import ch.turic.utils.parameter.Declare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0558

=== `file_stat`

Returns the metadata of a file-system entry as an object.

   fn file_stat(path: str) -> obj

The result object has the fields

* `size` — the size in bytes,
* `modified`, `created` — the modification and creation times as epoch milliseconds,
* `is_file`, `is_dir`, `is_symlink` — the kind of the entry (`is_symlink` refers to the path
itself; the other fields follow the link),
* `readable`, `writable` — whether the hosting process may read/write the entry.

The function requires the `FILE_READ` capability.

end snippet */

/**
 * Returns the metadata of a file-system entry as an object. See the {@code FILE_IO.md}
 * design document.
 */
@Name("file_stat")
@RequiresCapability(Capability.FILE_READ)
public class FileStat implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var file = SafePath.forRead(FileIo.global(ctx), path).requireExists();
        try {
            final var attributes = Files.readAttributes(file, BasicFileAttributes.class);
            final var result = LngObject.newEmpty(FileIo.ctx(ctx));
            result.setField("size", attributes.size());
            result.setField("modified", attributes.lastModifiedTime().toMillis());
            result.setField("created", attributes.creationTime().toMillis());
            result.setField("is_file", attributes.isRegularFile());
            result.setField("is_dir", attributes.isDirectory());
            result.setField("is_symlink", Files.isSymbolicLink(file));
            result.setField("readable", Files.isReadable(file));
            result.setField("writable", Files.isWritable(file));
            return result;
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot stat file '" + path + "': " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileStat() {
        this.params = Declare.params(
                param("path").str().positional().mandatory()
        ).done();
    }
}
