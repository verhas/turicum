package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;

import java.util.ArrayList;
import java.util.Iterator;

public class LngList implements HasIndex, HasFields {

    public final ArrayList<Object> array = new ArrayList<>();
    private int offset = 0;

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        assertNumerical(index);
        final var indexValue = Cast.toLong(index).intValue();
        if (indexValue < 0) {
            ExecutionException.when(array.size() + indexValue < offset, "Indexing error, %d is too small, %s-%s < %s.", indexValue,
                array.size(), -indexValue, offset);
            array.set(array.size() + indexValue, value);
        }
        ExecutionException.when(indexValue < 0, "Indexing error, %d is negative", indexValue);
        final var realIndex = indexValue - offset;
        array.ensureCapacity(realIndex + 1);
        while (realIndex >= array.size()) {
            array.add(null);
        }
        array.set(realIndex, value);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        assertNumerical(index);
        final var indexValue = Cast.toLong(index).intValue();
        ExecutionException.when(indexValue < offset, "Indexing error, %d < ", indexValue, offset);
        if (indexValue >= offset + array.size()) {
            return null;
        } else {
            return array.get(indexValue - offset);
        }
    }

    private void assertNumerical(Object index) {
        ExecutionException.when(!Cast.isLong(index), "List index cannot be cast to numeric value");
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        switch (name) {
            case "length":
                throw new ExecutionException("Length field cannot be set");
            case "offset":
                ExecutionException.when(Cast.isLong(value), "Array offset must be numeric.");
                offset = Cast.toLong(value).intValue();
                break;
            default:
                throw new ExecutionException(name + " is not a valid field name for an array");
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "length" -> array.size() - offset;
            case "offset" -> offset;
            default -> throw new ExecutionException(name + " is not a valid field name for an array");
        };
    }

    @Override
    public Iterator<Object> iterator() {
        return array.iterator();
    }

    @Override
    public String toString() {
        return array.toString();
    }

}
