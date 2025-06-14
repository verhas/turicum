package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;
import ch.turic.utils.Unmarshaller;

public class WhileLoop extends Loop {
    public final Command startCondition;
    public final Command exitCondition;
    public final boolean resultList;
    public final Command body;

    public Command body() {
        return body;
    }

    public Command exitCondition() {
        return exitCondition;
    }

    public static WhileLoop factory(final Unmarshaller.Args args) {
        return new WhileLoop(
                args.command("startCondition"),
                args.command("exitCondition"),
                args.bool("resultList"),
                args.command("body")
        );
    }

    public WhileLoop(Command startCondition, Command exitCondition, boolean resultList, Command body) {
        this.body = body;
        this.resultList = resultList;
        this.startCondition = startCondition;
        this.exitCondition = exitCondition;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object lp = null;
        final var listResult = resultList ? new LngList() : null;
        context.step();
        final var loopContext = context.wrap();
        while (Cast.toBoolean(startCondition.execute(loopContext))) {
            final var innerContext = loopContext.wrap();
            lp = loopCore(body, innerContext, listResult);
            if (breakLoop(lp)) {
                return normalize(lp);
            } else {
                lp = normalize(lp);
            }
            if (exitLoop(innerContext)) {
                break;
            }
        }
        return resultList ? listResult : lp;
    }
}
