package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public record FunctionDefinition(String functionName, String[] arguments, BlockCommand body) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
            final var closure = new Closure(arguments,null,body);
            if( functionName != null ) {
                context.local(functionName, closure);
            }
            return closure;
    }
}
