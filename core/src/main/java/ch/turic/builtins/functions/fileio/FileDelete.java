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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0562

=== `file_delete`

Deletes a file or a directory.

   fn file_delete(path: str, @force: bool = false, @must_exist: bool = false) -> bool

The function returns whether something was deleted. Deleting a non-empty directory without
`force` is an error; with `force=true` the directory tree is deleted recursively —
the most destructive primitive of the file family, guarded by the `FILE_DELETE` capability
and, in a sandbox, by the read-write root confinement. With `must_exist=true`, a missing
path is an error instead of a `false` result.

The function requires the `FILE_DELETE` capability.

end snippet */

/**
 * Deletes a file or a directory, optionally recursively. See the {@code FILE_IO.md} design
 * document.
 */
@Name("file_delete")
@RequiresCapability(Capability.FILE_DELETE)
public class FileDelete implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        final var force = args.at(1).as(Boolean.class);
        final var mustExist = args.at(2).as(Boolean.class);
        final var target = SafePath.forWrite(FileIo.global(ctx), path);
        try {
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (mustExist) {
                    throw new ExecutionException("Cannot delete '%s': the file does not exist", path);
                }
                return false;
            }
            if (force && Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
                deleteRecursively(target);
                return true;
            }
            Files.delete(target);
            return true;
        } catch (DirectoryNotEmptyException e) {
            throw new ExecutionException(e,
                    "Cannot delete '" + path + "': the directory is not empty; use force=true to delete recursively");
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot delete '" + path + "': " + e.getMessage());
        }
    }

    private static void deleteRecursively(Path target) throws IOException {
        try (final var walk = Files.walk(target)) {
            final var failure = new IOException[1];
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    if (failure[0] == null) {
                        failure[0] = e;
                    }
                }
            });
            if (failure[0] != null) {
                throw failure[0];
            }
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileDelete() {
        this.params = Declare.params(
                param("path").str().positional().mandatory(),
                param("force").bool().named().defaultValue(false),
                param("must_exist").bool().named().defaultValue(false)
        ).done();
    }
}
