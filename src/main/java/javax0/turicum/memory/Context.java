package javax0.turicum.memory;


import javax0.turicum.ExecutionException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep a context of the current threads executing environment.
 */
public class Context implements javax0.turicum.Context {
    public final int stepLimit;
    public final AtomicInteger steps;
    protected final Map<String, Object> heap;
    final Map<String, Object> frame;
    private final Set<String> globals = new HashSet<>();
    private final Set<String> frozen = new HashSet<>();
    private final Context wrapped;
    public final GlobalContext globalContext;
    public final ThreadContext threadContext;

    /**
     * Freeze a local variable adding it to the frozen names. It means that it cannot be changed anymore after this
     * point.
     *
     * @param identifier the identifier to freeze
     */
    public void freeze(String identifier) {
        ExecutionException.when(frozen.contains(identifier), "variable is already pinned '" + identifier + "'");
        frozen.add(identifier);
    }

    public void local(String local, Object value) throws ExecutionException {
        ExecutionException.when(globals.contains(local), "Local variable is already defined as global '" + local + "'");
        ExecutionException.when(frozen.contains(local), "final variable cannot be altered '" + local + "'");
        frame.put(local, value);
    }

    public void global(String global) throws ExecutionException {
        ExecutionException.when(frame.containsKey(global), "Global variable is already defined as local '" + global + "'");
        ExecutionException.when(globals.contains(global), "Global variable is already defined '" + global + "'");
        globals.add(global);
        heap.get(global);
    }

    public void global(String global, Object value) throws ExecutionException {
        ExecutionException.when(frame.containsKey(global), "Global variable is already defined as local '" + global + "'");
        ExecutionException.when(globals.contains(global), "Global variable is already defined '" + global + "'");
        globals.add(global);
        heap.put(global, value);
    }

    /**
     * Decide if the identifier refers to a global variable.
     *
     * @param identifier the identifier we are looking for.
     * @return {@code true} iff the identifier was declared global in this or any transitively wrapped context
     */
    private boolean isGlobal(final String identifier) {
        if (globals.contains(identifier)) {
            return true;
        } else if (wrapped == null) {
            return false;
        } else {
            return wrapped.isGlobal(identifier);
        }
    }

    /**
     * Open a new stack frame and return that context with the new stack frame.
     * Opening a new frame does not reference the current frame as a parent.
     * This is used when we execute a call in a new independent scope, like calling a method or function.
     * The code in the function does not have access to the caller.
     *
     * @return the new context. New stack frame, no wrapped.
     */
    public Context open() {
        return new Context(this, null);
    }

    /**
     * Wrap the context into a new context. The old context will serve as the parent context.
     * <p>
     * This is used when we execute context in a scope that is an inherent part of the surrounding scope.
     * For example, when a block is executed. The block can see the context of the surrounding scope, so the surrounding
     * scope is wrapped into the current scope.
     *
     * @return the new context. New stack frame, wrapped context.
     */
    public Context wrap() {
        return new Context(this, this);
    }

    /**
     * Create a new context and wrap the argument context into the new context.
     * This is the situation when we call a closure from somewhere. The context of the closure will not wrap the
     * current context, rather it will wrap the context in which it was created.
     *
     * @param wrapped teh wrapped context, probably stored in a closure and queried from there
     * @return the new context
     */
    public Context wrap(Context wrapped) {
        return new Context(this, wrapped);
    }

    /**
     * Create a new context with 100_0000 as a step limit
     */
    public Context() {
        this(new GlobalContext(), new ThreadContext(), -1);
    }

    /**
     * Create a new top-level context with the given step limit. Since it is top-level, the frame is the same as the
     * heap.
     *
     * @param stepLimit the number of steps the interpreter will execute in this context.
     */
    public Context(final GlobalContext globalContext, final ThreadContext threadContext, final int stepLimit) {
        this.stepLimit = stepLimit;
        this.steps = new AtomicInteger();
        this.wrapped = null;
        this.heap = new HashMap<>();
        this.frame = heap;
        this.globalContext = globalContext;
        this.threadContext = threadContext;
    }

    public Context(Context heapContext, final GlobalContext globalContext, final ThreadContext threadContext, final int stepLimit) {
        this.heap = heapContext.heap;
        this.globalContext = globalContext;
        this.threadContext = threadContext;
        this.stepLimit = stepLimit;
        this.steps = new AtomicInteger();
        this.wrapped = null;
        this.frame = new HashMap<>();
    }

    /**
     * Clone the context creating a new stack frame. The new context inherits the heap and the step limit and the
     * already consumed steps' counter.
     * <p>
     * The new context has its own heap.
     * <p>
     * The set of globals is also inherited (copied). If a global declaration is used in a context it will not affect
     * the surrounding context.
     *
     * @param clone   the current context when starting a new frame
     * @param wrapped is the context wrapped by this context
     */
    private Context(final Context clone, final Context wrapped) {
        this.stepLimit = clone.stepLimit;
        this.steps = clone.steps;
        this.heap = clone.heap;
        this.globalContext = clone.globalContext;
        this.threadContext = clone.threadContext;
        this.frame = new HashMap<>();
        this.wrapped = wrapped;
    }

    /**
     * Redefine the value of a variable in the frame of the context or in the wrapped context transitively.
     * <p>
     * If it is not found anywhere, then just return false.
     * <p>
     * Note that a variable is also found even if it has the undefined value.
     *
     * @param key   the name of the variable.
     * @param value the new value for the variable if it already exists.
     * @return {@code true} if a variable was found and redefined, and {@code false} is the variable was not found.
     * {@code false} if a variable was not found ior was found frozen (in which case like we did not find it)
     */
    private boolean redefine(final String key, final Object value) {
        if (frame.containsKey(key)) {
            if (frozen.contains(key)) {
                return false;
            }
            frame.put(key, value);
            return true;
        }
        return wrapped != null && wrapped.redefine(key, value);
    }

    /**
     * Assign a new value to an identifier. If the identifier is already defined in the wrapper context, including the
     * transitive closures of the wrappers, then redefine that. Otherwise, create a new local definition.
     *
     * @param key   the identifier
     * @param value the value of the local whatnot
     */
    public void let(final String key, final Object value) {
        if (isGlobal(key)) {
            heap.put(key, value);
        }
        if (!redefine(key, value)) {
            ExecutionException.when(frozen.contains(key), "Redefining final variable '" + key + "'");
            let0(key, value);
        }
    }

    /**
     * Assing a value to the local symbol key in the current context does not matter if it is defined there or
     * in the wrapped context global.
     * It is a primitive call used in for each loop.
     *
     * @param key   the loop identifier
     * @param value the value in the loop
     */
    public void let0(final String key, final Object value) {
        frame.put(key, value);
    }

    /**
     * Define a variable in the surrounding environment. For example, when a variable is assigned in a code block,
     * like {@code a := 13;} then the variable {@code a} will be defined or redefined in the surrounding environment.
     * It is an error to use this method from a context that is not wrapped. For example, you should not use this
     * assignment from the code in a method or top-level code and not inside a closure or code block.
     * <p>
     * If the wrapped context is also wrapped, then the method finds the top-level wrapped context, but NOT the heap.
     *
     * @param key   the identifier
     * @param value the value of the local whatnot
     * @throws ExecutionException if the context does not wrap anything
     */
    public void exportLet(final String key, final Object value) throws ExecutionException {
        ExecutionException.when(isGlobal(key), "You cannot := a variable defined as global '" + key + "'");
        ExecutionException.when(this.wrapped == null, "You cannot := from a no-wrapped environment");

        var ctx = this;
        while (ctx.wrapped != null) {
            ctx = ctx.wrapped;
        }

        ctx.frame.put(key, value);
    }

    /**
     * Retrieve the object associated with the key. It is either on the local stack or a global whatever.
     *
     * @param key the identifier.
     * @return the object or null if not defined
     */
    public Object get(String key) {
        if (frame.containsKey(key)) {
            return frame.get(key);
        }
        if (wrapped != null && wrapped.contains(key)) {
            return wrapped.get(key);
        }
        return heap.get(key);
    }

    public boolean contains(String key) {
        if (frame.containsKey(key)) {
            return true;
        }
        if (wrapped != null && wrapped.contains(key)) {
            return true;
        }
        return heap.containsKey(key);
    }

    /**
     * Get the value of a local variable from this field or from any of the wrapped context, but not a global variable.
     *
     * @param key the name of the variable
     * @return the value of the variable
     */
    public Object getLocal(String key) {
        var ctx = this;
        while (ctx != null) {
            if (ctx.frame.containsKey(key)) {
                return ctx.frame.get(key);
            }
            ctx = ctx.wrapped;
        }
        return null;
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
