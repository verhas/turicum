package javax0.turicum.memory;

import javax0.turicum.TuriClass;
import javax0.turicum.ExecutionException;

import java.util.HashMap;
import java.util.Map;

/**
 * A special context holding constant values, like built-ins, one for the interpreter
 */
public class GlobalContext {

    private final Map<Class<?>, TuriClass> turiClasses = new HashMap<>();

    public TuriClass getTuriClass(Class<?> clazz) {
        return turiClasses.get(clazz);
    }

    public void addTuriClass(Class<?> clazz, TuriClass turiClass) throws ExecutionException {
        ExecutionException.when(turiClasses.containsKey(clazz), "Class " + clazz.getName() + " already exists as a turi class.");
        turiClasses.put(clazz, turiClass);
    }

}
