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

import static ch.turic.utils.parameter.Declare.Parameter.param;

/*snippet builtin0556

=== `file_exists`, `is_file`, `is_dir`

File-system tests.

   fn file_exists(path: str) -> bool
   fn is_file(path: str)     -> bool
   fn is_dir(path: str)      -> bool

`file_exists` returns whether the path names an existing entry; `is_file` whether it is a
regular file; `is_dir` whether it is a directory. Symbolic links are followed. Path
resolution and sandbox confinement are the same as for `file_read`; a relative path that
exists under none of the configured roots simply yields `false`.

The functions require the `FILE_READ` capability.

end snippet */

/**
 * Returns whether a path names an existing file-system entry. See the {@code FILE_IO.md}
 * design document.
 */
@Name("file_exists")
@RequiresCapability(Capability.FILE_READ)
public class FileExists implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var path = args.at(0).as(String.class);
        return SafePath.forRead(FileIo.global(ctx), path).exists();
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public FileExists() {
        this.params = Declare.params(
                param("path").str().positional().mandatory()
        ).done();
    }

    /**
     * Returns whether a path names a regular file (following symbolic links).
     */
    @Name("is_file")
    @RequiresCapability(Capability.FILE_READ)
    public static class IsFile implements TuriFunction {

        @Override
        public Object call(Context ctx, Object[] arguments) throws ExecutionException {
            final var args = new FunUtils.ArgumentsHolder(arguments, name());
            final var path = args.at(0).as(String.class);
            return Files.isRegularFile(SafePath.forRead(FileIo.global(ctx), path).path());
        }

        @Override
        public ParameterList parameters() {
            return params;
        }

        final ParameterList params;

        public IsFile() {
            this.params = Declare.params(
                    param("path").str().positional().mandatory()
            ).done();
        }
    }

    /**
     * Returns whether a path names a directory (following symbolic links).
     */
    @Name("is_dir")
    @RequiresCapability(Capability.FILE_READ)
    public static class IsDir implements TuriFunction {

        @Override
        public Object call(Context ctx, Object[] arguments) throws ExecutionException {
            final var args = new FunUtils.ArgumentsHolder(arguments, name());
            final var path = args.at(0).as(String.class);
            return Files.isDirectory(SafePath.forRead(FileIo.global(ctx), path).path());
        }

        @Override
        public ParameterList parameters() {
            return params;
        }

        final ParameterList params;

        public IsDir() {
            this.params = Declare.params(
                    param("path").str().positional().mandatory()
            ).done();
        }
    }
}
