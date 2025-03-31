package javax0.turicum;

import java.util.List;

public non-sealed interface TuriFunction extends LngCallable, ServiceLoaded {

    static List<TuriFunction> getInstances() {
        return ServiceLoaded.getInstances(TuriFunction.class);
    }

    /**
     * @return the name of the function used to register in the heap (global space)
     */
    String name();

}
