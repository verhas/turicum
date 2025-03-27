package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;

public interface HasFields {
    void setField(String name, Object value) throws ExecutionException;
    Object getField(String name) throws ExecutionException;
}
