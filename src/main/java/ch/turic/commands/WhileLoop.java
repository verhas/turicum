package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;

public class WhileLoop extends AbstractCommand {
    public final Command startCondition;
    public final Command exitCondition;

    public Command body() {
        return body;
    }

    public Command exitCondition() {
        return exitCondition;
    }

    public Command startCondition() {
        return startCondition;
    }

    public WhileLoop(Command startCondition, Command exitCondition, Command body) {
        this.body = body;
        this.startCondition = startCondition;
        this.exitCondition = exitCondition;
    }

    public final Command body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result = null;
        context.step();
        final var loopContext = context.loop();
        int loopCounter = 0;
        while (Cast.toBoolean(startCondition.execute(loopContext))) {
            loopContext.count(loopCounter++);
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
        }
        return result;
    }
}
