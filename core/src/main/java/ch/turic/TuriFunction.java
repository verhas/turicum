package ch.turic;

import java.util.List;

public interface TuriFunction extends LngCallable.LngCallableClosure, ServiceLoaded {

    static List<TuriFunction> getInstances() {
        return ServiceLoaded.getInstances(TuriFunction.class);
    }

    /**
     * @return the name of the function used to register in the heap (global space)
     */
    String name();

}
