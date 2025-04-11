package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

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
    public Object execute(Context context) throws ExecutionException {
        Object result = null;
        context.step();
        final var loopContext = context.wrap();
        while (Cast.toBoolean(startCondition.execute(loopContext))) {
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
