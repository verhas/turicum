package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.commands.operators.Cast;

public interface HasIndex {
    void setIndex(Object index, Object value) throws ExecutionException;

    Object getIndex(Object index) throws ExecutionException;

    static HasIndex createFor(Object indexValue, Context ctx) {
        final HasIndex newIndexable;
        if (Cast.isLong(indexValue)) {
            newIndexable = new LngList();
            newIndexable.setIndex(indexValue, null);
        } else {
            newIndexable = new LngObject(null, ctx.open());
        }
        return newIndexable;
    }
}
