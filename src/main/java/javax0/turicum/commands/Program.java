package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public record Program(Command[] commands) implements Command {
    @Override
    public Object execute(Context ctx) throws ExecutionException {
        Object value = null;
        for (Command command : commands) {
            value = command.execute(ctx);
        }
        return value;
    }
}
