package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Pos;
import ch.turic.memory.*;
import ch.turic.memory.debugger.BreakPoint;
import ch.turic.memory.debugger.DebuggerCommand;
import ch.turic.memory.debugger.DebuggerContext;
import ch.turic.utils.Unmarshaller;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

import static ch.turic.memory.debugger.DebuggerContext.State.PAUSED;

public abstract class AbstractCommand implements Command, HasFields {
    private Pos startPosition;
    private Pos endPosition;
    private volatile LngObject lngObjectRef = null;

    public LngObject toLngObject(LocalContext context) {
        if (lngObjectRef == null) {
            synchronized (this) {
                if (lngObjectRef == null) {
                    lngObjectRef = _toLngObject(context);
                }
            }
        }
        return lngObjectRef;
    }

    public LngObject _toLngObject(LocalContext context) throws ExecutionException {
        return _toLngObject(this, context);
    }

    private LngObject _toLngObject(Object object, LocalContext context) throws ExecutionException {
        try {
            final var lngObject = LngObject.newEmpty(context);
            lngObject.setField("java$canonicalName", object.getClass().getCanonicalName());
            for (final var f : object.getClass().getDeclaredFields()) {
                final var name = f.getName();
                final var modifiers = f.getModifiers();
                if (!f.isSynthetic() && (modifiers & Modifier.FINAL) != 0 && (modifiers & Modifier.STATIC) == 0) {
                    if (f.getType().isArray()) {
                        final var list = new LngList();
                        lngObject.setField(name, list);
                        f.setAccessible(true);
                        final var array = f.get(object);
                        if (array != null) {
                            final int length = Array.getLength(array);
                            for (int i = 0; i < length; i++) {
                                list.array.add(cast2Lang(context, Array.get(array, i)));
                            }
                        }
                    } else {
                        f.setAccessible(true);
                        final var value = f.get(object);
                        if (value != null) {
                            lngObject.setField(name, cast2Lang(context, value));
                        }
                    }
                }
            }
            return lngObject;
        } catch (IllegalAccessException e) {
            throw new ExecutionException(e);
        }
    }

    private LngList _toLngList(Object object, LocalContext context) throws ExecutionException {
        final var lngList = new LngList();
        final int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            lngList.array.add(cast2Lang(context, Array.get(object, i)));
        }
        return lngList;
    }

    private LngObject castMap(LocalContext context, Map<?, ?> map) throws ExecutionException {
        final var lngObject = LngObject.newEmpty(context);
        map.forEach((key, value) -> lngObject.setField(key.toString(), cast2Lang(context, value)));
        return lngObject;
    }

    private Object cast2Lang(LocalContext context, Object command) {
        if (command.getClass().isArray()) {
            return _toLngList(command, context);
        }
        return switch (command) {
            case AbstractCommand cmd -> cmd.toLngObject(context);
            case String s -> s;
            case Long s -> s;
            case Double s -> s;
            case Boolean s -> s;
            case Map<?, ?> m -> castMap(context, (Map<?, ?>) m);
            default -> _toLngObject(command, context);
        };
    }

    public Pos startPosition() {
        return startPosition;
    }

    public Pos endPosition() {
        return endPosition;
    }

    public void setEndPosition(Pos endPosition) {
        this.endPosition = endPosition == null ? null : endPosition.clone();
    }

    public void setStartPosition(Pos startPosition) {
        this.startPosition = startPosition == null ? null : startPosition.clone();
    }

    @SuppressWarnings("unchecked")
    protected <T extends Command> T fixPosition(Unmarshaller.Args args) {
        this.setStartPosition(args.get("startPosition", Pos.class));
        this.setEndPosition(args.get("endPosition", Pos.class));
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Command> T fixPosition(Lex lex) {
        setStartPosition(lex.startPosition());
        setEndPosition(lex.endPosition());
        return (T) this;
    }


    public Object execute(final LocalContext ctx) throws ExecutionException {
        final var sf = new LngStackFrame(this);
        final var dc = ctx.threadContext.getDebuggerContext();
        boolean step = handleDebugActions(ctx, dc);

        ctx.threadContext.push(sf);
        final var result = _execute(ctx);
        ctx.threadContext.pop();
        if (dc != null && step) {
            dc.setState(DebuggerContext.State.STEPPING);
        }
        return result;
    }

    /**
     * Handles the debug actions based on the current context and debugger state.
     * This method processes and updates the debugger state according to the
     * provided commands and checks for breakpoints or stepping actions.
     * It handles stepping into, stepping over, continuing execution, or stopping
     * the execution based on the debugger commands.
     *
     * @param ctx The current execution context, which includes global states and configurations.
     * @param dc  The debugger context, containing state and commands for controlling debugging actions.
     * @return A boolean indicating whether stepping has to be restored after the execution of the command or not.
     * @throws ExecutionException If an error occurs during execution or if stopped by the debugger.
     */
    private boolean handleDebugActions(LocalContext ctx, DebuggerContext dc) {
        boolean step = false;
        if (!ctx.globalContext.debugMode() || dc == null) {
            return false;
        }
        if (dc.getState() == DebuggerContext.State.STEPPING) {
            dc.setState(PAUSED);
        }
        if (dc.getState() == DebuggerContext.State.RUNNING) {
            if (dc.isBreakPoint(this)) {
                dc.setState(PAUSED);
            }
        }
        final var command = new DebuggerCommand(ctx.threadContext);
        while (dc.getState() == PAUSED) {
            try {
                dc.pause(command);
                dc.setState(switch (command.command()) {
                    case STOP -> throw new ExecutionException("Stopped by debugger");
                    case RUN -> DebuggerContext.State.RUNNING;
                    case STEP_OVER -> {
                        step = true; // step over
                        yield DebuggerContext.State.RUNNING;
                    }
                    case STEP_INTO -> {
                        step = true; // we need to stop if we step into and on a deeper level, we start to run
                        yield DebuggerContext.State.STEPPING;
                    }
                    case POS -> {
                        command.response = new DebuggerCommand.PosResponse(startPosition(), endPosition());
                        yield PAUSED;
                    }
                    case LOCALS -> {
                        final var keys = ctx.keys();
                        final var map = new HashMap<String, Object>();
                        for (final var k : keys) {
                            final var variable = ctx.get(k);
                            map.put(k, variable);
                        }
                        command.response = new DebuggerCommand.VarResponse(map);
                        yield PAUSED;
                    }
                    case GLOBALS -> {
                        final var keys = ctx.globalContext.heap.keySet();
                        final var map = new HashMap<String, Object>();
                        for (final var k : keys) {
                            final var variable = ctx.globalContext.heap.get(k);
                            map.put(k, variable.get());
                        }
                        command.response = new DebuggerCommand.VarResponse(map);
                        yield PAUSED;
                    }
                    case BREAKPOINTS -> {
                        command.response = new DebuggerCommand.BreakpointsResponse(dc.breakpoints());
                        yield PAUSED;
                    }
                    case GLOBAL_BREAKPOINTS -> {
                        final var dbgCtx = ctx.globalContext.getDebuggerContext();
                        command.response = new DebuggerCommand.BreakpointsResponse(dbgCtx.breakpoints());
                        yield PAUSED;
                    }
                    case ADD_BREAKPOINT -> {
                        final var bp = new BreakPoint(command.breakPointLine());
                        dc.addBreakPoint(bp);
                        yield PAUSED;
                    }
                    case REMOVE_BREAKPOINT -> {
                        final var bp = new BreakPoint(command.breakPointLine());
                        dc.removeBreakPoint(bp);
                        yield PAUSED;
                    }
                    case ADD_GLOBAL_BREAKPOINT -> {
                        final var bp = new BreakPoint(command.breakPointLine());
                        final var dbgCtx = ctx.globalContext.getDebuggerContext();
                        dbgCtx.addBreakPoint(bp);
                        yield PAUSED;
                    }
                    case REMOVE_GLOBAL_BREAKPOINT -> {
                        final var bp = new BreakPoint(command.breakPointLine());
                        final var dbgCtx = ctx.globalContext.getDebuggerContext();
                        dbgCtx.removeBreakPoint(bp);
                        yield PAUSED;
                    }
                    case COMMAND -> {
                        command.response = new DebuggerCommand.CommandResponse(this);
                        yield PAUSED;
                    }
                    default -> PAUSED;
                });
            } catch (Throwable e) {
                throw new ExecutionException(e);
            }
        }
        return step;
    }

    public abstract Object _execute(final LocalContext ctx) throws ExecutionException;

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("Commands are immutable objects");
    }

    /**
     * Retrieves the value of a specified field by its name.
     * Special handling is provided for array fields which are converted into a list.
     * If the field name is "java$canonicalName", the canonical name of the class is returned.
     * Throws an {@link ExecutionException} if the field does not exist or cannot be accessed.
     *
     * @param name The name of the field to retrieve.
     * @return The value of the specified field, or a converted list if the field is an array.
     * @throws ExecutionException If the specified field is not found or cannot be accessed.
     */
    @Override
    public Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "java$canonicalName" -> this.getClass().getCanonicalName();
            default -> {
                try {
                    final var f = this.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    final var value = f.get(this);
                    if (f.getType().isArray()) {
                        final var list = new LngList();
                        if (value != null) {
                            final int length = Array.getLength(value);
                            for (int i = 0; i < length; i++) {
                                list.array.add(Array.get(value, i));
                            }
                        }
                        yield list;
                    } else {
                        yield value;
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new ExecutionException("There is no such field: " + name);
                }
            }
        };
    }

    /**
     * Retrieves a set of field names declared in the current class.
     * The method includes the predefined field "java$canonicalName" and dynamically inspects
     * all declared fields of the class. Only non-synthetic fields that are `final`
     * but not `static` are added to the set.
     *
     * @return A set of field names representing the declared fields in the current class,
     * including "java$canonicalName" and other eligible fields.
     */
    @Override
    public Set<String> fields() {
        final var fieldSet = new HashSet<String>();
        fieldSet.add("java$canonicalName");
        for (final var f : this.getClass().getDeclaredFields()) {
            final var name = f.getName();
            final var modifiers = f.getModifiers();
            if (!f.isSynthetic() && (modifiers & Modifier.FINAL) != 0 && (modifiers & Modifier.STATIC) == 0) {
                fieldSet.add(name);
            }
        }
        return fieldSet;
    }

}
