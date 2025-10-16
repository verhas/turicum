package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;

/**
 * The HasIndex interface provides methods for setting, retrieving, and iterating over values
 * based on an index. It is designed to represent a structure where elements can be accessed
 * and manipulated via indices. The interface extends {@code Iterable<Object>}, enabling iteration
 * over its elements.
 */
public interface HasIndex extends Iterable<Object> {
    void setIndex(Object index, Object value) throws ExecutionException;

    Object getIndex(Object index) throws ExecutionException;

    static HasIndex createFor(Object indexValue, LocalContext ctx) {
        final HasIndex newIndexable;
        if (Cast.isLong(indexValue)) {
            newIndexable = new LngList();
        } else {
            newIndexable = LngObject.newEmpty(ctx);
        }
        return newIndexable;
    }

}
