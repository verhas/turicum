package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.commands.operators.Cast;

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
}
