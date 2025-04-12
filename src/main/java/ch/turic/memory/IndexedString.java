package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A wrapper object to handle string indexing and slicing. Note, however, that you cannot assign value to a character in
 * the original string, therefore, the assignment HAS TO replace the string in the context assigned to the left value
 * after the character was changed in the COPY of the string calling the {@link #setIndex(Object, Object)} method.
 *
 * @param string
 */
public record IndexedString(StringBuilder string) implements HasIndex {

    public IndexedString(final String s) {
        this(new StringBuilder(s));
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        final int start;
        final int end;
        if (Cast.isLong(index)) {
            start = Cast.toLong(index).intValue();
            ExecutionException.when(start < 0 || start >= string.length(), "String indexing error, %d is out of array range", start);
            end = start + 1;
        } else if (index instanceof Range(Object rangeStart, Object rangeEnd)) {
            start = getStart(string, rangeStart);
            end = getEnd(string, rangeEnd);
        } else {
            throw new ExecutionException("Cannot use '%s' as index", index);
        }
        final var sb = new StringBuilder();
        if( start > 0 ){
            sb.append(string, 0, start);
        }
        sb.append(value);
        if( end < string.length() ){
            sb.append(string, end, string.length());
        }
        string.setLength(0);
        string.append(sb);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        final int start;
        final int end;
        if (Cast.isLong(index)) {
            start = Cast.toLong(index).intValue();
            ExecutionException.when(start < 0 || start >= string.length(), "String indexing error, %d is out of array range", start);
            end = start + 1;
        } else if (index instanceof Range(Object rangeStart, Object rangeEnd)) {
            start = getStart(string, rangeStart);
            end = getEnd(string, rangeEnd);
        } else {
            throw new ExecutionException("Cannot use '%s' as index", index);
        }
        return string.substring(start, end);
    }

    private static int getStart(StringBuilder string, Object rangeStart) {
        if (rangeStart == InfiniteValue.INF_NEGATIVE) {
            return inRange(string, 0);
        } else if (Cast.isLong(rangeStart)) {
            return inRange(string, Cast.toLong(rangeStart).intValue());
        }
        throw new ExecutionException("Cannot use '%s' as index", rangeStart);
    }

    private static int getEnd(StringBuilder string, Object rangeStart) {
        if (rangeStart == InfiniteValue.INF_POSITIVE) {
            return string.length();
        } else if (Cast.isLong(rangeStart)) {
            final var end = Cast.toLong(rangeStart).intValue();
            if (end < 0) {
                return inRange(string, string.length() + end);
            } else {
                inRange(string, end-1);
                return end;
            }
        }
        throw new ExecutionException("Cannot use '%s' as index", rangeStart);
    }

    private static int inRange(StringBuilder string, int index) {
        ExecutionException.when(index < 0 || index >= string.length(), "String indexing error, %d is out of array range", index);
        return index;
    }

    @Override
    public Iterator<Object> iterator() {
        final var strings = new ArrayList<>();
        for (char ch : string.toString().toCharArray()) {
            strings.add("" + ch);
        }
        return strings.iterator();
    }
}
