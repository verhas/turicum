package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

public interface HasFields {
    public void setField(String name, Object value) throws ExecutionException;
    public Object getField(String name) throws ExecutionException;
}
