package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;
import javax0.turicum.commands.operators.Cast;

import java.util.Arrays;
import java.util.Iterator;

public class JavaArray implements HasIndex{
    private final Object[] values;

    public JavaArray(Object[] values) {
        this.values = values;
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        ExecutionException.when(!Cast.isLong(index),"Cannot use '%s' as index", index);
        int indexValue = Cast.toLong(index).intValue();
        ExecutionException.when( indexValue < 0 || indexValue >= values.length, "Indexing error, %d is out of array range",indexValue);
        values[indexValue] = value;
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        ExecutionException.when(!Cast.isLong(index),"Cannot use '%s' as index", index);
        int indexValue = Cast.toLong(index).intValue();
        ExecutionException.when( indexValue < 0 || indexValue >= values.length, "Indexing error, %d is out of array range",indexValue);
        return values[indexValue];
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(values).iterator();
    }
}
