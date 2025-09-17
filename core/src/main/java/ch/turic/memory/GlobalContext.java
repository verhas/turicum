package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.TuriClass;
import ch.turic.memory.debugger.DebuggerContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special context holding a constant string, like built-ins, one for the interpreter.
 * <p>
 * This global context is shared for the whole interpreter for all the threads.
 */
public class GlobalContext {
    public VarTable heap = new VarTable();
    public final int stepLimit;
    public final AtomicInteger steps = new AtomicInteger();
    private final Map<Class<?>, TuriClass> turiClasses = new HashMap<>();
    Path sourcePath;
    private boolean debugMode = false; // true when the interpreter is in debug mode
    final DebuggerContext debuggerContext = new DebuggerContext(null,null);
    private final List<LocalContext> contexts = new ArrayList<>();

    public GlobalContext(int stepLimit) {
        this.stepLimit = stepLimit;
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

    /**
     * Adds a mapping between a Java {@code Class} and a corresponding {@code TuriClass} instance.
     * If the class is already registered as a Turi class, an {@code ExecutionException} is thrown.
     *
     * @param clazz     the Java {@code Class} to be associated with the given {@code TuriClass}
     * @param turiClass the {@code TuriClass} instance to be associated with the Java {@code Class}
     * @throws ExecutionException if the provided class is already registered as a Turi class
     */
    public void addTuriClass(Class<?> clazz, TuriClass turiClass) throws ExecutionException {
        ExecutionException.when(turiClasses.containsKey(clazz), "Class " + clazz.getName() + " already exists as a turi class.");
        turiClasses.put(clazz, turiClass);
    }

    /**
     * Retrieves the current state of the debug mode.
     *
     * @return {@code true} if the debug mode is enabled, {@code false} otherwise
     */
    public boolean debugMode() {
        return debugMode;
    }

    /**
     * Updates the debug mode state of the interpreter and retrieves the previous state.
     *
     * @param debugMode the new state of the debug mode to set
     * @return the current state of the debug mode before the update
     */
    public boolean debugMode(boolean debugMode) {
        try {
            return this.debugMode;
        } finally {
            this.debugMode = debugMode;
        }
    }

    /**
     * Executes a single computational step based on the defined step limit.
     * This method ensures that the number of executed steps does not exceed the configured step limit.
     * <p>
     * - If the step limit is negative, the method returns immediately without performing any action.
     * - If the current number of steps has reached or exceeded the step limit, an {@code ExecutionException} is thrown.
     * - Otherwise, the step counter is incremented by one.
     *
     * @throws ExecutionException if the step limit is reached or exceeded
     */
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
     * Registers the given context into the global context registry.
     * Ensures thread-safe addition of the context to the internal collection.
     *
     * @param context the context to be registered
     */
    public void registerContext(LocalContext context) {
        synchronized (contexts) {
            contexts.add(context);
        }
    }

    /**
     * Removes the specified context from the global context registry.
     * Ensures thread-safe removal of the context from the internal collection.
     *
     * @param context the context to be removed
     */
    public void removeContext(LocalContext context) {
        synchronized (contexts) {
            if( !contexts.remove(context) ){
                throw new RuntimeException("Context of a thread was not found in registry");
            }
        }
    }

    /**
     * Switch the global heap to multithreaded mode, converting the heap from a normal hash map to a concurrent hash
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
            heap.parallel();
    }

}
