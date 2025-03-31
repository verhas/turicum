package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public record ForEachLoop(Identifier identifier, Command expression, Command body,
                          Command exitCondition) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        context.step();
        final var loopContext = context.wrap();
        final var id = identifier.name();
        loopContext.freeze(id);
        final var array = expression.execute(loopContext);
        Object result = null;
        for (final var item : LeftValue.toIndexable(array)) {
            loopContext.let0(id, item);
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
