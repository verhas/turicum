package javax0.turicum;

import javax0.turicum.memory.GlobalContext;
import javax0.turicum.memory.InfiniteValue;

class BuiltIns {

    private final static Object[] values = new Object[]{
            "true", true,
            "false", false,
            "none", null,
            "inf", InfiniteValue.INF_POSITIVE
    };

    public static void register(Context context) {
        if( context instanceof javax0.turicum.memory.Context ctx){
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

    private static void registerGlobalFunctions(javax0.turicum.memory.Context context) {
        TuriFunction.getInstances().forEach(
                jif -> context.global(jif.name(), jif));
    }

    static void registerGlobalConstants(javax0.turicum.memory.Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
