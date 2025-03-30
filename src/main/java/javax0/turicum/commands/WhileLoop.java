package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record WhileLoop(Command startCondition, Command exitCondition, Command body) implements Command {
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
