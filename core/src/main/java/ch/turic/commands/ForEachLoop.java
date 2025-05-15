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
        final var loopContext = context.wrap();
        final var array = expression.execute(loopContext);
        Object singleResult = null;
        final var listResult = resultList ? new LngList() : null;
        long loopCounter = 0;
        for (final var item : LeftValue.toIterable(array)) {
            final var innerContext = loopContext.wrap();
            if (with != null) {
                innerContext.let0(with.name, loopCounter);
                innerContext.freeze(with.name);
            }
            innerContext.let0(identifier.name, item);
            innerContext.freeze(identifier.name);

            if (body instanceof BlockCommand block) {
                final var lp = block.loop(innerContext);
                singleResult = lp.result();
                if (resultList) {
                    listResult.array.add(singleResult);
                }
                if (lp.isDone()) {
                    return resultList ? listResult : lp.result();
                }
            } else {
                singleResult = body.execute(innerContext);
                if( resultList ){
                    listResult.array.add(singleResult);
                }
            }

            if (Cast.toBoolean(exitCondition.execute(innerContext))) {
                break;
            }
            loopCounter++;
        }
        return resultList ? listResult : singleResult;
    }
}
