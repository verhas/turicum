package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;
import ch.turic.memory.Range;

import java.util.Iterator;
/*snippet builtin0330

end snippet */

/**
 * Return a range of numbers.
 * <p>
 * The call {@code rng(start, stop, step)} will return a range of numbers starting with {@code start} and ending
 * before {@code stop}. That way, the interval is closed at the start and open at the end. Includes {@code start},
 * but excludes {@code end}.
 * <p>
 * If {@code start} and {@code stop} are the same value, and {@code step} is zero or missing, then the range will
 * contain one single number: {@code start}.
 * <p>
 * Prividing a {@code step} value is optional. If it is missing, it will be +1, 0 or -1 depending on the {@code start}
 * and {@code stop} value.
 * <p>
 * If the {@code start} and {@code stop} values are not the same it is an error specifying a zero value for {@code step}.
 * <p>
 * The object is dynamic. It is not a list. It is an iterable that you can loop through.
 */
public class Rng implements TuriFunction {

    public static final class LongRange extends LngList {
        private final long start, end, step;
        private final long length;

        public LongRange(long start, long end, long step) {
            if (step == 0) {
                throw new ExecutionException("rng(%s,%s,%s) has zero step size", start, end, step);
            }
            this.start = start;
            this.end = end;
            this.step = step;
            length = (end - start) / step;
        }

        @Override
        public Object getField(String name) throws ExecutionException {
            return switch (name) {
                case "length" -> length;
                default -> throw new ExecutionException(name + " is not a valid field name for an array");
            };
        }

        @Override
        public Object getIndex(Object index) throws ExecutionException {
            if (Cast.isLong(index)) {
                final var indexValue = Cast.toLong(index).intValue();
                ExecutionException.when(indexValue < 0, "Indexing error, %s < 0", indexValue);
                if (indexValue >= length) {
                    return null;
                } else {
                    return start + indexValue * step;
                }
            }
            if (index instanceof Range range) {
                final var newStart = range.getStart((int) length);
                final var newEnd = range.getEnd((int) length);
                return new LongRange(start + newStart * step, start + newEnd * step, step);
            }
            throw new ExecutionException("You cannot use '%s' as index", index);
        }

        private static class LongIterator implements Iterator<Object> {
            private long current;
            private final long end, step;

            private LongIterator(long start, long end, long step) {
                this.current = start;
                this.end = end;
                this.step = step;
            }

            @Override
            public boolean hasNext() {
                if (step < 0) {
                    return current > end;
                } else {
                    return current < end;
                }
            }

            @Override
            public Long next() {
                long result = current;
                current = current + step;
                return result;
            }
        }

        @Override
        public Iterator<Object> iterator() {
            return new LongIterator(start, end, step);
        }

        @Override
        public void setField(String name, Object value) throws ExecutionException {
            throw new ExecutionException(name + " cannot be set on a number range");
        }

        @Override
        public void setIndex(Object index, Object value) throws ExecutionException {
            throw new ExecutionException(index + " cannot be set on a number range");
        }

        @Override
        public String toString() {
            if (length <= 5) {
                final var builder = new StringBuilder("[");
                final String[] sep = {""};
                new LongIterator(start, end, step).forEachRemaining(
                        x -> {
                            builder.append(sep[0]).append(x);
                            sep[0] = ", ";
                        }
                );
                builder.append(']');
                return builder.toString();
            }
            return "[" + start + ", " +
                    (start + step) + ", " +
                    (start + 2 * step) + ",..., " +
                    (start + (length - 2) * step) + ", " +
                    (start + (length - 1) * step) + "]";
        }
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if (arguments.length > 3) {
            throw new ExecutionException("%s expects two or three arguments", name());
        }
        final var start = Cast.toLong(arguments[0]);
        final var end = Cast.toLong(arguments[1]);
        final var step = arguments.length > 2 ? Cast.toLong(arguments[2]) : (end.compareTo(start));
        if ((end > start && step < 0) || (end < start && step > 0)) {
            throw new ExecutionException("rng(%s,%s,%s) would never end", start, end, step);
        }
        if (step == 0 && start.equals(end)) {
            return new LongRange(start, end + 1, 1);
        }
        return new LongRange(start, end, step);
    }
}
