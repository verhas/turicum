package ch.turic.utils;

import ch.turic.exceptions.ExecutionException;

/**
 * Utility methods for the {@code bin} type, whose runtime value is a raw Java {@code byte[]}.
 * <p>
 * The language surface of a {@code bin} is unsigned: reading a byte yields a value in the
 * {@code 0..255} range, writing accepts {@code -128..255} and stores the low 8 bits.
 * The helpers here implement that convention plus the textual representation and the
 * byte-order handling of the integer codec methods.
 */
public class BinUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * Converts the byte array to its lowercase hexadecimal representation, two digits per byte,
     * without any separator.
     *
     * @param bytes the bytes to convert
     * @return the hexadecimal string
     */
    public static String hex(byte[] bytes) {
        final var sb = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            sb.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array. An optional {@code 0x} or {@code 0X} prefix
     * is accepted; the number of digits has to be even.
     *
     * @param hexString the string to parse
     * @return the parsed bytes
     * @throws ExecutionException when the string is not a valid even-length hexadecimal string
     */
    public static byte[] parseHex(String hexString) {
        var s = hexString.strip();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        ExecutionException.when(s.length() % 2 != 0, "Hex string needs an even number of digits, got %d", s.length());
        final var bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            final int hi = Character.digit(s.charAt(2 * i), 16);
            final int lo = Character.digit(s.charAt(2 * i + 1), 16);
            ExecutionException.when(hi < 0 || lo < 0, "'%s' is not a valid hex string", hexString);
            bytes[i] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }

    /**
     * The textual representation of a {@code bin} value: {@code bin"..."} with the content in hex.
     *
     * @param bytes the value to display
     * @return the display string, for example {@code bin"48656c6c6f"}
     */
    public static String display(byte[] bytes) {
        return "bin\"" + hex(bytes) + "\"";
    }

    /**
     * Converts a language value to a single byte. Accepts integer values in the {@code -128..255}
     * range and stores the low 8 bits; anything else is an error.
     *
     * @param value the value to convert
     * @return the byte
     * @throws ExecutionException when the value is not an integer or is out of range
     */
    public static byte toByte(Object value) {
        if (!(value instanceof Number n) || value instanceof Double || value instanceof Float) {
            throw new ExecutionException("Byte value must be an integer number, got '%s'", value);
        }
        final long v = n.longValue();
        ExecutionException.when(v < -128 || v > 255, "Byte value %d is out of the -128..255 range", v);
        return (byte) v;
    }

    /**
     * The unsigned value of a byte as a language integer.
     *
     * @param b the byte
     * @return the value in the 0..255 range as a {@code Long}
     */
    public static Long unsigned(byte b) {
        return (long) (b & 0xFF);
    }

    /**
     * Finds the first occurrence of a byte sequence.
     *
     * @param haystack the array to search in
     * @param needle   the sequence to search for
     * @param from     the index where the search starts
     * @return the index of the first occurrence at or after {@code from}, or -1
     */
    public static int indexOf(byte[] haystack, byte[] needle, int from) {
        if (needle.length == 0) {
            return Math.min(Math.max(from, 0), haystack.length);
        }
        outer:
        for (int i = Math.max(from, 0); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Finds the last occurrence of a byte sequence.
     *
     * @param haystack the array to search in
     * @param needle   the sequence to search for
     * @param from     the highest index where the occurrence may start
     * @return the index of the last occurrence starting at or before {@code from}, or -1
     */
    public static int lastIndexOf(byte[] haystack, byte[] needle, int from) {
        if (needle.length == 0) {
            return Math.min(Math.max(from, 0), haystack.length);
        }
        outer:
        for (int i = Math.min(from, haystack.length - needle.length); i >= 0; i--) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Parses a byte-order specification for the {@code uXX_at}/{@code iXX_at} codec methods.
     * <p>
     * The specification is {@code "be"} (most significant byte first), {@code "le"} (least
     * significant byte first), or a permutation string of the digits {@code 1..n}, each exactly
     * once, listing the bytes in memory order where digit {@code 1} is the least significant
     * byte. For example, for 32-bit values {@code "1234"} is the same as {@code "le"},
     * {@code "4321"} is the same as {@code "be"}, and {@code "3412"} is the PDP-11 middle-endian
     * layout.
     *
     * @param spec the order specification
     * @param n    the number of bytes of the integer
     * @return an array where element {@code k} is the zero-based significance of the byte at
     * memory offset {@code k} (0 = least significant byte)
     * @throws ExecutionException when the specification is malformed
     */
    public static int[] order(String spec, int n) {
        final var significance = new int[n];
        switch (spec) {
            case "le" -> {
                for (int i = 0; i < n; i++) {
                    significance[i] = i;
                }
            }
            case "be" -> {
                for (int i = 0; i < n; i++) {
                    significance[i] = n - 1 - i;
                }
            }
            default -> {
                ExecutionException.when(spec.length() != n,
                        "Byte order '%s' must be 'be', 'le' or a permutation of the digits 1..%d", spec, n);
                final var seen = new boolean[n];
                for (int i = 0; i < n; i++) {
                    final int digit = spec.charAt(i) - '1';
                    ExecutionException.when(digit < 0 || digit >= n,
                            "Byte order '%s' contains an invalid digit, it must be a permutation of the digits 1..%d", spec, n);
                    ExecutionException.when(seen[digit],
                            "Byte order '%s' contains the digit %d more than once", spec, digit + 1);
                    seen[digit] = true;
                    significance[i] = digit;
                }
            }
        }
        return significance;
    }

    /**
     * Reads an integer of {@code n} bytes from the array.
     *
     * @param bytes        the array to read from
     * @param pos          the position of the first byte
     * @param n            the number of bytes, 1..8
     * @param significance the byte order as returned by {@link #order(String, int)}
     * @param signed       {@code true} to sign-extend (two's complement), {@code false} for unsigned
     * @return the value
     * @throws ExecutionException when the range is out of the array, or an unsigned 64-bit value
     *                            does not fit into the signed language integer
     */
    public static long readInt(byte[] bytes, int pos, int n, int[] significance, boolean signed) {
        checkRange(bytes, pos, n);
        long value = 0;
        for (int k = 0; k < n; k++) {
            value |= (long) (bytes[pos + k] & 0xFF) << (8 * significance[k]);
        }
        if (signed) {
            if (n < 8) {
                final long signBit = 1L << (8 * n - 1);
                if ((value & signBit) != 0) {
                    value -= 1L << (8 * n);
                }
            }
        } else {
            ExecutionException.when(n == 8 && value < 0,
                    "u64 value is above the signed 64-bit integer range");
        }
        return value;
    }

    /**
     * Writes an integer of {@code n} bytes into the array in place.
     *
     * @param bytes        the array to write into
     * @param pos          the position of the first byte
     * @param n            the number of bytes, 1..8
     * @param significance the byte order as returned by {@link #order(String, int)}
     * @param value        the value to write
     * @param signed       selects the accepted value range: signed {@code -2^(8n-1)..2^(8n-1)-1}
     *                     or unsigned {@code 0..2^(8n)-1} (for 8 bytes unsigned: non-negative)
     * @throws ExecutionException when the range is out of the array or the value does not fit
     */
    public static void writeInt(byte[] bytes, int pos, int n, int[] significance, long value, boolean signed) {
        checkRange(bytes, pos, n);
        if (n < 8) {
            final long min = signed ? -(1L << (8 * n - 1)) : 0;
            final long max = signed ? (1L << (8 * n - 1)) - 1 : (1L << (8 * n)) - 1;
            ExecutionException.when(value < min || value > max,
                    "Value %d is out of the %s%d range %d..%d", value, signed ? "i" : "u", 8 * n, min, max);
        } else {
            ExecutionException.when(!signed && value < 0, "Value %d is out of the u64 range", value);
        }
        for (int k = 0; k < n; k++) {
            bytes[pos + k] = (byte) (value >>> (8 * significance[k]));
        }
    }

    private static void checkRange(byte[] bytes, int pos, int n) {
        ExecutionException.when(pos < 0 || pos + n > bytes.length,
                "Position %d with %d bytes is out of the bin of length %d", pos, n, bytes.length);
    }
}
