package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;

public class ForEachLoop extends AbstractCommand {
    public final Identifier identifier;
    public final Identifier with;
    public final Command expression;
    public final Command body;
    public final Command exitCondition;

    public ForEachLoop(Identifier identifier, Identifier with, Command expression, Command body, Command exitCondition) {
        this.identifier = identifier;
        this.with = with;
        this.expression = expression;
        this.body = body;
        this.exitCondition = exitCondition;
    }

    public Command body() {
        return body;
    }

    public Command exitCondition() {
        return exitCondition;
    }

    public Command expression() {
        return expression;
    }

    public Identifier identifier() {
        return identifier;
    }


    @Override
    public Object _execute(final Context context) throws ExecutionException {
        context.step();
        final var loopContext = context.loop();
        final var id = identifier.name();
        loopContext.freeze(id);
        final var array = expression.execute(loopContext);
        Object result = null;
        int loopCounter = 0;
        if( with != null ) {
            loopContext.let0(with.name, (long) loopCounter);
        }
        for (final var item : LeftValue.toIterable(array)) {
            if( with != null ) {
                loopContext.let0(with.name, (long) loopCounter);
            }
            loopContext.count(loopCounter++);
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
