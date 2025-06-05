package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngObject;

import java.util.ArrayList;

/**
 * The IsType class is an implementation of the TuriFunction interface that provides a function
 * named "is_type" for checking if an object belongs to a specified class in the Turi language system.
 * <p>
 * It is not an alternative using the function {@code type()} implemented in {@link Type}.
 * That function returns the string representation of the type and that works for all types.
 * This function
 * <ul>
 *     <li>works only on object and will result in {@code false} for non-object first arguments</li>
 *     <li>will work {@code true} if the object is not the type given, but matches the type: a child class of the specified type.</li>
 * </ul>
 */
public class IsType implements TuriFunction {

    @Override
    public String name() {
        return "is_type";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class, Object.class);
        final var obj = args.at(0).get();
        final var type = args.at(1).get();
        // it has to be an object
        if (!(obj instanceof LngObject lngObject)) {
            return isJavaType(obj, type);
        }
        if (type instanceof String typeName) {
            return isType(lngObject.lngClass(), typeName);
        } else if (type instanceof LngClass lngClass) {
            return isType(lngObject.lngClass(), lngClass);
        } else {
            // if not a string and not a class, then it is not a type and hence nothing can be of this "type"
            return false;
        }
    }

    private static final String[] packages;

    static {
        final var list = new ArrayList<String>();
        for (Package p : Package.getPackages()) {
            if (p.getName().startsWith("java.")) {
                list.add(p.getName() + ".");
            }
        }
        packages = list.toArray(String[]::new);
    }

    private static boolean isJdkType(String typeName) {
        for (final var p : packages) {
            if (typeName.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJavaType(Object obs, Object type) {
        if (type instanceof String typeName) {
            try {
                // in Turicum java types have a 'java.' prefix,
                // but we allow sloppily without this if it is a JDK class because
                // 'java.java.lang.'... would seem excessive and easy to forget in a source to add the extra prefix
                if (typeName.startsWith("java.") && !isJdkType(typeName)) {
                    typeName = typeName.substring(5);
                }
                final var klass = Class.forName(typeName);
                return klass.isInstance(obs);
            } catch (ClassNotFoundException e) {
                throw new ExecutionException("'%s' is not a valid Java type", typeName);
            }
        }
        throw new ExecutionException("Type " + type + " is not a string");
    }

    /**
     * Checks if a given class (`lngClass`) is the same as or derives from another class (`type`).
     *
     * @param lngClass the class to be checked.
     * @param type     the class to check against.
     * @return true if `lngClass` is the same as or is derived from `type`, false otherwise.
     */
    private static boolean isType(LngClass lngClass, LngClass type) {
        if (lngClass.equals(type)) {
            return true;
        }
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (isType(p, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given class (`lngClass`) matches the specified type name or
     * derives from a class with the specified type name.
     *
     * @param lngClass the class to check.
     * @param type     the name of the type to check against.
     * @return true if `lngClass` matches or derives from the given type name, false otherwise.
     */
    private static boolean isType(LngClass lngClass, String type) {
        if( lngClass == null ) {
            return false;
        }
        if (lngClass.name().equals(type)) {
            return true;
        }
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (isType(p, type)) {
                return true;
            }
        }
        return false;
    }
}
