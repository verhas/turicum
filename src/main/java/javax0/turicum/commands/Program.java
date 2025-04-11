package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public class Program extends AbstractCommand {
    public Command[] commands() {
        return commands;
    }

    final Command[] commands;

    public Program(Command[] commands) {
        this.commands = commands;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object value = null;
        for (Command command : commands) {
            value = command.execute(context);
        }
        return value;
    }
}
