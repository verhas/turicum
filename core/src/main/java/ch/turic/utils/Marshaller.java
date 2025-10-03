package ch.turic.utils;

import ch.turic.ExecutionException;
import ch.turic.Program;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A utility class responsible for serializing objects into byte arrays.
 * This class works in conjunction with the Unmarshaller to convert objects
 * into a binary format that can be stored or transmitted.
 */
public class Marshaller {

    /**
     * Marker used to indicate array values in the serialized stream.
     */
    public static final short ARRAY_SIGN = 999;

    /**
     * Marker used to indicate map values in the serialized stream.
     */
    public static final short MAP_SIGN = 998;

    /**
     * Marker used to indicate null values in the serialized stream.
     */
    public static final short NULL_SIGN = 0;

    /**
     * Magic number used to identify the serialized data format.
     */
    public static final int MAGIC = 0xCADAFABE;

    /**
     * Version number of the serialization format.
     */
    public static final short VERSION = 1;

    /**
     * Starting offset for class registry IDs.
     */
    public static final short OFFSET = 1000;

    private final Map<String, Short> classRegistry = new HashMap<>();
    private final ArrayList<String> classes = new ArrayList<>();
    private short registryCounter = OFFSET;

    /**
     * Serializes a {@code Program} object into its corresponding byte array representation.
     * The serialization process includes adding metadata, such as a magic number, class registry entries,
     * and the marshalled form of the program's internal data structure.
     *
     * @param program the {@code Program} object to be serialized
     * @return a {@code byte[]} containing the serialized representation of the given {@code Program} object
     * @throws RuntimeException if any I/O error occurs during serialization
     */
    public byte[] serialize(Program program) {
        try (final var baos = new ByteArrayOutputStream();
             final var buffer = new DataOutputStream(baos)) {
            final var code = marshall(program);
            buffer.writeInt(MAGIC);
            buffer.writeShort(VERSION);
            buffer.writeShort(classes.size());
            for (final var className : classes) {
                buffer.writeUTF(className);
            }
            buffer.write(code);
            byte[] serialized = baos.toByteArray();
            return compress(serialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Compresses a byte array using Deflater.
     *
     * @param serialized the byte array to compress
     * @return the compressed byte array
     * @throws RuntimeException if compression fails
     */
    private byte[] compress(byte[] serialized) {
        final var deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream dStream = new DeflaterOutputStream(baos, deflater)) {
            new ByteArrayInputStream(serialized).transferTo(dStream);
            dStream.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Compression failed", e);
        } finally {
            deflater.end();
        }
    }

    /**
     * Marshals an object into its byte array representation. The method determines if the object
     * is an array or a non-array object, and delegates marshalling to the appropriate method.
     *
     * @param object the object to be marshalled, which can be null, an array, or a non-array object
     * @return a byte array representing the marshalled form of the given object
     */
    private byte[] marshall(Object object) {
        if (object == null) {
            return new byte[]{NULL_SIGN, NULL_SIGN};
        }
        if (object instanceof Map<?,?> map) {
            return marshall_map(map);
        }
        if (object.getClass().isArray()) {
            return marshall_array(object);
        } else {
            return marshall_non_array(object);
        }
    }

    /**
     * Marshals a Map into its byte array representation. This method serializes
     * the map's size, followed by the serialized forms of its keys and values.
     *
     * @param map the Map to be marshalled; each key and value should be serializable
     * @return a byte array representing the marshalled form of the given Map
     * @throws RuntimeException if an I/O error occurs during the marshalling process
     */
    private byte[] marshall_map(Map<?, ?> map) {
        try (final var baos = new ByteArrayOutputStream();
             final var buffer = new DataOutputStream(baos)) {
            buffer.writeShort(MAP_SIGN);
            buffer.writeInt(map.size());
            for (final var entry : map.entrySet()) {
                buffer.write(marshall(entry.getKey()));
                buffer.write(marshall(entry.getValue()));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Marshals an array into its byte array representation. This method ensures that the input
     * is an array and serializes its elements along with the necessary metadata such as the array identifier
     * and its length.
     *
     * @param array the array to be marshalled; must be a valid array object
     * @return a byte array representing the marshalled form of the given array
     * @throws RuntimeException if the input is not an array or if any I/O error occurs during the marshalling process
     */
    private byte[] marshall_array(Object array) {
        if (array.getClass().isArray()) {
            try (final var baos = new ByteArrayOutputStream();
                 final var buffer = new DataOutputStream(baos)) {
                buffer.writeShort(ARRAY_SIGN);
                final int length = Array.getLength(array);
                buffer.writeInt(length);
                for (int i = 0; i < length; i++) {
                    buffer.write(marshall(Array.get(array, i)));
                }
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("marshall_array was called for non array");
        }
    }

    /**
     * Marshals a non-array object into its byte array representation. This method retrieves
     * metadata such as the class ID and number of fields, and serializes each field that is not synthetic,
     * static, or non-final into the output stream. The serialized object is returned as a byte array.
     *
     * @param object the non-array object to be marshalled; must not be null
     * @return a byte array representing the marshalled form of the given non-array object
     * @throws ExecutionException if an error occurs while accessing the object's fields
     * @throws RuntimeException   if an I/O error occurs during the marshalling process
     */
    private byte[] marshall_non_array(Object object) {
        try (final var baos = new ByteArrayOutputStream();
             final var buffer = new DataOutputStream(baos)) {
            final var id = getClassId(object);
            buffer.writeShort(id);
            switch (object) {
                case String s -> buffer.writeUTF(s);
                case Boolean b -> buffer.writeBoolean(b);
                case Long l -> buffer.writeLong(l);
                case Integer i -> buffer.writeInt(i);
                case Double d -> buffer.writeDouble(d);
                case Enum<?> e -> buffer.writeUTF(e.name());
                default -> {
                    short fieldCounter = countFields(object);
                    buffer.writeShort(fieldCounter);
                    for (final var f : object.getClass().getDeclaredFields()) {
                        if (isFieldToMarshall(f)) {
                            final var name = f.getName();
                            buffer.writeUTF(name);
                            f.setAccessible(true);
                            buffer.write(marshall(f.get(object)));
                        }
                    }
                }
            }
            return baos.toByteArray();
        } catch (IllegalAccessException e) {
            throw new ExecutionException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Counts the number of fields in the given object that meet the specified criteria
     * of being marshalled. The criteria are defined by the {@code isFieldToMarshall} method,
     * which checks if the field is non-synthetic, non-static, and final.
     *
     * @param object the object whose fields will be analyzed; must not be null
     * @return the number of fields in the object that satisfy the specified criteria
     */
    private short countFields(Object object) {
        short fieldCounter = 0;
        for (final var f : object.getClass().getDeclaredFields()) {
            if (isFieldToMarshall(f)) {
                fieldCounter++;
            }
        }
        return fieldCounter;
    }

    /**
     * Determines whether the specified field should be included in the marshalling process.
     * A field is eligible for marshalling if it is not synthetic, is declared as final,
     * and is not static.
     *
     * @param f the {@code Field} object to be evaluated
     * @return {@code true} if the field meets the criteria for marshalling;
     * {@code false} otherwise
     */
    private boolean isFieldToMarshall(Field f) {
        int modifiers = f.getModifiers();
        return !f.isSynthetic() && (modifiers & Modifier.FINAL) != 0 && (modifiers & Modifier.STATIC) == 0;
    }

    /**
     * Retrieves the class ID associated with the given object's class.
     * <p>
     * If the class is not already registered, it assigns a new ID and registers the class
     * into the internal class registry.
     *
     * @param object the object whose class ID is to be retrieved; must not be null
     * @return the class ID corresponding to the given object's class
     */
    private short getClassId(Object object) {
        final var cname = object.getClass().getName();
        if (!classRegistry.containsKey(cname)) {
            classRegistry.put(cname, registryCounter++);
            classes.add(cname);
        }
        return classRegistry.get(cname);
    }

}
