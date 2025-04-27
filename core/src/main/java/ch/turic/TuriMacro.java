package ch.turic;

import java.util.List;

public interface TuriMacro extends LngCallable.LngCallableMacro, ServiceLoaded {

    static List<TuriMacro> getInstances() {
        return ServiceLoaded.getInstances(TuriMacro.class);
    }

    /**
     * @return the name of the function used to register in the heap (global space)
     */
    String name();

}
