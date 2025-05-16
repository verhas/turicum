package ch.turic.commands;

import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

public abstract class Loop extends AbstractCommand {

    public abstract Command exitCondition();

    public boolean exitLoop(Context ctx) {
        return Cast.toBoolean(exitCondition().execute(ctx));
    }

    public static boolean breakLoop(final Object lp) {
        return lp instanceof Conditional c && c.isDone();
    }

    public static Object normalize(final Object lp) {
        if( lp instanceof Conditional c ){
            return c.result();
        }
        return lp;
    }

    public static Object loopCore(Command body, Context ctx, LngList listResult) {
        if (listResult == null) {
            return loopCoreForObject(body, ctx);
        } else {
            return loopCoreList(body, ctx, listResult);
        }
    }

    public static Object loopCoreForObject(Command body, Context ctx) {
        if (body instanceof BlockCommand block) {
            return block.loop(ctx);
        } else {
            return singleCommandExecutionForObject(body, ctx);
        }
    }

    public static Object loopCoreList(Command body, Context ctx, LngList listResult) {
        if (body instanceof BlockCommand block) {
            final var conditional = block.loop(ctx);
            listResult.array.add(conditional.result());
            if (conditional.isDone()) {
                return Conditional.doBreak(listResult);
            }
        } else {
            return singleCommandExecutionForList(body, ctx, listResult);
        }
        return listResult;
    }

    private static LngList singleCommandExecutionForList(Command body, Context ctx, LngList listResult) {
        listResult.array.add(body.execute(ctx));
        return listResult;
    }

    private static Object singleCommandExecutionForObject
            (Command body, Context ctx) {
        return body.execute(ctx);
    }

}
