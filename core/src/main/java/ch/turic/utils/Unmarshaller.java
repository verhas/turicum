package ch.turic.utils;

import ch.turic.Command;
import ch.turic.Program;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Unmarshaller {

    private final Map<Short, Class<?>> classRegistry = new HashMap<>();

    public Program deserialize(byte[] data) {
        try (var bais = new ByteArrayInputStream(data);
             var input = new DataInputStream(bais)) {

            int magic = input.readInt();
            if (magic != Marshaller.MAGIC) throw new RuntimeException("Invalid magic number");

            short version = input.readShort();
            if (version != Marshaller.VERSION) throw new RuntimeException("Unsupported version: " + version);

            short count = input.readShort();
            for (short i = 0; i < count; i++) {
                String cname = input.readUTF();
                classRegistry.put((short) (i + Marshaller.OFFSET), Class.forName(cname));
            }
            return (Program) unmarshall(input);
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException("Failed to deserialize", e);
        }
    }

    private Object unmarshall(DataInputStream input) throws IOException, ReflectiveOperationException {
        short marker = input.readShort();

        if (marker == Marshaller.NULL_SIGN) {
            return null;
        }

        if (marker == Marshaller.ARRAY_SIGN) {
            return unmarshall_array(input);
        }
        if (marker == Marshaller.MAP_SIGN) {
            return unmarshall_map(input);
        }

        Class<?> cls = classRegistry.get(marker);
        if (cls == null) throw new ClassNotFoundException("Unknown class ID: " + marker);

        final var name = cls.getCanonicalName();
        return switch (name) {
            case "java.lang.String" -> input.readUTF();
            case "java.lang.Boolean" -> input.readBoolean();
            case "java.lang.Long" -> input.readLong();
            case "java.lang.Integer" -> input.readInt();
            case "java.lang.Double" -> input.readDouble();
            default -> {
                if (cls.isEnum()) {
                    final var enumString = input.readUTF();
                    yield Enum.valueOf((Class<Enum>) cls, enumString);
                } else {
                    final var args = new Args();
                    short fieldCount = input.readShort();
                    for (int i = 0; i < fieldCount; i++) {
                        String fieldName = input.readUTF();
                        args.put(fieldName, unmarshall(input));
                    }
                    yield constructViaFactory(cls, args);
                }
            }
        };
    }

    private Map<?, ?> unmarshall_map(DataInputStream input) throws IOException, ReflectiveOperationException {
        int size = input.readInt();
        final var result = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            final var key = unmarshall(input);
            final var value = unmarshall(input);
            result.put(key, value);
        }
        return result;
    }

    private Object[] unmarshall_array(DataInputStream input) throws IOException, ReflectiveOperationException {
        int length = input.readInt();
        final var result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = unmarshall(input);
        }
        return result;
    }

    private Object constructViaFactory(Class<?> cls, Args fieldMap) {
        try {
            Method factory = cls.getDeclaredMethod("factory", Args.class);
            factory.setAccessible(true);
            if (!Modifier.isStatic(factory.getModifiers())) {
                throw new IllegalStateException(cls.getName() + " factory method must be static");
            }
            return factory.invoke(null, fieldMap);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing factory(Args) in " + cls.getName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke factory in " + cls.getName(), e);
        }
    }

    public static class Args {
        private final Map<String, Object> args = new HashMap<>();

        public void put(String name, Object value) {
            args.put(name, value);
        }

        public <T> T get(final String name, Class<T> targetClass) {
            return cast(args.get(name), targetClass);
        }

        public boolean bool(final String name) {
            return get(name, Boolean.class);
        }

        public String str(final String name) {
            return get(name, String.class);
        }

        public Command command(final String name) {
            return get(name, Command.class);
        }

        public Command[] commands() {
            return commands("commands");
        }

        public Command[] commands(final String name) {
            return get(name, Command[].class);
        }

        /**
         * Casts the given object to the specified type.
         * If the object is an array, converts it to a new array of the specified component type.
         *
         * @param obj         the object to cast or convert
         * @param targetClass the class representing the desired type or component type
         * @param <T>         the target type
         * @return the cast or converted object
         * @throws ClassCastException if conversion is not possible
         */
        @SuppressWarnings("unchecked")
        private static <T> T cast(Object obj, Class<T> targetClass) {
            if (obj == null) return null;

            if (targetClass.isArray()) {
                // target is an array type
                Class<?> componentType = targetClass.getComponentType();

                if (!obj.getClass().isArray()) {
                    throw new ClassCastException("Cannot cast non-array to array of " + componentType.getName());
                }

                int length = Array.getLength(obj);
                Object result = Array.newInstance(componentType, length);

                for (int i = 0; i < length; i++) {
                    Object element = Array.get(obj, i);
                    Array.set(result, i, cast(element, componentType)); // recursive cast
                }

                return (T) result;
            } else if (targetClass.isPrimitive()) {
                // Handle primitive types
                return switch (obj) {
                    case Number number when targetClass == double.class -> (T) (Object) number.doubleValue();
                    case Number number when targetClass == float.class -> (T) (Object) number.floatValue();
                    case Number number when targetClass == long.class -> (T) (Object) number.longValue();
                    case Number number when targetClass == int.class -> (T) (Object) number.intValue();
                    case Number number when targetClass == short.class -> (T) (Object) number.shortValue();
                    case Number number when targetClass == byte.class -> (T) (Object) number.byteValue();
                    case Character c when targetClass == char.class -> (T) c;
                    case Boolean b when targetClass == boolean.class -> (T) obj;
                    default ->
                            throw new ClassCastException("Cannot cast " + obj.getClass().getName() + " to " + targetClass.getName());
                };
            } else {
                // single object cast
                return targetClass.cast(obj);
            }
        }
    }


}



