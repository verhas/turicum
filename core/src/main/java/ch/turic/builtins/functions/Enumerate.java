package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Implementation of the {@code enumerate} function for the Turicum programming language.
 *
 * <p>The {@code enumerate} function takes an iterable object and returns a new iterable
 * that produces pairs (tuples) containing an index and the corresponding element from
 * the original iterable. The index starts at 0 and increments for each element.</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Print all enumerated pairs
 * println [..enumerate(["a","b","c"])]
 * // Output: [[0,"a"], [1,"b"], [2,"c"]]
 *
 * // Iterate through enumerated pairs
 * for each k in enumerate(["a","b","c"]):
 *   println k
 * // Output:
 * // [0,"a"]
 * // [1,"b"]
 * // [2,"c"]
 * }</pre>
 */
public class Enumerate implements TuriFunction {

    @Override
    public String name() {
        return "enumerate";
    }

    private static class EnumeratorIterable implements Iterable<LngList>{
        private final Iterable<?> iterable;

        private EnumeratorIterable(Iterable<?> iterable) {
            this.iterable = iterable;
        }


        @Override
        public Iterator<LngList> iterator() {
            return new EnumeratorIterator(iterable.iterator());
        }

        private static class EnumeratorIterator implements Iterator<LngList>{
            private int i=0;
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
