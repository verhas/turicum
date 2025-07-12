package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.TuriClass;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special context holding a constant string, like built-ins, one for the interpreter.
 * <p>
 * This global context is shared for the whole interpreter for all the threads.
 */
public class GlobalContext {
    public Map<String, Variable> heap = new HashMap<>();
    private final Context top;
    public final AtomicBoolean isMultiThreading = new AtomicBoolean(false);
    public final int stepLimit;
    public final AtomicInteger steps = new AtomicInteger();
    private final Map<Class<?>, TuriClass> turiClasses = new HashMap<>();
    Path sourcePath;

    public GlobalContext(int stepLimit, Context top) {
        this.stepLimit = stepLimit;
        this.top = top;
    }

    /**
     * Retrieves the {@code TuriClass} object associated with the given class. If the exact class is not found
     * in the internal mapping, it attempts to find the superclass or implemented interface
     * that matches the given class.
     * <p>
     * If there are multiple interfaces or classes matching, it finds one randomly.
     *
     * @param clazz the class for which the corresponding {@code TuriClass} object is to be retrieved
     * @return the {@code TuriClass} associated with the given class, or {@code null} if no match is found
     */
    public TuriClass getTuriClass(Class<?> clazz) {
        if (turiClasses.containsKey(clazz)) {
            return turiClasses.get(clazz);
        }
        for (final var turiclass : turiClasses.keySet()) {
            if (turiclass.isAssignableFrom(clazz)) {
                return turiClasses.get(turiclass);
            }
        }
        return null;
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

    /**
     * Switch the global heap to multithreaded mode converting the heap from a normal hash map to a concurrent hash
     * map.
     * <p>
     * This method does not need synchronization. When it is called first time, there are no multiple threads.
     * The single main thread that is about to start other threads will wait till the heap is replaced.
     * <p>
     * On subsequent calls, when there are already multiple threads, the replacement does ot happen. It is already done.
     * <p>
     * The boolean flag needs to be atomic, so a second thread does not think she is the only one running and start
     * the replacement of the heap map again.
     * <p>
     * The heap replacement to {@link ConcurrentHashMap} does not guarantee that the variables are also updated.
     */
    public void switchToMultithreading() {
        if (isMultiThreading.compareAndSet(false, true)) {
            heap = new ConcurrentHashMap<>(heap);
            top.frame = heap;
        }
    }

}
