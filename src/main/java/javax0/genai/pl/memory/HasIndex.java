package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

public interface HasIndex {
    public void setIndex(Object index, Object value) throws ExecutionException;
    public Object getIndex(Object index) throws ExecutionException;
}
