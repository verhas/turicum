package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.commands.operators.Cast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LngArray implements HasIndex {

    private enum Index {NUMERIC, SYMBOLIC}

    private final List<Object> array = new ArrayList<>();
    private final Map<Object, Object> map = new HashMap<>();
    private Index indexType = Index.NUMERIC;

    private void convertToSymbolic() {
        indexType = Index.SYMBOLIC;
        for (int i = 0; i < array.size(); i++) {
            map.put((long) i, array.get(i));
        }
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        switch (indexType) {
            case Index.SYMBOLIC:
                map.put(index, value);
                return;
            case Index.NUMERIC:
                if (Cast.isLong(index)) {
                    final var indexValue = Cast.toLong(index).intValue();
                    ExecutionException.when(indexValue < 0, "Indexing error, %d is negative", indexValue);
                    while (indexValue > array.size()) {
                        array.add(null);
                    }
                    array.set(indexValue, value);
                } else {
                    convertToSymbolic();
                    map.put(index, value);
                }
                return;
            default:
                throw new ExecutionException("Unknown index type: " + indexType);
        }
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        return switch (indexType) {
            case Index.SYMBOLIC -> map.get(index);
            case Index.NUMERIC -> {
                if (Cast.isLong(index)) {
                    final var indexValue = Cast.toLong(index).intValue();
                    ExecutionException.when(indexValue < 0, "Indexing error, %d is negative", indexValue);
                    if (indexValue > array.size()) {
                        yield null;
                    } else {
                        yield array.get(indexValue);
                    }
                } else {
                    yield null;
                }
            }
        };
    }
}
