package ch.turic.utils;

import ch.turic.Command;
import ch.turic.Program;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * A class responsible for deserializing Program objects from byte arrays.
 * This class works in conjunction with the {@link Marshaller} to reconstruct objects
 * from their serialized form.
 */
public class Unmarshaller {

    private final Map<Short, Class<?>> classRegistry = new HashMap<>();

    /**
     * Deserializes a compressed byte array into a Program object.
     *
     * @param compressedData the compressed byte array containing serialized data
     * @return the deserialized Program object
     * @throws RuntimeException if deserialization fails, the magic number is invalid,
     *         or the version is not supported
     */
    public Program deserialize(byte[] compressedData) {
        final var data = decompress(compressedData);
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

    /**
     * Decompresses a byte array using InflaterInputStream.
     *
     * @param compressedData the compressed byte array to decompress
     * @return the decompressed byte array
     * @throws RuntimeException if decompression fails
     */
    public static byte[] decompress(byte[] compressedData) {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                InflaterInputStream inflaterStream = new InflaterInputStream(bais);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            inflaterStream.transferTo(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }

    /**
     * Unmarshalls data from an input stream into an object based on the marker type.
     *
     * @param input the DataInputStream to read from
     * @return the unmarshalled object
     * @throws IOException if an I/O error occurs
     * @throws ReflectiveOperationException if reflection-related operations fail
     */
    @SuppressWarnings("unchecked")
    private Object unmarshall(DataInputStream input) throws IOException, ReflectiveOperationException {
        final short marker = input.readShort();

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

    /**
     * Unmarshalls a map from the input stream.
     *
     * @param input the DataInputStream to read from
     * @return the unmarshalled Map object
     * @throws IOException if an I/O error occurs
     * @throws ReflectiveOperationException if reflection-related operations fail
     */
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

    /**
     * Unmarshalls an array from the input stream.
     *
     * @param input the DataInputStream to read from
     * @return the unmarshalled Object array
     * @throws IOException if an I/O error occurs
     * @throws ReflectiveOperationException if reflection-related operations fail
     */
    private Object[] unmarshall_array(DataInputStream input) throws IOException, ReflectiveOperationException {
        int length = input.readInt();
        final var result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = unmarshall(input);
        }
        return result;
    }

    /**
     * Constructs an object using its factory method.
     *
     * @param cls the Class of the object to construct
     * @param fieldMap the Args object containing field values
     * @return the constructed object
     * @throws IllegalStateException if the factory method is missing or not static
     * @throws RuntimeException if factory method invocation fails
     */
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

    /**
     * Inner class that holds argument values for object construction.
     * Provides utility methods for accessing and converting values.
     *
     * This class is used in the static factory classes in the command classes.
     */
    public static class Args {
        private final Map<String, Object> args = new HashMap<>();

        /**
         * Stores a value with the specified name.
         *
         * @param name the name of the argument
         * @param value the value to store
         */
        public void put(String name, Object value) {
            args.put(name, value);
        }

        /**
         * Retrieves a value of the specified type.
         *
         * @param name the name of the argument
         * @param targetClass the expected class of the value
         * @param <T> the type parameter
         * @return the value cast to the specified type
         */
        public <T> T get(final String name, Class<T> targetClass) {
            return cast(args.get(name), targetClass);
        }

        /**
         * Retrieves a boolean value.
         *
         * @param name the name of the argument
         * @return the boolean value
         */
        public boolean bool(final String name) {
            return get(name, Boolean.class);
        }

        /**
         * Retrieves a String value.
         *
         * @param name the name of the argument
         * @return the String value
         */
        public String str(final String name) {
            return get(name, String.class);
        }

        /**
         * Retrieves a Command value.
         *
         * @param name the name of the argument
         * @return the Command value
         */
        public Command command(final String name) {
            return get(name, Command.class);
        }

        /**
         * Retrieves an array of Commands using the default name "commands".
         *
         * @return array of Commands
         */
        public Command[] commands() {
            return commands("commands");
        }

        /**
         * Retrieves an array of Commands with the specified name.
         *
         * @param name the name of the argument
         * @return array of Commands
         */
        public Command[] commands(final String name) {
            return get(name, Command[].class);
        }

        /**
         * Casts or converts an object to the specified type.
         *
         * @param obj the object to cast
         * @param targetClass the target class
         * @param <T> the type parameter
         * @return the cast or converted object
         * @throws ClassCastException if the conversion is not possible
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