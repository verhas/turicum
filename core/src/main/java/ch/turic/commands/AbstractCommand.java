package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.analyzer.Pos;
import ch.turic.memory.*;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCommand implements Command, HasFields {
    private Pos startPosition;
    private Pos endPosition;
    private volatile LngObject lngObjectRef = null;

    public LngObject toLngObject(Context context) {
        if (lngObjectRef == null) {
            synchronized (this) {
                if (lngObjectRef == null) {
                    lngObjectRef = _toLngObject(context);
                }
            }
        }
        return lngObjectRef;
    }

    public LngObject _toLngObject(Context context) throws ExecutionException {
        return _toLngObject(this, context);
    }

    private LngObject _toLngObject(Object object, Context context) throws ExecutionException {
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

    private LngList _toLngList(Object object, Context context) throws ExecutionException {
        final var lngList = new LngList();
        final int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            lngList.array.add(cast2Lang(context, Array.get(object, i)));
        }
        return lngList;
    }

    private LngObject castMap(Context context, Map<?, ?> map) throws ExecutionException {
        final var lngObject = LngObject.newEmpty(context);
        map.forEach((key, value) -> lngObject.setField(key.toString(), cast2Lang(context, value)));
        return lngObject;
    }

    private Object cast2Lang(Context context, Object command) {
        if (command.getClass().isArray()) {
            return _toLngList(command, context);
        }
        return switch (command) {
            case AbstractCommand cmd -> cmd.toLngObject(context);
            case String s -> s;
            case Long s -> s;
            case Double s -> s;
            case Boolean s -> s;
            case Map m -> castMap(context, (Map<?, ?>) m);
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
        this.endPosition = endPosition;
    }

    public void setStartPosition(Pos startPosition) {
        this.startPosition = startPosition;
    }

    public Object execute(final Context ctx) throws ExecutionException {
        final var sf = new LngStackFrame(this);
        ctx.threadContext.push(sf);
        final var result = _execute(ctx);
        ctx.threadContext.pop();
        return result;
    }

    public abstract Object _execute(final Context ctx) throws ExecutionException;

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("Commands are immutable objects");
    }

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
