package ch.turic;

import ch.turic.commands.AbstractCommand;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class Program extends AbstractCommand {
    final Command[] commands;

    public Command[] commands() {
        return commands;
    }

    public static Program factory(final Unmarshaller.Args args) {
        return new Program(args.get("commands", Command[].class));
    }

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
