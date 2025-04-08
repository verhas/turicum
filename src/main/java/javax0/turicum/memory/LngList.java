package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;

import java.util.ArrayList;
import java.util.Iterator;

public class LngList implements HasIndex, HasFields {

    public final ArrayList<Object> array = new ArrayList<>();

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        if (Cast.isLong(index)) {
            final var indexValue = Cast.toLong(index).intValue();
            if (indexValue < 0) {
                ExecutionException.when(array.size() + indexValue < 0, "Indexing error, %d is too small, %s-%s < %s.", indexValue,
                        array.size(), -indexValue, 0);
                array.set(array.size() + indexValue, value);
                return;
            }
            array.ensureCapacity(indexValue + 1);
            while (indexValue >= array.size()) {
                array.add(null);
            }
            array.set(indexValue, value);
            return;
        }
        if (index instanceof Range range) {
            final var start = range.getStart(array.size());
            final var end = range.getEnd(array.size());
            if( end < start ) {
                throw new ExecutionException("reverse range [%d .. %d] cannot be set.", start, end);
            }
            if (value instanceof Iterable<?> iterable) {
                final var result = new ArrayList<>();
                int i = 0;
                for (; i < start; i++) {
                    result.add(array.get(i));
                }
                for (final var obj : iterable) {
                    result.add(obj);
                }
                for (i = end; i < array.size(); i++) {
                    result.add(this.array.get(i));
                }
                array.clear();
                array.addAll(result);
            } else {
                throw new ExecutionException("You cannot insert '%s' into a list", value);
            }
            return;
        }
        throw new ExecutionException("You cannot use '%s' as index", index);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        if (Cast.isLong(index)) {
            final var indexValue = Cast.toLong(index).intValue();
            ExecutionException.when(indexValue < 0, "Indexing error, %s < 0", indexValue);
            if (indexValue >= array.size()) {
                return null;
            } else {
                return array.get(indexValue);
            }
        }
        if (index instanceof Range range) {
            final var start = range.getStart(array.size());
            final var end = range.getEnd(array.size());
            final var result = new LngList();
            for (int i = start; i < end; i++) {
                result.array.add(this.array.get(i));
            }
            return result;
        }
        throw new ExecutionException("You cannot use '%s' as index", index);
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        switch (name) {
            case "length":
                throw new ExecutionException("Length field cannot be set");
            default:
                throw new ExecutionException(name + " is not a valid field name for an array");
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "length" -> array.size();
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
