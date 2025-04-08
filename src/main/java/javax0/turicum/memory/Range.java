package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;

/**
 * An object describing a range of lists, strings or anything indexable. The interpretation of the range is up to the
 * caller, but it generally is assumed that the {@code start} and {@code end} values are either {@link Long}
 * (something that can be cast to long) or {@link InfiniteValue#INF_POSITIVE} or }{@link InfiniteValue#INF_NEGATIVE}.
 * <p>
 * Caller may interpret the range as descending if the {@code end} value is smaller than {@code start}. In this case,
 * reasonably the indexes in the range will be indexed as
 * {@code start}, {@code start}-1, {@code start}-2, ... , {@code end}+1.
 *
 * @param start the start of the range inclusive.
 *              If {@code start} is larger than the index of the last element then the range should be interpreted as
 *              empty.
 * @param end   the end of the range exclusive. The last element of the range is indexed by {@code end -1}
 */
public record Range(Object start, Object end) {

    @Override
    public String toString() {
        return start + ".." + end;
    }


    /**
     * Get the start integer value from the range 'start' object. If the value is {@link InfiniteValue#INF_POSITIVE}
     * it means the start is one after the last element. The range will return {@code size}.
     * <p>
     * If the value is negative, then it will be interpreted as indexing from the end and the returned value will be
     * the {@code start} which is negative {@code +size}. If it is still negative, an error is thrown.
     * <p>
     * It is also an error to specify a value that is larger than {@code size-1}, except the
     * {@link InfiniteValue#INF_POSITIVE} one.
     *
     * @param size the size of the indexed list, array, string, whatever indexable is
     * @return the integer value of the start
     */
    public int getStart(int size) {
        return resolve(start, size, true);
    }

    public int getEnd(int size) {
        return resolve(end, size, false);
    }

    /**
     * Resolve the integer index value from the specified range endpoint object.
     * <p>
     * The index can be a {@link Long}, or one of the special values {@link InfiniteValue#INF_POSITIVE} or
     * {@link InfiniteValue#INF_NEGATIVE}. The interpretation depends on whether the index is the start or end
     * of the range.
     * <ul>
     *     <li>If the index is {@link InfiniteValue#INF_NEGATIVE}, it resolves to {@code 0} when resolving the start,
     *     and to {@code -1} when resolving the end.</li>
     *     <li>If the index is {@link InfiniteValue#INF_POSITIVE}, it resolves to {@code size} regardless of whether it is start or end.</li>
     *     <li>If the index is a numeric value:
     *         <ul>
     *             <li>If the value is negative, it is interpreted as {@code index + size} (i.e., indexing from the end).</li>
     *             <li>If the resulting index is out of bounds (less than 0 or greater than allowed), an {@link ExecutionException} is thrown.</li>
     *             <li>For start indices, the upper bound is {@code size}; for end indices, it is {@code size + 1}.</li>
     *         </ul>
     *     </li>
     *     <li>All other types will trigger an {@link ExecutionException}.</li>
     * </ul>
     *
     * @param index   the object to resolve as an index
     * @param size    the size of the indexed structure (e.g., list, array, string)
     * @param isStart whether the index is the start of the range (affects interpretation of special values and bounds)
     * @return the resolved integer index
     * @throws ExecutionException if the index is not a valid range value
     */

    private int resolve(Object index, int size, boolean isStart) {
        if (index == InfiniteValue.INF_NEGATIVE) {
            return isStart ? 0 : -1;
        } else if (index == InfiniteValue.INF_POSITIVE) {
            return size;
        } else if (Cast.isLong(index)) {
            int i = Cast.toLong(index).intValue();
            if (i < 0) {
                i = i + size;
            }
            final var limit = isStart ? size : size + 1;
            if (i < 0 || i > limit) {
                throw new ExecutionException("Index is out of range %d", i);
            }
            return i;
        }
        throw new ExecutionException("Cannot use '%s' as index", index);
    }
}
