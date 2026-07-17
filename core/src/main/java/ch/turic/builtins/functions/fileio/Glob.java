package ch.turic.builtins.functions.fileio;

import ch.turic.Capability;
import ch.turic.Context;
import ch.turic.RequiresCapability;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.utils.parameter.Declare;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;
import static ch.turic.utils.parameter.Declare.Parameter.param;
/*snippet builtin0110

=== `glob`

`glob` is a built-in function to list files.

   fn glob(pattern: str,@path: str|none=none,@recursive: bool=false)

The function has one mandatory and two optional parameters.

* `patter` is a mandatory string parameter.
  It has to define the file name pattern.
  The list matches the usual "glob" patterns, which means

** `pass:[*]` matches zero or more characters, except `/`,
** `pass:[**]` matches zero or more any characters, and
** `pass:[?]` matches exactly one any character.

* `path` optional, named, string parameter.
If not specified, the current working directory will be used.

* `recursive` optional, named, boolean parameter.
Default value is `false`.

The following example lists the first three class files in the test target directory:

{%S glob1%}

The second example lists the files in the current working directory only.
It is also an example that shows how to import only one function from the import library,
which otherwise exports multiple functions and classes.

{%S glob2%}

The directories in the result list have a trailing `/` at the end of their names (it is guaranteed).

In a sandbox with configured file roots, `glob` is confined by the file read/read-write
roots (not by the import root): a relative pattern is globbed under every root and the
results are the union over the roots; an absolute pattern must point under a root; a result
that a symbolic link would carry outside the roots is silently omitted.

end snippet */

/**
 * This function is used to list file names in a directory, possibly using recursion.
 * <p>
 * <pre>{@code
 * // 'file_list' will contain all the files without recursion
 * let file_list = glob("*")
 * die "" when type(file_list) != "lst"
 * die "" when len(file_list) == 0
 *
 * // get all the files recursively
 * let all_files = glob("*",recursive=true)
 * // it has to be at least the same number of files, presumably more
 * die "" when len(file_list) > len(all_files)
 * }</pre>
 */
@Name("glob")
@RequiresCapability(Capability.FILE_READ)
public class Glob implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class, optional(String.class), Boolean.class);
        var pattern = args.at(0).as(String.class);
        var pathOpt = args.at(1).optional(String.class);
        final var recursive = args.at(2).as(Boolean.class);
        if (pathOpt.isPresent()) {
            var path = pathOpt.get();
            if (!path.endsWith("/")) {
                path += "/";
            }
            pattern = path + pattern;
        }
        final var globalContext = FileIo.global(ctx);
        final var result = new LngList();
        try {
            if (!SafePath.isConfined(globalContext)) {
                result.addAll(ch.turic.utils.Glob.glob(pattern, recursive));
            } else {
                result.addAll(confinedGlob(globalContext, pattern, recursive));
            }
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(e, "Error, while globbing file names in glob(): '" + e.getMessage() + "'");
        }
        return result;
    }

    /**
     * Globbing under sandbox file roots: an absolute pattern must have its base directory
     * under a root; a relative pattern is globbed under every root and the results are the
     * union over the roots. Every result is re-checked so that a symlink inside a root cannot
     * list content outside it — such results are silently omitted, the listing does not fail.
     */
    private static LinkedHashSet<String> confinedGlob(ch.turic.memory.GlobalContext globalContext,
                                                      String pattern, boolean recursive) throws Exception {
        final var results = new LinkedHashSet<String>();
        if (isAbsolutePattern(pattern)) {
            final var base = ch.turic.utils.Glob.baseDirOf(pattern);
            // throws the policy denial when the base directory is outside every root
            SafePath.forRead(globalContext, base);
            collect(globalContext, results, base, pattern, recursive);
        } else {
            for (final var root : SafePath.readRootList(globalContext)) {
                final var rooted = root + "/" + pattern;
                final var base = ch.turic.utils.Glob.baseDirOf(rooted);
                SafePath.forRead(globalContext, base);
                if (Files.isDirectory(Path.of(base))) {
                    collect(globalContext, results, base, rooted, recursive);
                }
            }
        }
        return results;
    }

    private static void collect(ch.turic.memory.GlobalContext globalContext,
                                LinkedHashSet<String> results,
                                String base, String pattern, boolean recursive) throws Exception {
        for (final var relative : ch.turic.utils.Glob.glob(pattern, recursive)) {
            final var absolute = Path.of(base).resolve(
                    relative.endsWith("/") ? relative.substring(0, relative.length() - 1) : relative);
            if (SafePath.isReadable(globalContext, absolute)) {
                results.add(relative);
            }
        }
    }

    private static boolean isAbsolutePattern(String pattern) {
        final int star = pattern.indexOf('*');
        final int qm = pattern.indexOf('?');
        final int firstWildcard = star < 0 ? qm : qm < 0 ? star : Math.min(star, qm);
        final var prefix = firstWildcard < 0 ? pattern : pattern.substring(0, firstWildcard);
        try {
            return Path.of(prefix).isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParameterList parameters() {
        return params;
    }

    final ParameterList params;

    public Glob() {
        this.params = Declare.params(
                param("pattern").str().positional().mandatory(),
                param("path").str().or().none().named().defaultNone(),
                param("recursive").bool().named().defaultValue(false)
        ).done();
    }
}
