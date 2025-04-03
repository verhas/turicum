package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.TuriClass;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special context holding constant string, like built-ins, one for the interpreter.
 * <p>
 * This global context is shared for the whole interpreter for all the threads.
 */
public class GlobalContext {
    public final Map<String, Object> heap = new HashMap<>();
    public final int stepLimit;
    public final AtomicInteger steps = new AtomicInteger();
    private final Map<Class<?>, TuriClass> turiClasses = new HashMap<>();

    public GlobalContext(int stepLimit) {
        this.stepLimit = stepLimit;
    }

    public TuriClass getTuriClass(Class<?> clazz) {
        return turiClasses.get(clazz);
    }

    public void addTuriClass(Class<?> clazz, TuriClass turiClass) throws ExecutionException {
        ExecutionException.when(turiClasses.containsKey(clazz), "Class " + clazz.getName() + " already exists as a turi class.");
        turiClasses.put(clazz, turiClass);
    }

    public void step() throws ExecutionException {
        if (stepLimit < 0) {
            return;
        }
        if (stepLimit <= steps.get()) {
            throw new ExecutionException("Step limit %d reached", stepLimit);
        }
        steps.incrementAndGet();
    }

}
