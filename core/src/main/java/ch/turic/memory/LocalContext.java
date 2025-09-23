package ch.turic.memory;


import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.memory.debugger.ConcurrentWorkItem;
import ch.turic.memory.debugger.DebuggerContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keep a context of the current thread's executing environment.
 */
public class LocalContext implements Context, AutoCloseable {
    VarTable frame;
    private final Set<String> globals = new HashSet<>();
    private final Set<String> nonlocal = new HashSet<>();
    private final Set<String> frozen;
    private final LocalContext wrapped;
    public final GlobalContext globalContext;
    public final ThreadContext threadContext;
    public LocalContext caller = null;
    private final List<String> exporting = new ArrayList<>();
    private final boolean shadow;
    private final boolean with;
    private boolean pinned = false;

    public Set<String> keys() {
        return frame.keySet();
    }

    /**
     * Retrieves a set containing all keys present in the current context.
     * This includes both local keys and global keys. The method combines the keys
     * from the local context and the global context, ensuring no duplicates.
     * <p>
     * Since this returns only the set of keys, it isn't very sensible to ask which is
     * returned in the case of name collision. It is the name only.
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
    public LocalContext() {
        this(-1);
    }

    /**
     * Create a new context with limited steps
     *
     * @param stepLimit the number of maximal steps before killing the interpreter
     */
    public LocalContext(int stepLimit) {
        this.globalContext = new GlobalContext(stepLimit);
        this.threadContext = new ThreadContext(Thread.currentThread());
        this.globalContext.registerContext(threadContext);
        this.wrapped = null;
        this.frame = globalContext.heap;
        this.shadow = false;
        this.with = false;
        this.frozen = new HashSet<>();
    }

    public LocalContext(final GlobalContext globalContext, final ThreadContext threadContext) {
        this.wrapped = null;
        this.frame = new VarTable();
        this.globalContext = globalContext;
        this.threadContext = threadContext;
        if (threadContext != null) {
            this.globalContext.registerContext(threadContext);
        }
        this.shadow = false;
        this.with = false;
        this.frozen = new HashSet<>();
    }

    /**
     * Clone the context, creating a new stack frame. The new context inherits the heap and the step limit and the
     * already consumed the steps' counter.
     * <p>
     * The new context has its own heap.
     * <p>
     * The set of globals is also inherited (copied). If a global declaration is used in a context it will not affect
     * the surrounding context.
     *
     * @param clone   the current context when starting a new frame
     * @param wrapped is the context wrapped by this context
     */
    private LocalContext(final LocalContext clone, final LocalContext wrapped) {
        this.globalContext = clone.globalContext;
        this.threadContext = clone.threadContext;
        this.frame = new VarTable();
        this.wrapped = wrapped;
        this.shadow = false;
        this.with = false;
        this.frozen = new HashSet<>();
    }

    private LocalContext(final LocalContext thisContext, final LocalContext wrappedContext, final boolean shadow) {
        this.globalContext = thisContext.globalContext;
        this.threadContext = thisContext.threadContext;
        this.frame = new VarTable();
        this.wrapped = wrappedContext;
        this.shadow = shadow;
        this.with = false;
        this.frozen = new HashSet<>();
    }

    private LocalContext(final LocalContext thisContext, final LocalContext wrappedContext, final LocalContext withContext) {
        this.globalContext = thisContext.globalContext;
        this.threadContext = thisContext.threadContext;
        this.frame = withContext.frame;
        this.frozen = withContext.frozen;
        this.wrapped = wrappedContext;
        this.shadow = false;
        this.with = true;
    }

    @Override
    public void close() {
        this.globalContext.removeContext(this.threadContext);
    }

    /**
     * The VariableHibernation class represents a variable that is temporarily
     * frozen and cannot be modified during hibernation.
     * <p>
     * Typically, a variable gets hibernated during an assignment while the right-hand side is calculated.
     * The right-hand side expression can use the value of the variable, but must not have side effects that modify it.
     * For example:
     * <pre>
     * {@code
     *      a = a++ +3
     * }
     * <pre/>
     * is an illegal structure, because it is a hack, unreadable and leads to bugs.
     * <p>
     * The calculation where the variable is to be hibernated should be performed in a try-with-resources block.
     * This ensures that the temporary freezing is released.
     * <p>
     * Constructor:
     * - Initializes a VariableHibernation with a specified variable.
     * - Freezes the variable if it is not already frozen.
     * - Sets the variable to null if it is already frozen.
     * <p>
     * Methods:
     * - {@code close()}: Releases the frozen state of the variable, allowing it
     * to be reused by other instances.
     */
    public class VariableHibernation implements AutoCloseable {
        final String variable;

        public static void close(VariableHibernation vh) {
            if (vh != null) {
                vh.close();
            }
        }

        public VariableHibernation() {
            this.variable = null;
        }

        public VariableHibernation(String variable) {
            if (frozen.contains(variable)) {
                this.variable = null;
            } else {
                frozen.add(variable);
                this.variable = variable;
            }
        }

        @Override
        public void close() {
            if (variable != null) {
                frozen.remove(variable);
            }
        }

    }

    /**
     * Creates a new instance of VariableHibernation using the provided identifier.
     *
     * @param identifier the unique identifier for the variable to be hibernated
     * @return a new VariableHibernation instance constructed with the specified identifier
     */
    public VariableHibernation hibernate(String identifier) {
        return new VariableHibernation(identifier);
    }

    /**
     * Freeze a local variable adding it to the frozen names. It means that it cannot be changed anymore after this
     * point.
     *
     * @param identifier the identifier to freeze
     */
    public void freeze(String identifier) {
        ExecutionException.when(frozen.contains(identifier), "variable is already pinned '" + identifier + "'");
        if (!contains(identifier)) {
            throw new ExecutionException("variable '" + identifier + "' is not defined, cannot be pinned");
        }
        if (with) {// we can freeze in the wrapped context if we are in a 'with' command
            if (containsFrame(identifier)) {
                frozen.add(identifier);
            } else {
                if (wrapped != null) {
                    wrapped.freeze(identifier);
                } else {
                    throw new ExecutionException("variable '" + identifier + "' is defined, but not found in freeze. It is an internal error.");
                }
            }
        } else {
            frozen.add(identifier);
        }
    }

    /**
     * Define a variable with the types defined by {@code typeNames}.
     *
     * @param key       the name of the variable
     * @param value     the value of the new variable
     * @param typeNames the names of the accepted types
     */
    public void define(String key, Object value, String[] typeNames) {
        final var v = createVariable(key, typeNames);
        v.value = value;
    }

    /**
     * Same as {@link #define(String, Object, String[]) define()}, but it throws an exception if the type does not fit the
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
     * Create a new variable. Also, check that the name is not global, not non-local, and not frozen.
     * <p>
     * The variable is also added to the local frame with the name, but it does not have a value (it is null).
     * <p>
     * It is an error if the variable already exists in the current context.
     * If this is a shadow context, then the next wrapped non-shadow context is checked for the variable definition.
     * Even if it is a shadow context, the variable, if it gets defined, is defined in the current context.
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
        ExecutionException.when(!frame.containsKey(key), "Variable '%s' is not defined in the local context, you cannot unlet it.", key);
        frame.remove(key);
        frozen.remove(key);
        globals.remove(key);
        nonlocal.remove(key);
    }


    public void mergeVariablesFrom(LocalContext ctx, Set<String> exceptions) throws ExecutionException {
        for (final var e : ctx.frame.entrySet()) {
            final var key = e.getKey();
            final var value = e.getValue();
            if (!exceptions.contains(key)) {
                if (frame.containsKey(key)) {
                    final var v = frame.get(key);
                    final var types = new ArrayList<Variable.Type>(v.types.length + value.types.length);
                    for (final var t : v.types) {
                        if (isNewType(types, t)) {
                            types.add(t);
                        }
                    }
                    for (final var t : value.types) {
                        if (isNewType(types, t)) {
                            types.add(t);
                        }
                    }
                    v.types = types.toArray(Variable.Type[]::new);
                    v.set(value.value);
                } else {
                    frame.put(key, value);
                }
            }
        }
    }

    private boolean isNewType(List<Variable.Type> types, Variable.Type type) {
        for (final var t : types) {
            if (t.equals(type)) {
                return false;
            }
        }
        return true;
    }

    public void local(String key, Object value) throws ExecutionException {
        ExecutionException.when(globals.contains(key), "Local variable is already defined as global '" + key + "'");
        ExecutionException.when(nonlocal.contains(key), "Variable cannot be local, it is already used as non-local '" + key + "'");
        ExecutionException.when(frozen.contains(key), "pinned variable cannot be altered '" + key + "'");
        frame.set(key, value);
    }

    public void global(String global) throws ExecutionException {
        ExecutionException.when(frame != globalContext.heap && frame.containsKey(global), "Global variable '%s' is already defined as local.", global);
        globals.add(global);
    }

    public void global(String global, Object value) throws ExecutionException {
        global(global);
        globalContext.heap.set(global, value);
    }

    /**
     * Open a new stack frame and return that context with the new stack frame.
     * Opening a new frame does not reference the current frame as a parent.
     * This is used when we execute a call in a new independent scope, like calling a method or function.
     * The code in the function does not have access to the caller.
     *
     * @return the new context. New stack frame, no wrapped.
     */
    public LocalContext open() {
        return new LocalContext(this, null);
    }

    /**
     * Create a new context that will have the {@code other} context frame.
     * It is used with the {@code with} command.
     *
     * @param other the other context from which we will use the frame in the new one
     * @return the nex context
     */
    public LocalContext with(LocalContext other) {
        return new LocalContext(this, this, other);
    }

    /**
     * Creates and returns a new Context instance associated with the specified thread.
     * This method generates a ThreadContext using the provided thread and combines it
     * with the existing globalContext to construct the new Context instance.
     *
     * @param thread the thread to be associated with the new Context
     * @return a new Context instance tied to the specified thread
     */
    public LocalContext thread(Thread thread) {
        return new LocalContext(globalContext, new ThreadContext(thread));
    }

    /**
     * Creates and returns a new Context instance, which is composed of a global context
     * and a thread-specific context.
     * <p>
     * Use this method if the thread context is created from a different thread and not from inside
     * the thread that is currently running.
     *
     * @return a new Context object that combines the global context and a thread-specific context
     */
    public LocalContext thread() {
        return new LocalContext(globalContext, new ThreadContext());
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
    public LocalContext wrap() {
        return new LocalContext(this, this);
    }

    public LocalContext shadow() {
        return new LocalContext(this, this, true);
    }

    /**
     * Create a new context and wrap the argument context into the new context.
     * This is the situation when we call a closure from somewhere. The context of the closure will not wrap the
     * current context, rather it will wrap the context in which it was created.
     *
     * @param wrapped the wrapped context, probably stored in a closure and queried from there
     * @return the new context
     */
    public LocalContext wrap(LocalContext wrapped) {
        return new LocalContext(this, wrapped);
    }

    /**
     * Assign a new value to an already defined identifier.
     * It is an error if the variable is not defined.
     * <p>
     * If the variable was declared as global, then update the value in the global context.
     * In this case, the variable may not exist yet in the global scope, but it was declared as 'global' in the local
     * scope, so it is okay.
     * This is a special case.
     * <p>
     * If the variable is not global, then the method tries to find the variable in the context and then in the wrapped
     * contexts one after the other, and it will update the one it finds the earliest.
     *
     * @param key   the identifier
     * @param value the value of the local whatnot
     */
    public void update(final String key, final Object value) {
        for (final var ctx : wrappingContexts()) {
            if (ctx.frozen.contains(key)) {
                throw new ExecutionException("Variable '%s' is pinned.", key);
            }
        }
        if (globals.contains(key)) {
            // when we set a global value, it does not matter if it is already defined because it is declared or
            // was already declared as 'global'
            globalContext.heap.set(key, value);
            return;
        }

        for (final var ctx : wrappingContexts()) {
            if (ctx.frame.containsKey(key)) {
                if (ctx.pinned) {
                    throw new ExecutionException("Variable '%s' is in a pinned context.", key);
                }
                if (ctx != this) {
                    nonlocal.add(key);
                }
                ctx.frame.set(key, value);
                return;
            }
        }
        ExecutionException.when(nonlocal.contains(key), "Variable '%s' was used as global, but is not declared, cannot be changed.", key);
        throw new ExecutionException("Variable '%s' is not defined.", key);
    }


    /**
     * Updates the value of a variable identified by the given key.
     * If the key exists in the global context, the value is updated directly in the global heap.
     * If the key exists in one of the wrapping contexts, its value is updated in the respective context.
     * Throws an exception if attempting to modify a variable that is either undeclared or treated as non-local
     * but not globally declared.
     * <p>
     * The variable is updated even if it was pinned.
     *
     * @param key   The identifier of the variable whose value needs to be updated.
     * @param value The new value to be assigned to the variable.
     */
    public void updateForce(final String key, final Object value) {
        if (globals.contains(key)) {
            globalContext.heap.set(key, value);
            return;
        }

        for (final var ctx : wrappingContexts()) {
            if (ctx.frame.containsKey(key)) {
                if (ctx != this) {
                    nonlocal.add(key);
                }
                ctx.frame.set(key, value);
                return;
            }
        }
        ExecutionException.when(nonlocal.contains(key), "Variable '%s' was used as global, but is not declared, cannot be changed.", key);
        throw new ExecutionException("Variable '%s' is not defined.", key);
    }

    /**
     * Returns a list of {@code LocalContext} instances representing the current context
     * and all wrapped contexts recursively.
     *
     * @return a list of {@code LocalContext} objects, including the current context and all wrapped contexts
     */
    public List<LocalContext> wrappingContexts() {
        final var ctxList = new ArrayList<LocalContext>();
        ctxList.add(this);
        if (wrapped != null) {
            ctxList.addAll(wrapped.wrappingContexts());
        }
        return ctxList;
    }

    public class ContextLock implements AutoCloseable {

        @Override
        public void close() {
            LocalContext.this.pinned = false;
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
     * Assigning a value to the local symbol key in the current context does not matter if it is defined there or
     * in the wrapped context, global, or frozen.
     * <p>
     * It is a primitive call used in a for each loop or setting 'this', 'cls', or the exception variable in 'catch'.
     * Generally, it is called from locations where it is guaranteed that the variable can be declared as local and
     * it was NOT defined in the context before.
     * (Hence ending zero at the end of the name, which many times denotes internal or native methods in the JDK).
     *
     * @param key   the loop identifier
     * @param value the value in the loop
     */
    public void let0(final String key, final Object value) {
        frame.set(key, value);
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

    public boolean debugMode(boolean debugMode, Channel<ConcurrentWorkItem<?>> channel) {
        threadContext.setDebuggerContext(new DebuggerContext(globalContext.debuggerContext, channel));
        return globalContext.debugMode(debugMode);
    }

    public void step() throws ExecutionException {
        globalContext.step();
    }

    public LocalContext caller() {
        var ctx = this;
        while (ctx.caller == null && ctx.wrapped != null) {
            ctx = ctx.wrapped;
        }
        return ctx.caller;
    }

    public void setCaller(LocalContext caller) {
        this.caller = caller;
    }

    public List<String> exporting() {
        return exporting;
    }

    public void addExport(String exporting) {
        this.exporting.add(exporting);
    }

    /**
     * Retrieves the current interpreted source or compiled file path.
     *
     * @return the current file path as a Path object
     */
    public Path sourcePath() {
        return globalContext.sourcePath;
    }

    /**
     * Sets the current interpreter source or compiled file path.
     *
     * @param path the Path object representing the current file to be set
     */
    public void sourcePath(Path path) {
        this.globalContext.sourcePath = path;
    }

}
