package ch.turic;

import ch.turic.memory.Sentinel;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.InfiniteValue;

public class BuiltIns {

    private final static Object[] values = new Object[]{
            // snippet builtins
            "true", true,
            // has the constant boolean __true__ value.
            "false", false,
            // has the constant boolean __false__ value.

            "none", null,
            // is the undefined value.
            // The Java representation of the undefined value is `null`.

            "inf", InfiniteValue.INF_POSITIVE,
            // is the infinite numeric value.

            "fini", Sentinel.FINI,
            // is the special value that, when returned from a cell command, prevents the cell from updating,
            // but it also signals that this cell is stopped, should not be evaluated any further

            "non_mutat", Sentinel.NON_MUTAT,
            // is the special value that, when returned from a cell command, prevents the cell from updating
            // its value and halts propagation to dependent cells in a flow.

            "pi", Math.PI,
            // is the mathematical constant π (approximately 3.14159).

            "e", Math.E,
            // is the base of the natural logarithm (Euler’s number, approximately 2.71828).

            "nan", Double.NaN,
            // is the special IEEE-754 "not a number" floating-point value.

            // end snippet
    };

    public static void register(Context context) {
        if (context instanceof ch.turic.memory.Context ctx) {
            registerGlobalConstants(ctx);
            registerGlobalFunctionsAndMacros(ctx);
            registerTuriClasses(ctx.globalContext);
        } else {
            throw new IllegalArgumentException("Expected ch.turic.memory.Context but got " + context.getClass());
        }
    }

    private static void registerTuriClasses(GlobalContext globalContext) {
        TuriClass.getInstances().forEach(turiClass -> globalContext.addTuriClass(turiClass.forClass(), turiClass));
    }

    private static void registerGlobalFunctionsAndMacros(ch.turic.memory.Context context) {
        TuriFunction.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
        TuriMacro.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
    }

    static void registerGlobalConstants(ch.turic.memory.Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
