package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;

public interface LngCallable {
    Object call(Context ctx, Object[] arguments)throws ExecutionException;
}
