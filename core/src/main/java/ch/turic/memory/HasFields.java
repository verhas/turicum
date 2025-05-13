package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.Iterator;
import java.util.Set;

public interface HasFields extends HasIndex {
    void setField(String name, Object value) throws ExecutionException;

    Object getField(String name) throws ExecutionException;

    Set<String> fields();

    default void setIndex(Object index, Object value) throws ExecutionException {
        setField(index.toString(), value);
    }

    default Object getIndex(Object index) throws ExecutionException {
        return getField(index.toString());
    }

    default Iterator<Object> iterator() {
        return fields().stream().map(o -> (Object) o).iterator();
    }

}
