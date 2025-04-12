package ch.turic;

import ch.turic.memory.GlobalContext;
import ch.turic.memory.InfiniteValue;

public class BuiltIns {

    private final static Object[] values = new Object[]{
            // snippet builtins
            "true", true,
            "false", false,
            "none", null,
            "inf", InfiniteValue.INF_POSITIVE
            // end snippet
    };

    public static void register(Context context) {
        if( context instanceof ch.turic.memory.Context ctx){
            registerGlobalConstants(ctx);
            registerGlobalFunctions(ctx);
            registerTuriClasses(ctx.globalContext);
        }else{
            throw new RuntimeException("wtf");
        }
    }

    private static void registerTuriClasses(GlobalContext globalContext) {
        TuriClass.getInstances().forEach(turiClass -> globalContext.addTuriClass(turiClass.forClass(), turiClass));
    }

    private static void registerGlobalFunctions(ch.turic.memory.Context context) {
        TuriFunction.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
    }

    static void registerGlobalConstants(ch.turic.memory.Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
