package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.memory.LngList;

public class ForEachLoop extends AbstractCommand {
    public final Identifier identifier;
    public final Identifier with;
    public final Command expression;
    public final boolean resultList;
    public final Command body;
    public final Command exitCondition;

    public ForEachLoop(Identifier identifier, Identifier with, Command expression, boolean resultList, Command body, Command exitCondition) {
        this.identifier = identifier;
        this.with = with;
        this.expression = expression;
        this.resultList = resultList;
        this.body = body;
        this.exitCondition = exitCondition;
    }

    public Command body() {
        return body;
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
        final var list = resultList ? new LngList() : null;
        int loopCounter = 0;
        if (with != null) {
            loopContext.let0(with.name, (long) loopCounter);
        }
        for (final var item : LeftValue.toIterable(array)) {
            if (with != null) {
                loopContext.let0(with.name, (long) loopCounter);
            }
            loopContext.count(loopCounter++);
            loopContext.let0(id, item);
            if (body instanceof BlockCommand block) {
                final var lp = block.loop(loopContext);
                result = lp.result();
                if (resultList) {
                    list.array.add(result);
                }
                if (lp.isDone()) {
                    return resultList ? list : lp.result();
                }
            } else {
                result = body.execute(loopContext);
                if( resultList ){
                    list.array.add(result);
                }
            }
            if (Cast.toBoolean(exitCondition.execute(loopContext))) {
                break;
            }
        }
        return resultList ? list : result;
    }
}
