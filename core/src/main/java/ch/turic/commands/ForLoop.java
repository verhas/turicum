package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

public class ForLoop extends AbstractCommand {
    final public Command startCommand;
    final public Command loopCondition;
    final public Command exitCondition;
    final public Command stepCommand;
    final public Command body;
    final boolean resultList;

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

    public ForLoop(final Command startCommand,
                   final Command loopCondition,
                   final Command exitCondition,
                   final Command stepCommand,
                   final boolean resultList,
                   final Command body) {
        this.startCommand = startCommand;
        this.loopCondition = loopCondition;
        this.exitCondition = exitCondition;
        this.stepCommand = stepCommand;
        this.resultList = resultList;
        this.body = body;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result = null;
        final var list = resultList ? new LngList() : null; // not only to save an object memory but also to fail fast
        context.step();
        final var loopContext = context.wrap();
        if (startCommand != null) {
            startCommand.execute(loopContext);
        }
        while (Cast.toBoolean(loopCondition.execute(loopContext))) {
            final var innerContext = loopContext.wrap();
            if (body instanceof BlockCommand block) {
                final var lp = block.loop(innerContext);
                result = lp.result();
                if (resultList) {
                    list.array.add(lp.result());
                }
                if (lp.isDone()) {
                    return resultList ? list : lp.result();
                }
            } else {
                result = body.execute(innerContext);
                if (resultList) {
                    list.array.add(result);
                }
            }
            if (Cast.toBoolean(exitCondition.execute(innerContext))) {
                break;
            }
            if (stepCommand != null) {
                stepCommand.execute(loopContext);
            }
        }
        return resultList ? list : result;
    }
}
