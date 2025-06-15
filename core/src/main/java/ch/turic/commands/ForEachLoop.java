package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.memory.LngList;
import ch.turic.utils.Unmarshaller;

public class ForEachLoop extends Loop {
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

    public static ForEachLoop factory(final Unmarshaller.Args args) {
        return new ForEachLoop(
                args.get("identifier", Identifier.class),
                args.get("with", Identifier.class),
                args.command("expression"),
                args.bool("resultList"),
                args.command("body"),
                args.command("exitCondition")
        );
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
        Object lp = null;
        final var listResult = resultList ? new LngList() : null;
        long loopCounter = 0;
        final var withString = with == null ? null : with.name(context);
        final var identifierString = identifier.name(context);
        for (final var item : LeftValue.toIterable(array)) {
            final var innerContext = loopContext.wrap();
            if (with != null) {
                innerContext.let0(withString, loopCounter);
                innerContext.freeze(withString);
            }
            innerContext.let0(identifierString, item);
            innerContext.freeze(identifierString);

            lp = loopCore(body, innerContext, listResult);
            if (breakLoop(lp)) {
                return normalize(lp);
            } else {
                lp = normalize(lp);
            }
            if (exitLoop(innerContext)) {
                break;
            }
            loopCounter++;
        }
        return resultList ? listResult : lp;
    }

    @Override
    public Command exitCondition() {
        return exitCondition;
    }
}
