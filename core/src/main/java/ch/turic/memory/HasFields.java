package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.Set;

public interface HasFields {
    void setField(String name, Object value) throws ExecutionException;
    Object getField(String name) throws ExecutionException;
    Set<String> fields();
}
