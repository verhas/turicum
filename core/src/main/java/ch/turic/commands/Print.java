package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

/**
 * Evaluate a Command
 */
public class Print extends AbstractCommand {

    private final Command[] commands;
    private final boolean nl;

    public Print(Command[] commands, boolean nl) {
        this.commands = commands;
        this.nl = nl;
    }


    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        for (final var cmd : commands) {
            final var arg = cmd.execute(ctx);
            System.out.print(switch (arg) {
                case null -> "none";
                default -> "" + arg;
            });
        }
        if( nl ){
            System.out.println();
        }
        System.out.flush();
        return null;
    }
}
