package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;
import ch.turic.memory.Sentinel;

public abstract class Loop extends AbstractCommand {

    public abstract Command exitCondition();

    public boolean exitLoop(Context ctx) {
        return Cast.toBoolean(exitCondition().execute(ctx));
    }

    public static boolean breakLoop(final Object lp) {
        return lp instanceof Conditional c && c.isDone();
    }

    public static Object normalize(final Object lp) {
        if (lp instanceof Conditional c) {
            if (c.result() == Sentinel.NO_VALUE) {
                return null;
            }
            return c.result();
        }
        return lp;
    }

    public Object loopCore(Command body, Context ctx, LngList listResult) {
        if (listResult == null) {
            return loopCoreForObject(body, ctx);
        } else {
            return loopCoreList(body, ctx, listResult);
        }
    }

    public Object loopCoreForObject(Command body, Context ctx) {
        if (body instanceof BlockCommand block) {
            return loop(ctx, block, false);
        } else {
            return singleCommandExecutionForObject(body, ctx);
        }
    }

    private Object loopCoreList(Command body, Context ctx, LngList listResult) {
        if (body instanceof BlockCommand block) {
            final var conditional = loop(ctx, block, true);
            if (conditional != null) {
                if (conditional.result() != Sentinel.NO_VALUE) {
                    listResult.array.add(conditional.result());
                }
                if (conditional.isDone()) {
                    return Conditional.doBreak(listResult);
                }
            }
        } else {
            return singleCommandExecutionForList(body, ctx, listResult);
        }
        return listResult;
    }

    /**
     * Execute the body of the loop once and return the result of this single execution.
     * This result will only be used if the loop is a list-creating loop or if it was the last execution.
     *
     * @param context    to execute the loop body in
     * @param block      the body of the loop containing the commands
     * @param resultList signals if this is a list-resulting loop or not. {@code true} if it is a list resulting loop.
     * @return the result of the last executed command wrapped in a conditional.
     * It returns {@code null} when the last command was a {@code continue} without any value.
     * It means no result, and in case it is a list-producing loop then no value will be added to the list for this
     * execution.
     */
    private Conditional loop(final Context context, BlockCommand block, boolean resultList) {
        Object result = null;
        for (final var cmd : block.commands) {
            if (cmd instanceof ContinueCommand continueCommand) {
                if (!resultList && continueCommand.expression != null) {
                    throw new ExecutionException("You cannot 'continue' with a value in a non-list resulting loop.");
                }
                final boolean doContinue;
                if (continueCommand.condition == null) {
                    doContinue = true;
                } else {
                    doContinue = Cast.toBoolean(continueCommand.condition.execute(context));
                }
                if (doContinue) {
                    if (continueCommand.expression == null) {
                        return null;
                    }
                    return Conditional.result(continueCommand.expression.execute(context));
                }
            } else {
                result = cmd.execute(context);
            }
            if (result instanceof Conditional cResult) {
                if (cResult.isDone()) {
                    return cResult;
                }
                // important if it was the last command
                // to avoid double conditional casketing
                result = cResult.result();
            }
        }
        return Conditional.result(result);
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
