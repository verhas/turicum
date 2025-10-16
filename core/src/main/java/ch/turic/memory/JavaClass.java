package ch.turic.memory;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.utils.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a wrapper for a Java class, providing functionality to interact with its
 * fields and instantiate objects using constructors via reflection.
 * The class implements the `HasFields` interface, allowing field manipulation,
 * and the `LngCallable.LngCallableClosure` for callable behavior in a runtime context.
 */
public record JavaClass(Class<?> klass) implements HasFields, LngCallable.LngCallableClosure{

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        try {
            Field field;
            try {
                field = klass.getField(name);
            }catch (NoSuchFieldException e){
                field = klass().getDeclaredField(name);
                field.setAccessible(true);
            }
            if(Modifier.isStatic(field.getModifiers()) ){
                field.setAccessible(true);
                field.set(null,value);
            }else{
                throw new ExecutionException("Cannot set field '%s.%s' on the Java class, it is not static.", klass.getName(), name);
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        try {
            Field field;
            try {
                field = klass.getField(name);
            }catch (NoSuchFieldException e){
                field = klass().getDeclaredField(name);
                field.setAccessible(true);
            }
            if(Modifier.isStatic(field.getModifiers()) ){
                field.setAccessible(true);
                return field.get(null);
            }else{
                throw new ExecutionException("Cannot set field '%s.%s' on the Java class, it is not static.", klass.getName(), name);
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Set<String> fields() {
        return Arrays.stream(klass.getFields()).filter(f -> Modifier.isStatic(f.getModifiers())).map(Field::getName).collect(Collectors.toSet());
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var constructor = Reflection.getConstructorForArgs(klass,arguments);
        if( constructor == null ){
            throw new ExecutionException("Cannot find constructor for class % s arguments (%s)", klass.getName(), Arrays.stream(arguments).map(o -> o.getClass() +":" +o).collect(Collectors.joining(",")));
        }
        try {
            return Reflection.newInstance(constructor,arguments);
        } catch (Exception e) {
            throw new ExecutionException("Cannot instantiate class %s", klass.getName(), e);
        }
    }
}
