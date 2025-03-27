package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;
import javax0.turicum.commands.operators.Cast;

public interface HasIndex extends Iterable<Object> {
    void setIndex(Object index, Object value) throws ExecutionException;

    Object getIndex(Object index) throws ExecutionException;

    static HasIndex createFor(Object indexValue, Context ctx) {
        final HasIndex newIndexable;
        if (Cast.isLong(indexValue)) {
            newIndexable = new LngList();
        } else {
            newIndexable = new LngObject(null, ctx.open());
        }
        return newIndexable;
    }
}
