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

/*snippet builtin0568

=== `tmp_file`, `tmp_dir`

Creates a temporary file or directory in the session's scratch area.

   fn tmp_file(@prefix: str|none = none, @suffix: str|none = none) -> str
   fn tmp_dir(@prefix: str|none = none) -> str

Both functions return the absolute path of the freshly created entry as a string. The
scratch area is a per-session directory created on first use; it acts as an additional
read-write file root, so every file built-in can operate on the entries under it — and it is
deleted, with everything in it, when the session ends.

The functions require the `FILE_TEMP` capability. Granting `FILE_TEMP` implies the whole
file capability family (`FILE_READ`, `FILE_WRITE`, `FILE_CREATE`, `FILE_DELETE`); there is
no use case for temp files that can be created but not read and written.

end snippet */

/**
 * Creates a temporary file in the session's scratch directory. See the {@code FILE_IO.md}
 * design document, decision D8.
 */
@Name("tmp_file")
@RequiresCapability(Capability.FILE_TEMP)
public class TmpFile implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = new FunUtils.ArgumentsHolder(arguments, name());
        final var prefix = args.at(0).as(String.class, null);
        final var suffix = args.at(1).as(String.class, null);
        final var root = FileIo.global(ctx).tempRoot();
        try {
            return Files.createTempFile(root, prefix, suffix).toString();
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot create a temp file: " + e.getMessage());
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public TmpFile() {
        this.params = Declare.params(
                param("prefix").str().or().none().named().defaultNone(),
                param("suffix").str().or().none().named().defaultNone()
        ).done();
    }

    /**
     * Creates a temporary directory in the session's scratch directory.
     */
    @Name("tmp_dir")
    @RequiresCapability(Capability.FILE_TEMP)
    public static class TmpDir implements TuriFunction {

        @Override
        public Object call(Context ctx, Object[] arguments) throws ExecutionException {
            final var args = new FunUtils.ArgumentsHolder(arguments, name());
            final var prefix = args.at(0).as(String.class, null);
            final var root = FileIo.global(ctx).tempRoot();
            try {
                return Files.createTempDirectory(root, prefix).toString();
            } catch (IOException e) {
                throw new ExecutionException(e, "Cannot create a temp directory: " + e.getMessage());
            }
        }

        @Override
        public ParameterList parameters() {
            return params;
        }

        final ParameterList params;

        public TmpDir() {
            this.params = Declare.params(
                    param("prefix").str().or().none().named().defaultNone()
            ).done();
        }
    }
}
