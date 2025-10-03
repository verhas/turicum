package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;

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
