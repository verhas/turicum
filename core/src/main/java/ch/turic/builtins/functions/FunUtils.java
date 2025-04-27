package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;

public class FunUtils {

    public static void needArg(final String name, Object[] args) {
        ExecutionException.when(args.length == 0, "Function %s needs at least one argument.", name);
    }

    public static void noArg(final String name, Object[] args) {
        ExecutionException.when(args.length > 1, "Built-in function %s needs no arguments", name);
    }

    public static void oneArgOpt(final String name, Object[] args) {
        ExecutionException.when(args.length > 1, "Built-in function %s needs at most one argument", name);
    }

    public static Object oneArg(final String name, Object[] args) {
        ExecutionException.when(args.length != 1, "Built-in function '%s' needs exactly one argument", name);
        return args[0];
    }

    public static void twoArgs(final String name, Object[] args) {
        ExecutionException.when(args.length != 2, "Function %s needs two arguments.", name);
    }

    public static void nArgs(final String name, Object[] args, int n) {
        ExecutionException.when(args.length != n, "Function %s needs %d argument.", name, n);
    }

    public static ch.turic.memory.Context ctx(Context context) {
        if (context instanceof ch.turic.memory.Context ctx) {
            return ctx;
        }
        throw new ExecutionException("context must be a context of type ch.turic.memory.Context");
    }

}
