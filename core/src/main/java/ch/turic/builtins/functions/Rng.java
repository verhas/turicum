package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;
import ch.turic.memory.Range;

import java.util.Iterator;
/*snippet builtin0330

=== `rng`

Returns a number range.

[source]
----
rng(start, end, step)
----
or

[source]
----
rng(start, end)
----

It is a read-only list containing all numbers from the start (inclusive) to the end (exclusive) with the given step.
It does not create an actual list, but instead provides access to the numbers in the range by index.
You can also iterate through the elements.

The following example demonstrates that you can have extensive ranges.

{%S rng%}

If you want a physical list with the elements, you can create it using the `..` spread operator:

{%S rngThousand%}

In this case, `array` will be a list containing the elements we print out.
Avoid using this with too large numbers unless you want to test for out-of-memory errors.

You can index a number range, and you can also use a range to index another range:

{%S rngRange%}

In this case, you index the range without spreading it into a list.
We only spread the new range for printing.
The `range` object does not allocate nearly two million elements.
It is simply another number range.
At the end, the code allocates a list of ten elements for the printout.

The third parameter `step` is optional.
If not specified, it will be `+1`, `0`, or `-1` depending on the start and the end value.

If `start` is larger than `end`, the step value must be negative.
The default value is `-1` in this case.

If `start` is smaller than `end`, the step value must be positive.
The default value is `+1` in this case.

When the `start` and `end` values are the same, the range will

* be empty if the step is specified and is not zero (positive or negative), and
* contain the one single `start`/`end` value if the step is zero.

The default value for `step` is `0` in this case.


If specified, it must not be zero, and it has to be positive or negative based on the relations between the start and end value.
Essentially, stepping has to approach the end value.

{%S rngErrs%}

It is recommended not to use the `rng()` function directly.
You can import the system module `range` that defines a more flexible name:

{%S range%}

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
        public long size(){
            return length;
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
