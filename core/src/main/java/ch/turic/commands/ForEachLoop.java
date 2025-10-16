package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LeftValue;
import ch.turic.memory.LngList;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;

public class ForEachLoop extends Loop {
    /**
     * the array of loop variables
     */
    public final Identifier[] identifiers;
    /**
     * true if the loop works on a list. Must be true when there is more than one loop variable
     */
    public final boolean listLoopVar;
    public final Identifier with;
    public final Command expression;
    public final boolean resultList;
    public final Command body;
    public final Command exitCondition;

    public ForEachLoop(Identifier[] identifiers, final boolean listLoopVar, Identifier with, Command expression, boolean resultList, Command body, Command exitCondition) {
        if (identifiers == null || identifiers.length == 0) {
            throw new IllegalArgumentException("Loop needs at least one identifier. Got: " + Arrays.toString(identifiers));
        }
        if (identifiers.length > 1 && !listLoopVar) {
            throw new IllegalArgumentException("Loop with multiple loop-vars needs to work on a list. Got: " + Arrays.toString(identifiers) + " and listLoopVar is false");
        }
        this.identifiers = identifiers;
        this.listLoopVar = listLoopVar;
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
                args.get("identifiers", Identifier[].class),
                args.bool("listLoopVar"),
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

    public Identifier[] identifiers() {
        return identifiers;
    }

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        context.step();
        final var loopContext = context.wrap();
        final var array = expression.execute(loopContext);
        Object lp = null;
        final var listResult = resultList ? new LngList() : null;
        long loopCounter = 0;
        for (final var item : LeftValue.toIterable(array)) {
            final var innerContext = loopContext.wrap();
            if (with != null) {
                innerContext.let0(with.name, loopCounter);
                innerContext.freeze(with.name);
            }
            if (listLoopVar) {
                if (item instanceof Iterable<?> list) {
                    int i = 0;
                    for (var listItem : list) {
                        if (i >= identifiers.length) {
                            throw new ExecutionException("Loop with list-loop-var needs " + identifiers.length + " arguments, and got more: " + item);
                        }
                        innerContext.let0(identifiers[i].name, listItem);
                        innerContext.freeze(identifiers[i].name);
                        i++;
                    }
                    if (i < identifiers.length) {
                        throw new ExecutionException("Loop with list-loop-var needs " + identifiers.length + " arguments, and got less: " + item);
                    }
                } else {
                    throw new ExecutionException("Loop with list-loop-var needs to work on a list. Got: " + item.getClass().getName());
                }
            } else {
                innerContext.let0(identifiers[0].name, item);
                innerContext.freeze(identifiers[0].name);
            }

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
