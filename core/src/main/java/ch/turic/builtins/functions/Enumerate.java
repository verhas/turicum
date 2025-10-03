package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import java.util.Iterator;

/**
 * Implementation of the {@code enumerate} function for the Turicum programming language.
 *
 * <p>The {@code enumerate} function takes an iterable object and returns a new iterable
 * that produces pairs (tuples) containing an index and the corresponding element from
 * the original iterable. The index starts at 0 and increments for each element.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Returns an iterable object that you can iterate through
 * die "" when str(enumerate(["a","b","c"])) != "EnumeratorIterable[iterable=[a, b, c]]"
 *
 * // Iterate through enumerated pairs
 * let z = {for each k in enumerate(["a","b","c"]) list:
 *   k}
 * die "" when str(z) != "[[0, a], [1, b], [2, c]]"
 * // you can also create a list from the iterable, if you must
 * die "" when str([..enumerate(["a","b","c"])]) != "[[0, a], [1, b], [2, c]]"
 * }</pre>
 */
public class Enumerate implements TuriFunction {

    @Override
    public String name() {
        return "enumerate";
    }

    private record EnumeratorIterable(Iterable<?> iterable) implements Iterable<LngList> {


        @Override
            public Iterator<LngList> iterator() {
                return new EnumeratorIterator(iterable.iterator());
            }

            private static class EnumeratorIterator implements Iterator<LngList> {
                private int i = 0;
                private final Iterator<?> iterator;

                private EnumeratorIterator(Iterator<?> iterator) {
                    this.iterator = iterator;
                }

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public LngList next() {
                    return LngList.of(i++, iterator.next());
                }
            }

        }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);

        if (arg instanceof Iterable<?> iterable) {
            return new EnumeratorIterable(iterable);
        } else {
            throw new ExecutionException("Cannot enumerate the value of %s", arg);
        }
    }
}
