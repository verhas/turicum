package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RangeTest {

    private static final int SIZE = 10;

    @Test
    void testPositiveIndicesWithinBounds() {
        Range range = new Range(2L, 5L);
        assertEquals(2, range.getStart(SIZE));
        assertEquals(5, range.getEnd(SIZE));
    }

    @Test
    void testNegativeIndices() {
        Range range = new Range(-3L, -1L);
        assertEquals(7, range.getStart(SIZE)); // 10 - 3
        assertEquals(9, range.getEnd(SIZE));   // 10 - 1
    }

    @Test
    void testStartIndexNegativeBeyondBoundsThrows() {
        Range range = new Range(-15L, 5L); // -15 + 10 = -5 -> invalid
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> range.getStart(SIZE));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void testEndIndexNegativeBeyondBoundsThrows() {
        Range range = new Range(0L, -15L);
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> range.getEnd(SIZE));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void testStartGreaterThanSizeThrows() {
        Range range = new Range(15L, 16L);
        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> range.getStart(SIZE));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void testEndGreaterThanSizeAllowed() {
        Range range = new Range(0L, 10L); // end == size → allowed
        assertEquals(10, range.getEnd(SIZE));
    }

    @Test
    void testStartEqualsSizeAllowed() {
        Range range = new Range(10L, 10L); // empty range
        assertEquals(10, range.getStart(SIZE));
    }

    @Test
    void testInfiniteValues() {
        Range range = new Range(InfiniteValue.INF_NEGATIVE, InfiniteValue.INF_POSITIVE);
        assertEquals(0, range.getStart(SIZE));
        assertEquals(10, range.getEnd(SIZE));
    }

    @Test
    void testInfiniteEndNegative() {
        Range range = new Range(0L, InfiniteValue.INF_NEGATIVE);
        assertEquals(-1, range.getEnd(SIZE));
    }

    @Test
    void testToStringFormatting() {
        Range range = new Range(3L, 7L);
        assertEquals("3..7", range.toString());
    }

    @Test
    void testInvalidIndexTypeThrows() {
        Range range = new Range("foo", 5L);
        assertThrows(ExecutionException.class, () -> range.getStart(SIZE));
    }

    @Test
    void testEmptyRange() {
        Range range = new Range(5L, 5L);
        assertEquals(5, range.getStart(SIZE));
        assertEquals(5, range.getEnd(SIZE));
        assertTrue(range.getStart(SIZE) >= range.getEnd(SIZE));
    }
}
