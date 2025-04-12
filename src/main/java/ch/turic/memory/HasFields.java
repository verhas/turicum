package ch.turic.memory;

import ch.turic.ExecutionException;

public interface HasFields {
    void setField(String name, Object value) throws ExecutionException;
    Object getField(String name) throws ExecutionException;
}
