package ch.turic.builtins.classes;

import ch.turic.LngCallable;
import ch.turic.TuriClass;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.utils.BinUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Implements the methods callable on {@code bin} values, which are raw Java {@code byte[]} arrays.
 * <p>
 * The language surface is unsigned: byte values are returned in the 0..255 range and accepted in
 * the -128..255 range (stored as the low 8 bits). Methods that produce a new buffer always return
 * a fresh array; {@code fill} and {@code set} and the {@code set_*_at} codec methods modify the
 * array in place and the change is visible through every reference to the same {@code bin}.
 */
public class TuriBin implements TuriClass {
    @Override
    public Class<?> forClass() {
        return byte[].class;
    }

    /**
     * Returns a callable object that provides bin manipulation methods for the given byte array target.
     *
     * @param target     the byte array instance on which the method will operate
     * @param identifier the name of the bin method to retrieve
     * @return a callable object implementing the requested operation, or null if the identifier is unrecognized
     * @throws ExecutionException if the target is not a byte array, or if invalid arguments are supplied
     */
    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if (!(target instanceof byte[] bytes)) {
            throw new ExecutionException("Target object is not a byte[], this is an internal error");
        }

        return switch (identifier) {
            // snippet bin_functions_doc
            case "to_string" ->
                // returns the textual representation of the bin, `bin"..."` with the content in hexadecimal.
                    new TuriMethod<>((args) -> BinUtils.display(bytes));
            case "length" ->
                // a convenience method that returns the number of bytes.
                // The Turicum way is to use the `len()` built-in function, but this method also works on bin values.
                    new TuriMethod<>((args) -> (long) bytes.length);
            case "text" ->
                // decode the bytes to a string.
                // The character encoding is UTF-8 by default; a different one can be named as an argument.
                    new TuriMethod<>((args) -> new String(bytes, charset("text", args)));
            case "to_list" ->
                // return a list that contains the byte values as integers in the range 0..255.
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        for (final byte b : bytes) {
                            list.array.add(BinUtils.unsigned(b));
                        }
                        return list;
                    });
            case "base64" ->
                // encode the bytes with Base64 and return the string.
                    new TuriMethod<>((args) -> Base64.getEncoder().encodeToString(bytes));
            case "base64_url" ->
                // encode the bytes with the URL and filename safe Base64 alphabet and return the string.
                    new TuriMethod<>((args) -> Base64.getUrlEncoder().encodeToString(bytes));
            case "hex" ->
                // return the content as a lowercase hexadecimal string, two digits per byte, no separator.
                    new TuriMethod<>((args) -> BinUtils.hex(bytes));
            case "copy" ->
                // return a new bin with the same content.
                // Use it to get an independent value, since bin values are mutable and assignment does not copy.
                    new TuriMethod<>((args) -> Arrays.copyOf(bytes, bytes.length));
            case "md5" ->
                // calculate the md5 hash of the bytes. The result is a bin; use `hex()` on it for display.
                    new TuriMethod<>((args) -> digest(bytes, "MD5"));
            case "sha_1" ->
                // calculate the sha-1 hash of the bytes. The result is a bin.
                    new TuriMethod<>((args) -> digest(bytes, "SHA-1"));
            case "sha_256" ->
                // calculate the sha-256 hash of the bytes. The result is a bin.
                    new TuriMethod<>((args) -> digest(bytes, "SHA-256"));
            case "sha_512" ->
                // calculate the sha-512 hash of the bytes. The result is a bin.
                    new TuriMethod<>((args) -> digest(bytes, "SHA-512"));
            case "digest" ->
                // calculate the hash of the bytes. The algorithm name has to be provided as an argument. The result is a bin.
                    new TuriMethod<>((args) -> digest(bytes, "" + args[0]));
            case "index_of" ->
                // return the first position of the argument, or -1 when it is not found.
                // The argument is a byte value (an integer) or a bin subsequence.
                // The optional second argument is the position where the search starts.
                    new TuriMethod<>((args) -> {
                        final var needle = needle("index_of", args);
                        final int from = args.length == 2 ? Cast.toInteger(args[1]) : 0;
                        ExecutionException.when(args.length > 2, "index_of() needs one or two arguments");
                        return (long) BinUtils.indexOf(bytes, needle, from);
                    });
            case "last_index_of" ->
                // return the last position of the argument, or -1 when it is not found.
                // The argument is a byte value (an integer) or a bin subsequence.
                // The optional second argument is the highest position where the occurrence may start.
                    new TuriMethod<>((args) -> {
                        final var needle = needle("last_index_of", args);
                        final int from = args.length == 2 ? Cast.toInteger(args[1]) : bytes.length;
                        ExecutionException.when(args.length > 2, "last_index_of() needs one or two arguments");
                        return (long) BinUtils.lastIndexOf(bytes, needle, from);
                    });
            case "contains" ->
                // return `true` if the bin contains the argument, which is a byte value (an integer) or a bin subsequence.
                // The same test is available as the `in` operator.
                    new TuriMethod<>((args) -> BinUtils.indexOf(bytes, needle("contains", args), 0) >= 0);
            case "starts_with" ->
                // return `true` if the bin starts with the argument bin.
                    new TuriMethod<>((args) -> {
                        final var prefix = binArg("starts_with", args);
                        return prefix.length <= bytes.length && BinUtils.indexOf(bytes, prefix, 0) == 0;
                    });
            case "ends_with" ->
                // return `true` if the bin ends with the argument bin.
                    new TuriMethod<>((args) -> {
                        final var postfix = binArg("ends_with", args);
                        return postfix.length <= bytes.length
                                && BinUtils.lastIndexOf(bytes, postfix, bytes.length - postfix.length) == bytes.length - postfix.length;
                    });
            case "count" ->
                // count the non-overlapping occurrences of the argument, which is a byte value (an integer) or a bin subsequence.
                    new TuriMethod<>((args) -> {
                        final var needle = needle("count", args);
                        ExecutionException.when(needle.length == 0, "count() needs a non-empty argument");
                        long count = 0;
                        int i = 0;
                        while ((i = BinUtils.indexOf(bytes, needle, i)) >= 0) {
                            i += needle.length;
                            count++;
                        }
                        return count;
                    });
            case "is_empty" ->
                // return `true` if the bin has no bytes.
                    new TuriMethod<>((args) -> bytes.length == 0);
            case "is_not_empty" ->
                // return `false` if the bin has no bytes.
                    new TuriMethod<>((args) -> bytes.length != 0);
            case "left" ->
                // return a new bin with at most the argument number of leading bytes.
                // If the argument is equal to, or larger than the length, the whole content is returned (as a new bin).
                    new TuriMethod<>((args) -> {
                        final int n = Cast.toInteger(args[0]);
                        return Arrays.copyOf(bytes, Math.min(Math.max(n, 0), bytes.length));
                    });
            case "right" ->
                // return a new bin with at most the argument number of trailing bytes.
                // If the argument is equal to, or larger than the length, the whole content is returned (as a new bin).
                    new TuriMethod<>((args) -> {
                        final int n = Math.min(Math.max(Cast.toInteger(args[0]), 0), bytes.length);
                        return Arrays.copyOfRange(bytes, bytes.length - n, bytes.length);
                    });
            case "reverse" ->
                // return a new bin that contains the bytes in reverse order.
                    new TuriMethod<>((args) -> {
                        final var reversed = new byte[bytes.length];
                        for (int i = 0; i < bytes.length; i++) {
                            reversed[i] = bytes[bytes.length - 1 - i];
                        }
                        return reversed;
                    });
            case "xor" ->
                // return a new bin that is the byte-wise XOR of this bin and the argument bin.
                // The two bins must have the same length.
                    new TuriMethod<>((args) -> {
                        final var other = binArg("xor", args);
                        ExecutionException.when(other.length != bytes.length,
                                "xor() needs a bin of the same length, %d != %d", bytes.length, other.length);
                        final var result = new byte[bytes.length];
                        for (int i = 0; i < bytes.length; i++) {
                            result[i] = (byte) (bytes[i] ^ other[i]);
                        }
                        return result;
                    });
            case "fill" ->
                // set a range of the bin to the given byte value, in place, and return the bin itself.
                // Without further arguments the whole bin is filled.
                // The optional second and third arguments give the start (inclusive) and the end (exclusive) of the range.
                    new TuriMethod<>((args) -> {
                        ExecutionException.when(args.length < 1 || args.length > 3, "fill() needs one to three arguments");
                        final var value = BinUtils.toByte(args[0]);
                        final int from = args.length >= 2 ? Cast.toInteger(args[1]) : 0;
                        final int to = args.length == 3 ? Cast.toInteger(args[2]) : bytes.length;
                        ExecutionException.when(from < 0 || to > bytes.length || from > to,
                                "fill() range %d..%d is out of the bin of length %d", from, to, bytes.length);
                        Arrays.fill(bytes, from, to, value);
                        return bytes;
                    });
            case "set" ->
                // copy the whole argument bin into this bin starting at the given position, in place,
                // and return the bin itself. The copied content must fit.
                    new TuriMethod<>((args) -> {
                        ExecutionException.when(args.length != 2, "set() needs two arguments, a position and a bin");
                        final int pos = Cast.toInteger(args[0]);
                        if (!(args[1] instanceof byte[] src)) {
                            throw new ExecutionException("The second argument to set() has to be a bin");
                        }
                        ExecutionException.when(pos < 0 || pos + src.length > bytes.length,
                                "set() with %d bytes at position %d does not fit into the bin of length %d", src.length, pos, bytes.length);
                        System.arraycopy(src, 0, bytes, pos, src.length);
                        return bytes;
                    });
            case "u8_at" ->
                // read the byte at the given position as an unsigned integer, 0..255.
                    intReader(bytes, "u8_at", 1, false);
            case "i8_at" ->
                // read the byte at the given position as a signed integer, -128..127.
                    intReader(bytes, "i8_at", 1, true);
            case "u16_at" ->
                // read two bytes at the given position as an unsigned integer.
                // The optional second argument is the byte order: `"be"` (default), `"le"`,
                // or a permutation of the digits 1..2 where 1 is the least significant byte.
                    intReader(bytes, "u16_at", 2, false);
            case "i16_at" ->
                // read two bytes at the given position as a signed integer.
                    intReader(bytes, "i16_at", 2, true);
            case "u32_at" ->
                // read four bytes at the given position as an unsigned integer.
                // The optional second argument is the byte order: `"be"` (default), `"le"`,
                // or a permutation of the digits 1..4, for example `"3412"` for the PDP-11 layout.
                    intReader(bytes, "u32_at", 4, false);
            case "i32_at" ->
                // read four bytes at the given position as a signed integer.
                    intReader(bytes, "i32_at", 4, true);
            case "u64_at" ->
                // read eight bytes at the given position as an unsigned integer.
                // A value above the signed 64-bit range is an error, because Turicum integers are signed 64-bit values.
                    intReader(bytes, "u64_at", 8, false);
            case "i64_at" ->
                // read eight bytes at the given position as a signed integer.
                    intReader(bytes, "i64_at", 8, true);
            case "set_u8_at" ->
                // write one byte at the given position, in place. The value must be in the range 0..255.
                    intWriter(bytes, "set_u8_at", 1, false);
            case "set_i8_at" ->
                // write one byte at the given position, in place. The value must be in the range -128..127.
                    intWriter(bytes, "set_i8_at", 1, true);
            case "set_u16_at" ->
                // write a value as two bytes at the given position, in place.
                // The optional third argument is the byte order, as for `u16_at`.
                    intWriter(bytes, "set_u16_at", 2, false);
            case "set_i16_at" ->
                // write a signed value as two bytes at the given position, in place.
                    intWriter(bytes, "set_i16_at", 2, true);
            case "set_u32_at" ->
                // write a value as four bytes at the given position, in place.
                    intWriter(bytes, "set_u32_at", 4, false);
            case "set_i32_at" ->
                // write a signed value as four bytes at the given position, in place.
                    intWriter(bytes, "set_i32_at", 4, true);
            case "set_u64_at" ->
                // write a non-negative value as eight bytes at the given position, in place.
                    intWriter(bytes, "set_u64_at", 8, false);
            case "set_i64_at" ->
                // write a signed value as eight bytes at the given position, in place.
                    intWriter(bytes, "set_i64_at", 8, true);
            // end snippet
            default -> null;
        };
    }

    /**
     * Creates the reader method for one of the {@code uXX_at}/{@code iXX_at} codecs.
     * The method takes the position and, for multibyte widths, an optional byte-order string.
     */
    private static TuriMethod<Object> intReader(byte[] bytes, String name, int n, boolean signed) {
        return new TuriMethod<>((args) -> {
            final int maxArgs = n == 1 ? 1 : 2;
            ExecutionException.when(args.length < 1 || args.length > maxArgs,
                    "%s() needs a position%s", name, n == 1 ? "" : " and an optional byte order");
            final int pos = Cast.toInteger(args[0]);
            final var order = BinUtils.order(args.length == 2 ? "" + args[1] : "be", n);
            return BinUtils.readInt(bytes, pos, n, order, signed);
        });
    }

    /**
     * Creates the writer method for one of the {@code set_uXX_at}/{@code set_iXX_at} codecs.
     * The method takes the position, the value and, for multibyte widths, an optional byte-order
     * string; it writes in place and returns the bin itself.
     */
    private static TuriMethod<Object> intWriter(byte[] bytes, String name, int n, boolean signed) {
        return new TuriMethod<>((args) -> {
            final int maxArgs = n == 1 ? 2 : 3;
            ExecutionException.when(args.length < 2 || args.length > maxArgs,
                    "%s() needs a position and a value%s", name, n == 1 ? "" : " and an optional byte order");
            final int pos = Cast.toInteger(args[0]);
            final long value = Cast.toLong(args[1]);
            final var order = BinUtils.order(args.length == 3 ? "" + args[2] : "be", n);
            BinUtils.writeInt(bytes, pos, n, order, value, signed);
            return bytes;
        });
    }

    private static byte[] digest(byte[] bytes, String algorithm) throws Exception {
        return MessageDigest.getInstance(algorithm).digest(bytes);
    }

    /**
     * Converts the single search argument of {@code index_of}, {@code contains} etc. to a byte
     * sequence: a bin argument is used as is, an integer argument becomes a one-byte sequence.
     */
    private static byte[] needle(String name, Object[] args) {
        ExecutionException.when(args.length == 0, "%s() needs an argument", name);
        if (args[0] instanceof byte[] b) {
            return b;
        }
        if (Cast.isLong(args[0])) {
            return new byte[]{BinUtils.toByte(args[0])};
        }
        throw new ExecutionException("The argument to %s() has to be a bin or a byte value", name);
    }

    private static byte[] binArg(String name, Object[] args) {
        if (args.length == 1 && args[0] instanceof byte[] b) {
            return b;
        }
        throw new ExecutionException("%s() needs one bin argument", name);
    }

    private static Charset charset(String name, Object[] args) {
        if (args == null || args.length == 0) {
            return StandardCharsets.UTF_8;
        }
        ExecutionException.when(args.length > 1, "%s() needs at most one argument, the name of the character encoding", name);
        return Charset.forName("" + args[0]);
    }
}
