package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

public class WhileLoop extends AbstractCommand {
    public final Command startCondition;
    public final boolean resultList;
    public final Command body;
    public final Command exitCondition;

    public Command body() {
        return body;
    }

    public WhileLoop(Command startCondition, Command exitCondition, boolean resultList, Command body) {
        this.body = body;
        this.resultList = resultList;
        this.startCondition = startCondition;
        this.exitCondition = exitCondition;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result = null;
        final var list = resultList ? new LngList() : null;
        context.step();
        final var loopContext = context.wrap();
        while (Cast.toBoolean(startCondition.execute(loopContext))) {
            final var innerContext= loopContext.wrap();
            if (body instanceof BlockCommand block) {
                final var lp = block.loop(innerContext);
                result = lp.result();
                if(resultList){
                    list.array.add(result);
                }
                if (lp.isDone()) {
                    return resultList ? list : result;
                }
            } else {
                result = body.execute(innerContext);
                if(resultList){
                    list.array.add(result);
                }
            }
            if (Cast.toBoolean(exitCondition.execute(innerContext))) {
                break;
            }
        }
        return resultList ? list : result;
    }
}
