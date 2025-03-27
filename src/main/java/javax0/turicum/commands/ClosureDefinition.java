package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public record ClosureDefinition( String[] arguments, BlockCommand body) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        return new Closure(arguments,context,body);
    }
}
