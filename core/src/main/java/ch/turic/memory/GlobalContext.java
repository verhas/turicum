package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.TuriClass;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special context holding constant string, like built-ins, one for the interpreter.
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

    public GlobalContext(int stepLimit, Context top) {
        this.stepLimit = stepLimit;
        this.top = top;
    }

    /**
     * Get the TuriClass object that can handle the method calls for this class type.
     * <p>
     * A TuriClass object can handle a type if the type returned by the objects {@link TuriClass#forClass()} method
     * is {@code clazz}.
     *
     * @param clazz the class of the object we want to handle
     * @return the {@link TuriClass} object or {@code null} if there is no registered object that could handle this
     * type.
     */
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
