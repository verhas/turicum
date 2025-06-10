package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;

public class Glob implements TuriFunction {
    @Override
    public String name() {
        return "_glob";
    }

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
            throw new ExecutionException(e,"Error, while globbing file names in glob(): '" + e.getMessage() + "'");
        }
        return result;
    }
}
