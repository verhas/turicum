package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;
/*snippet builtin0110

=== `glob`

`_glob` is a built-in function to list files.
The name starts with a `_` character because you are not supposed to call it directly.
To ease the use, there is a function called `glob` in the Turicum system library.
You can import it from the `+"turi.io"+` package.

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

end snippet */

/**
 * This function is used to list file names in a directory, possibly using recursion.
 * <p>
 * The function is named {@code _glob()} with an underscore at the start because it is
 * not supposed to be called directly. Source code should instead import the function {@code glob}
 * from the {@code turi.io} module.
 * <pre>{@code
 * // import the 'glob' function from the io module
 * sys_import "turi.io", "glob"
 *
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
@Name("_glob")
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
        final var result = new LngList();
        try {
            result.addAll(ch.turic.utils.Glob.glob(pattern, recursive));
        } catch (Exception e) {
            throw new ExecutionException(e, "Error, while globbing file names in glob(): '" + e.getMessage() + "'");
        }
        return result;
    }
}
