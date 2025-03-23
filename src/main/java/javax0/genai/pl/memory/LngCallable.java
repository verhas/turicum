package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

public interface LngCallable {
    Object call(Context ctx, Object[] arguments)throws ExecutionException;
}
