package ch.turic;

import ch.turic.memory.Sentinel;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.InfiniteValue;

/**
 * The BuiltIns class manages registration of built-in constants, functions, macros and classes
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
     * Registers all built-in elements (constants, functions, macros and classes) into the given context.
     *
     * @param context The context where built-ins should be registered
     * @throws IllegalArgumentException if the provided context is not an instance of ch.turic.memory.Context
     */
    public static void register(Context context) {
        if (context instanceof ch.turic.memory.Context ctx) {
            registerGlobalConstants(ctx);
            registerGlobalFunctionsAndMacros(ctx);
            registerTuriClasses(ctx.globalContext);
        } else {
            throw new IllegalArgumentException("Expected ch.turic.memory.Context but got " + context.getClass());
        }
    }

    /**
     * Registers all available Turi classes into the global context.
     *
     * @param globalContext The global context where classes should be registered
     */
    private static void registerTuriClasses(GlobalContext globalContext) {
        TuriClass.getInstances().forEach(turiClass -> globalContext.addTuriClass(turiClass.forClass(), turiClass));
    }

    /**
     * Registers all built-in functions and macros into the given context.
     *
     * @param context The context where functions and macros should be registered
     */
    private static void registerGlobalFunctionsAndMacros(ch.turic.memory.Context context) {
        TuriFunction.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
        TuriMacro.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
    }

    /**
     * Registers all built-in constants defined in the values array to the given context
     * and freezes them to prevent modification.
     *
     * @param context The context where constants should be registered
     */
    static void registerGlobalConstants(ch.turic.memory.Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
