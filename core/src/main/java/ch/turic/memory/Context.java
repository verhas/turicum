package ch.turic.memory;


import ch.turic.ExecutionException;

import java.util.*;

/**
 * Keep a context of the current threads executing environment.
 */
public class Context implements ch.turic.Context {
    Map<String, Variable> frame;
    private final Set<String> globals = new HashSet<>();
    private final Set<String> nonlocal = new HashSet<>();
    private final Set<String> frozen = new HashSet<>();
    private final Context wrapped;
    public final GlobalContext globalContext;
    public final ThreadContext threadContext;
    public Context caller = null;
    private final List<String> exporting = new ArrayList<>();
    private final boolean shadow;

    private boolean pinned = false;

    public Set<String> keys() {
        return frame.keySet();
    }

    /**
     * Retrieves a set containing all keys present in the current context.
     * This includes both local keys and global keys. The method combines the keys
     * from the local context and the global context, ensuring no duplicates.
     * <p>
     * Since this returns the set of only the keys it is meaningless to ask which is returned in case of name collision.
     * It is the name only.
     *
     * @return a set of all keys available in the current context, including local and global keys
     */
    public Set<String> allKeys() {
        final var keySet = new HashSet<String>();
        keySet.addAll(allLocalKeys());
        keySet.addAll(globalContext.heap.keySet());
        return keySet;
    }

    /**
     * Retrieves all local keys from the current context and all wrapped contexts.
     * This method traverses through the chain of wrapped contexts, collecting the keys
     * from each context's local frame.
     *
     * @return a set of all local keys present in the current context and its wrapped contexts
     */
    public Set<String> allLocalKeys() {
        final var keySet = new HashSet<String>();
        var ctx = this;
        while (ctx != null) {
            keySet.addAll(ctx.frame.keySet());
            ctx = ctx.wrapped;
        }
        return keySet;
    }

    /**
     * Retrieves a set containing all keys from the local frame of the current context.
     * This method provides access solely to the keys from the local frame, without including
     * keys from any global or wrapped contexts.
     *
     * @return a set of all keys present in the local frame of the current context
     */
    public Set<String> allFrameKeys() {
        return new HashSet<>(frame.keySet());
    }

    /**
     * Create a new context with -1 as a step limit, a.k.a. unlimited, for example, a server application.
     */
    public Context() {
        this(-1);
    }

    /**
     * Create a new context with limited steps
     *
     * @param stepLimit the number of maximal steps before killing the interpreter
     */
    public Context(int stepLimit) {
        this.globalContext = new GlobalContext(stepLimit, this);
        this.wrapped = null;
        this.frame = globalContext.heap;
        this.threadContext = new ThreadContext();
        this.shadow = false;
    }

    public Context(final GlobalContext globalContext, final ThreadContext threadContext) {
        this.wrapped = null;
        this.frame = new HashMap<>();
        this.globalContext = globalContext;
        this.threadContext = threadContext;
        this.shadow = false;
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
        this.globalContext = clone.globalContext;
        this.threadContext = clone.threadContext;
        this.frame = new HashMap<>();
        this.wrapped = wrapped;
        this.shadow = false;
    }

    private Context(final Context thisContext, final Context wrappedContext, final boolean shadow) {
        this.globalContext = thisContext.globalContext;
        this.threadContext = thisContext.threadContext;
        this.frame = new HashMap<>();
        this.wrapped = wrappedContext;
        this.shadow = shadow;

    }

    private Context(final Context thisContext, final Context wrappedContext, final Context withContext) {
        this.globalContext = thisContext.globalContext;
        this.threadContext = thisContext.threadContext;
        this.frame = withContext.frame;
        this.wrapped = wrappedContext;
        this.shadow = false;
    }

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

    /**
     * Define a variable with the types 'typeNames'.
     *
     * @param key       the name of the variable
     * @param value     the value of the new variable
     * @param typeNames the names of the accepted types
     */
    public void define(String key, Object value, String[] typeNames) {
        final var v = createVariable(key, typeNames);
        v.value = value;
        frame.put(key, v);
    }

    /**
     * Same as {@link #define(String, Object, String[]) define()} but it throws exception if the type does not fit the
     * value.
     *
     * @param key       the name of the variable
     * @param value     the value of the new variable
     * @param typeNames the names of the accepted types
     */
    public void defineTypeChecked(String key, Object value, String[] typeNames) {
        final var v = createVariable(key, typeNames);
        v.set(value);
    }

    /**
     * Create a new variable. Also check that the name is not global, not non-local and not frozen.
     * <p>
     * The variable is also added to the local frame with the name, but it does not have value (it has null).
     *
     * @param key       the name of the variable
     * @param typeNames the types for the variable
     * @return the newly created variable object
     */
    private Variable createVariable(String key, String[] typeNames) {
        ExecutionException.when(globals.contains(key), "Local variable is already defined as global '" + key + "'");
        ExecutionException.when(nonlocal.contains(key), "Variable cannot be local, it is already used as non-local '" + key + "'");
        ExecutionException.when(frozen.contains(key), "final variable cannot be altered '" + key + "'");
        // we are lenient when we have a "let" inside a loop, as it will be executed multiple times
        if (frame.containsKey(key)) {
            throw new ExecutionException("Variable '%s' is already defined.", key);
        }
        if (shadow) {
            var ctx = this;
            while (ctx != null && ctx.shadow) {
                ctx = ctx.wrapped;
                if (ctx != null && ctx.frame.containsKey(key)) {
                    throw new ExecutionException("Variable '%s' is already defined.", key);
                }
            }
        }
        final var v = new Variable(key);
        v.types = Variable.getTypes(this, typeNames);
        frame.put(key, v);
        return v;
    }

    /**
     * Remove a variable from the local context.
     *
     * @param key the name of the variable
     * @throws ExecutionException if the variable was not defined in the local context.
     */
    public void unlet(String key) throws ExecutionException {
        ExecutionException.when(!frame.containsKey(key), "Variable '%s' is not defined in the local context you cannot unlet '%s'", key);
        frame.remove(key);
    }

    public void local(String key, Object value) throws ExecutionException {
        ExecutionException.when(globals.contains(key), "Local variable is already defined as global '" + key + "'");
        ExecutionException.when(nonlocal.contains(key), "Variable cannot be local, it is already used as non-local '" + key + "'");
        ExecutionException.when(frozen.contains(key), "pinned variable cannot be altered '" + key + "'");
        frame.computeIfAbsent(key, x -> new Variable(key)).set(value);
    }

    public void global(String global) throws ExecutionException {
        ExecutionException.when(frame != globalContext.heap && frame.containsKey(global), "Global variable '%s' is already defined as local.", global);
        globals.add(global);
    }

    public void global(String global, Object value) throws ExecutionException {
        global(global);
        globalContext.heap.computeIfAbsent(global, Variable::new).set(value);
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
     * Create a new context that will have the {@code other} context frame.
     * It is used with the {@code with} command.
     *
     * @param other the other context from which we will use the frame in the new one
     * @return the nex context
     */
    public Context with(Context other) {
        return new Context(this, this, other);
    }

    public Context thread() {
        return new Context(globalContext, new ThreadContext());
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

    public Context shadow() {
        return new Context(this, this, true);
    }

    /**
     * Create a new context and wrap the argument context into the new context.
     * This is the situation when we call a closure from somewhere. The context of the closure will not wrap the
     * current context, rather it will wrap the context in which it was created.
     *
     * @param wrapped the wrapped context, probably stored in a closure and queried from there
     * @return the new context
     */
    public Context wrap(Context wrapped) {
        return new Context(this, wrapped);
    }

    /**
     * Assign a new value to an identifier. If the identifier is already defined in the wrapper context, including the
     * transitive closures of the wrappers, then redefine that.
     * <p>
     * It is an error if the variable is not defined.
     *
     * @param key   the identifier
     * @param value the value of the local whatnot
     */
    public void update(final String key, final Object value) {
        if (globals.contains(key)) {
            // when we set a global value, it does not matter if it is already defined because it is declared or
            // was already declared as 'global'
            globalContext.heap.computeIfAbsent(key, Variable::new).set(value);
            return;
        }

        // TODO it will not work for class context
        for (var ctx = this; ctx != null; ctx = ctx.wrapped) {
            if (ctx.frozen.contains(key)) {
                throw new ExecutionException("Variable '%s' is pinned.", key);
            }
            if (ctx.frame.containsKey(key)) {
                if (ctx.pinned) {
                    throw new ExecutionException("Variable '%s' is in a pinned context.", key);
                }
                if (ctx != this) {
                    nonlocal.add(key);
                }
                ctx.frame.computeIfAbsent(key, Variable::new).set(value);
                return;
            }
        }
        ExecutionException.when(nonlocal.contains(key), "Variable '%s' was used as global, but is not declared, cannot be changed.", key);
        throw new ExecutionException("Variable '%s' is not defined.", key);
    }

    public class ContextLock implements AutoCloseable {

        @Override
        public void close() {
            Context.this.pinned = false;
        }
    }

    /**
     * Locks the current context by setting its `pinned` state to true and returns a {@code ContextLock} instance.
     * The returned {@code ContextLock} ensures the context remains locked until the {@code close} method of the lock
     * is called.
     * <p>
     * The recommended usage is with try-with-resources statement to ensure the context is automatically unlocked:
     *
     * <pre>
     * try (var lock = context.lock()) {
     *     // context is locked here
     *     // perform operations that require locking
     * } // context is automatically unlocked when exiting try block
     * </pre>
     *
     * @return a new {@code ContextLock} instance that manages the lock state of the current context
     */
    public ContextLock lock() {
        pinned = true;
        return new ContextLock();
    }

    /**
     * Assing a value to the local symbol key in the current context does not matter if it is defined there or
     * in the wrapped context or global or frozen.
     * <p>
     * It is a primitive call used in for each loop or setting 'this', 'cls' or the exception variable in 'catch'.
     * Generally it is called from locations where it is guaranteed that the variable can be declared as local and
     * it was NOT defined in the context before.
     * (Hence ending 0 at the end of the name, that many times denotes internal or native methods in the JDK).
     *
     * @param key   the loop identifier
     * @param value the value in the loop
     */
    public void let0(final String key, final Object value) {
        frame.computeIfAbsent(key, Variable::new).set(value);
    }

    /**
     * Retrieve the object associated with the key. It is either on the local stack or a global whatever.
     *
     * @param key the identifier.
     * @return the object or null if not defined
     */
    public Object get(String key) {
        if (globals.contains(key)) {
            final var variable = globalContext.heap.get(key);
            if (variable == null) {
                return null;
            }
            return variable.get();
        }
        for (var ctx = this; ctx != null; ctx = ctx.wrapped) {
            if (ctx.frame.containsKey(key)) {
                if (ctx != this) {
                    nonlocal.add(key);
                }
                return ctx.frame.get(key).get();
            }
        }
        if (globalContext.heap.containsKey(key)) {
            nonlocal.add(key);
            return globalContext.heap.get(key).get();
        }
        throw new ExecutionException("Variable '%s' is undefined.", key);
    }

    public boolean contains(String key) {
        if (frame.containsKey(key)) {
            return true;
        }
        if (wrapped != null && wrapped.contains(key)) {
            return true;
        }
        return globalContext.heap.containsKey(key);
    }

    public boolean contains0(String key) {
        return frame.containsKey(key);
    }

    /**
     * Checks whether the specified key exists in the local frame or in any wrapped context.
     *
     * @param key the name of the key to check for presence within the local frame or wrapped contexts
     * @return true if the key is found in the local frame or any wrapped context, false otherwise
     */
    public boolean containsLocal(String key) {
        if (frame.containsKey(key)) {
            return true;
        }
        return wrapped != null && wrapped.containsLocal(key);
    }

    /**
     * Checks if the specified key exists within the local frame of the current context.
     *
     * @param key the name of the key to check within the local frame
     * @return true if the key exists in the local frame, false otherwise
     */
    public boolean containsFrame(String key) {
        return frame.containsKey(key);
    }

    /**
     * Get the value of a local variable from this field or from any of the wrapped context, but not a global variable.
     *
     * @param key the name of the variable
     * @return the value of the variable
     */
    public Object getLocal(String key) {
        for (var ctx = this; ctx != null; ctx = ctx.wrapped) {
            if (ctx.frame.containsKey(key)) {
                return ctx.frame.get(key).get();
            }
        }
        return null;
    }

    public void step() throws ExecutionException {
        globalContext.step();
    }

    public Context caller() {
        var ctx = this;
        while (ctx.caller == null && ctx.wrapped != null) {
            ctx = ctx.wrapped;
        }
        return ctx.caller;
    }

    public void setCaller(Context caller) {
        this.caller = caller;
    }

    public List<String> exporting() {
        return exporting;
    }

    public void addExport(String exporting) {
        this.exporting.add(exporting);
    }
}
