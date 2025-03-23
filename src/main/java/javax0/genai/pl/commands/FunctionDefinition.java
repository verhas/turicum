package javax0.genai.pl.commands;

import javax0.genai.pl.memory.Context;

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
