package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public class ForLoop extends AbstractCommand {
    final public Command startCommand;
    final public Command loopCondition;
    final public Command exitCondition;
    final public Command stepCommand;
    final public Command body;

    public Command body() {
        return body;
    }

    public Command exitCondition() {
        return exitCondition;
    }

    public Command loopCondition() {
        return loopCondition;
    }

    public Command startCommand() {
        return startCommand;
    }

    public Command stepCommand() {
        return stepCommand;
    }

    public ForLoop(Command startCommand, Command loopCondition, Command exitCondition, Command stepCommand, Command body) {
        this.startCommand = startCommand;
        this.loopCondition = loopCondition;
        this.exitCondition = exitCondition;
        this.stepCommand = stepCommand;
        this.body = body;
    }

    @Override
    public Object execute(Context context) throws ExecutionException {
        Object result = null;
        context.step();
        final var loopContext = context.wrap();
        startCommand.execute(loopContext);
        while (Cast.toBoolean(loopCondition.execute(loopContext))) {
            if (body instanceof BlockCommand block) {
                final var lp = block.loop(loopContext);
                result = lp.result();
                if (lp.isDone()) {
                    return lp.result();
                }
            } else {
                result = body.execute(loopContext);
            }
            if (Cast.toBoolean(exitCondition.execute(loopContext))) {
                break;
            }
            stepCommand.execute(loopContext);
        }
        return result;
    }
}
