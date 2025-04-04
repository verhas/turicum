package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngObject;

public record EmptyObject() implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return new LngObject(null, ctx.open());
    }
}

