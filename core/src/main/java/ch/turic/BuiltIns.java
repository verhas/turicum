package ch.turic;

import ch.turic.builtins.functions.FunUtils;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.InfiniteValue;
import ch.turic.memory.LocalContext;
import ch.turic.memory.Sentinel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The BuiltIns class manages registration of built-in constants, functions, macros, and classes
 * that are available globally in the Turi language runtime.
 */
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

            "nan", Double.NaN,
            // is the special IEEE-754 "not a number" floating-point value.

            // end snippet
    };

    /**
     * Registers all built-in elements (constants, functions, macros, and classes) into the given context.
     *
     * @param context The context where built-ins should be registered
     * @throws IllegalArgumentException if the provided context is not an instance of ch.turic.memory.Context
     */
    public static void register(Context context) {
        final var ctx = FunUtils.ctx(context);
        registerGlobalFunctionsAndMacros(ctx);
        registerTuriClasses(ctx.globalContext);
    }

    /**
     * Registers all available Turi classes into the global context.
     *
     * @param globalContext The global context where classes should be registered
     */
    private static void registerTuriClasses(GlobalContext globalContext) {
        TuriClass.getInstances(globalContext.classLoader).forEach(turiClass -> globalContext.addTuriClass(turiClass.forClass(), turiClass));
    }

    /**
     * Registers all built-in functions and macros into the given context.
     *
     * @param context The context where functions and macros should be registered
     */
    private static void registerGlobalFunctionsAndMacros(LocalContext context) {
        TuriFunction.getInstances(context.globalContext.classLoader).forEach(
                tf -> context.predefine(tf.name(), tf));
        TuriMacro.getInstances(context.globalContext.classLoader).forEach(
                tm -> context.predefine(tm.name(), tm));
    }



    /**
     * Registers all built-in constants defined in the values array to the given context
     * and freezes them to prevent modification.
     *
     * @param context The context where constants should be registered
     */
    public static void registerGlobalConstants(LocalContext context) {
        for (int i = 0; i < values.length; i += 2) {
            context.predefine((String) values[i], values[i + 1]);
        }
    }

}
