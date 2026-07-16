package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.utils.BinUtils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A wrapper object to handle {@code bin} (byte array) indexing and slicing.
 * <p>
 * Unlike {@link IndexedString}, the wrapped {@code byte[]} is mutable: a single-index assignment
 * writes into the array in place and is visible through every reference to the same array.
 * A range assignment may change the length, which a Java array cannot; in that case a new array
 * is created and {@link ArrayElementLeftValue} assigns it back to the left value, the same way
 * modified strings are propagated. The {@link #wasReplaced()} method tells the two cases apart.
 */
public final class IndexedBin implements HasIndex {
    private byte[] bytes;
    private boolean replaced = false;

    public IndexedBin(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * @return the current array; a new array when a range assignment replaced the original
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * @return {@code true} when a range assignment created a new array that has to be assigned
     * back to the left value
     */
    public boolean wasReplaced() {
        return replaced;
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        if (Cast.isLong(index)) {
            final int i = Cast.toInteger(index);
            ExecutionException.when(i < 0 || i >= bytes.length, "Bin indexing error, %d is out of array range", i);
            bytes[i] = BinUtils.toByte(value);
            return;
        }
        if (index instanceof Range(Object rangeStart, Object rangeEnd)) {
            final int start = getStart(bytes, rangeStart);
            final int end = getEnd(bytes, rangeEnd);
            if (!(value instanceof byte[] replacement)) {
                throw new ExecutionException("Only a bin value can be assigned to a bin range, got '%s'", value);
            }
            final var combined = new byte[start + replacement.length + bytes.length - end];
            System.arraycopy(bytes, 0, combined, 0, start);
            System.arraycopy(replacement, 0, combined, start, replacement.length);
            System.arraycopy(bytes, end, combined, start + replacement.length, bytes.length - end);
            bytes = combined;
            replaced = true;
            return;
        }
        throw new ExecutionException("Cannot use '%s' as index", index);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        if (Cast.isLong(index)) {
            final int i = Cast.toInteger(index);
            ExecutionException.when(i < 0 || i >= bytes.length, "Bin indexing error, %d is out of array range", i);
            return BinUtils.unsigned(bytes[i]);
        }
        if (index instanceof Range(Object rangeStart, Object rangeEnd)) {
            final int start = getStart(bytes, rangeStart);
            final int end = getEnd(bytes, rangeEnd);
            final var slice = new byte[end - start];
            System.arraycopy(bytes, start, slice, 0, end - start);
            return slice;
        }
        throw new ExecutionException("Cannot use '%s' as index", index);
    }

    private static int getStart(byte[] bytes, Object rangeStart) {
        if (rangeStart == InfiniteValue.INF_NEGATIVE) {
            return 0;
        } else if (Cast.isLong(rangeStart)) {
            return inRange(bytes, Cast.toInteger(rangeStart));
        }
        throw new ExecutionException("Cannot use '%s' as index", rangeStart);
    }

    private static int getEnd(byte[] bytes, Object rangeEnd) {
        if (rangeEnd == InfiniteValue.INF_POSITIVE) {
            return bytes.length;
        } else if (Cast.isLong(rangeEnd)) {
            final var end = Cast.toInteger(rangeEnd);
            if (end < 0) {
                return inRange(bytes, bytes.length + end);
            } else {
                inRange(bytes, end - 1);
                return end;
            }
        }
        throw new ExecutionException("Cannot use '%s' as index", rangeEnd);
    }

    private static int inRange(byte[] bytes, int index) {
        ExecutionException.when(index < 0 || index >= bytes.length, "Bin indexing error, %d is out of array range", index);
        return index;
    }

    @Override
    public Iterator<Object> iterator() {
        final var values = new ArrayList<>();
        for (final byte b : bytes) {
            values.add(BinUtils.unsigned(b));
        }
        return values.iterator();
    }
}
