package ch.turic.commands.operators;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CastTest {

    @Test
    void isLongAcceptsNumericTypes() {
        assertTrue(Cast.isLong(42L));
        assertTrue(Cast.isLong(42));
        assertTrue(Cast.isLong((short) 42));
        assertTrue(Cast.isLong((byte) 42));
        assertTrue(Cast.isLong('a'));
    }

    @Test
    void isLongAcceptsNumericStrings() {
        assertTrue(Cast.isLong("42"));
        assertTrue(Cast.isLong("0"));
        assertTrue(Cast.isLong("+42"));
        assertTrue(Cast.isLong("-7"));
        assertTrue(Cast.isLong("000042"));
    }

    @Test
    void isLongHandlesLongRangeBoundary() {
        assertTrue(Cast.isLong("9223372036854775807"), "Long.MAX_VALUE is a long");
        assertTrue(Cast.isLong("-9223372036854775808"), "Long.MIN_VALUE is a long");
        assertTrue(Cast.isLong("8999999999999999999"), "19 digits below MAX_VALUE is a long");
        assertTrue(Cast.isLong("00000000000000000000042"), "leading zeros do not overflow");
        assertFalse(Cast.isLong("9223372036854775808"), "Long.MAX_VALUE+1 overflows");
        assertFalse(Cast.isLong("-9223372036854775809"), "Long.MIN_VALUE-1 overflows");
        assertFalse(Cast.isLong("99999999999999999999"), "20 digits overflow");
    }

    @Test
    void isLongRejectsNonNumericValues() {
        assertFalse(Cast.isLong(null));
        assertFalse(Cast.isLong(""));
        assertFalse(Cast.isLong("+"));
        assertFalse(Cast.isLong("-"));
        assertFalse(Cast.isLong("1a"));
        assertFalse(Cast.isLong(" 42"));
        assertFalse(Cast.isLong("4 2"));
        assertFalse(Cast.isLong("1.5"));
        assertFalse(Cast.isLong(3.14));
        assertFalse(Cast.isLong(true));
        assertFalse(Cast.isLong(new Object()));
    }

    @Test
    void isDoubleAcceptsNumericStrings() {
        assertTrue(Cast.isDouble("42"));
        assertTrue(Cast.isDouble("+42"));
        assertTrue(Cast.isDouble("-7"));
        assertTrue(Cast.isDouble("1.5"));
        assertTrue(Cast.isDouble(".5"));
        assertTrue(Cast.isDouble("1."));
        assertTrue(Cast.isDouble("+4.2"));
        assertTrue(Cast.isDouble("-0.5"));
        assertTrue(Cast.isDouble("1.5e3"));
        assertTrue(Cast.isDouble("1.5E-3"));
        assertTrue(Cast.isDouble("1.e5"));
    }

    @Test
    void isDoubleRejectsNonNumericValues() {
        assertFalse(Cast.isDouble(null));
        assertFalse(Cast.isDouble(""));
        assertFalse(Cast.isDouble("."));
        assertFalse(Cast.isDouble("+."));
        assertFalse(Cast.isDouble("-."));
        assertFalse(Cast.isDouble("+"));
        assertFalse(Cast.isDouble("1.5.3"));
        assertFalse(Cast.isDouble("1.5e"));
        assertFalse(Cast.isDouble("1.5e+"));
        assertFalse(Cast.isDouble("1.5ee3"));
        assertFalse(Cast.isDouble(".e5"));
        assertFalse(Cast.isDouble("abc"));
        // stricter than Double.parseDouble: the exponent form requires a '.' before it
        assertFalse(Cast.isDouble("1e5"));
    }

    /**
     * The contract of the is-methods: whenever the check returns {@code true}, the
     * corresponding conversion must succeed without throwing. Exercised over samples
     * that historically violated it ({@code "."}, overflowing digit strings).
     */
    @Test
    void isLongTrueImpliesToLongSucceeds() {
        final var samples = List.of(
                "42", "+42", "-7", "0", "000042",
                "9223372036854775807", "-9223372036854775808", "8999999999999999999",
                "9223372036854775808", "99999999999999999999", "00000000000000000000042",
                ".", "+.", "1.5", "", "+", "1a",
                42L, 42, (short) 42, (byte) 42, 'a', 3.14, true);
        for (final var sample : samples) {
            if (Cast.isLong(sample)) {
                assertDoesNotThrow(() -> Cast.toLong(sample),
                        "isLong is true, so toLong must not throw for: " + sample);
            }
        }
    }

    @Test
    void isDoubleTrueImpliesToDoubleSucceeds() {
        final var samples = List.of(
                "42", "+42", "-7", "1.5", ".5", "1.", "1.5e3", "1.5E-3", "1.e5",
                ".", "+.", "-.", "", "1.5e", "1e5", "abc",
                42L, 42, (short) 42, (byte) 42, 'a', 3.14, 3.14f, true);
        for (final var sample : samples) {
            if (Cast.isDouble(sample)) {
                assertDoesNotThrow(() -> Cast.toDouble(sample),
                        "isDouble is true, so toDouble must not throw for: " + sample);
            }
        }
    }

    @Test
    void toLongConvertsBoundaryValues() {
        assertEquals(Long.MAX_VALUE, Cast.toLong("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, Cast.toLong("-9223372036854775808"));
        assertEquals(42L, Cast.toLong("000042"));
    }
}
