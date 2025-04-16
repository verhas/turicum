package ch.turic;

import ch.turic.memory.GlobalContext;
import ch.turic.memory.InfiniteValue;

public class BuiltIns {

    private final static Object[] values = new Object[]{
            // snippet builtins
            "true", true,
            // has the constant boolean __true__ value.
            "false", false,
            // has the constant boolean __false__ value
            "none", null,
            // is the undefined value.
            // The Java representation of the undefined value is `null`.
            "inf", InfiniteValue.INF_POSITIVE
            // is the infinite value.
            // end snippet
    };

    public static void register(Context context) {
        if( context instanceof ch.turic.memory.Context ctx){
            registerGlobalConstants(ctx);
            registerGlobalFunctionsAndMacros(ctx);
            registerTuriClasses(ctx.globalContext);
        }else{
            throw new RuntimeException("wtf");
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
