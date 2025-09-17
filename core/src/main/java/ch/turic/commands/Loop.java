package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LngList;
import ch.turic.memory.Sentinel;

public abstract class Loop extends AbstractCommand {

    public abstract Command exitCondition();

    public boolean exitLoop(LocalContext ctx) {
        return Cast.toBoolean(exitCondition().execute(ctx));
    }

    /**
     * Determines whether the given object represents a value that signals that a loop should be broken.
     * <p>
     * This method evaluates whether the provided object is an instance of the
     * {@code Conditional} interface and meets specific conditions to signify
     * the termination of a loop.
     * <p>
     * A loop is terminated if the value {@link Conditional#isDone()} is true and it is not a "continue"
     * result. The {@link Conditional} interface is implemented by the interfaces that represent the conditional
     * value of a {@code break}, {@code return} or {@code continue} command. The first two will also make the loop
     * to terminate, the last one, {@code continue} will not. However, even the {@link Conditional.ContinueResult}
     * will result in an {@link Conditional#isDone()} true, because if it was executed in a block, then the block
     * should not go on executing other commands.
     *
     * @param lp the object representing a loop or control flow state. It can be an
     *           instance of {@code Conditional} or other related types.
     * @return true if the loop should break, which occurs when the object is an
     * instance of {@code Conditional} and is marked as done, but not
     * specifically a {@code Conditional.ContinueResult}; false otherwise.
     */
    public static boolean breakLoop(final Object lp) {
        return lp instanceof Conditional c && c.isDone() && !(lp instanceof Conditional.ContinueResult);
    }

    /**
     * Normalizes the given object by processing its type and condition.
     * If the input object is an instance of {@code Conditional} and its result is {@code Sentinel.NO_VALUE},
     * the method returns {@code null}. If it is a {@code Conditional.BreakResult},
     * it returns the result value of the conditional. Otherwise, it returns the object as is.
     * <p>
     * This method is invoked from the execution of a loop. When the loop is terminated, the value of the
     * {@code break} command is returned. If this is an ordinary object or some other {@link Conditional}
     * object, then the object is returned.
     * <p>
     * The reason to return an ordinary object is obvious: that is the value.
     * <p>
     * For {@code continue} and {@code return} conditional values the original conditional value is returned
     * because it will be used by the loop (to add the value to the list) or by the function or closure from which
     * we will return. They have to know that this is a value that they have to handle.
     *
     * @param lp the object to be normalized. It may represent a loop or control flow state,
     *           specifically an instance of {@code Conditional} or its subclasses.
     * @return the normalized object. This can be {@code null}, the result value of a {@code Conditional},
     * or the original object if no specific normalization is required.
     */
    public static Object normalize(final Object lp) {
        if (lp instanceof Conditional c) {
            if (c.result() == Sentinel.NO_VALUE) {
                return null;
            }
            if (lp instanceof Conditional.BreakResult) {
                return c.result();
            }
        }
        return lp;
    }

    /**
     * Executes the core logic of a loop based on the given input parameters. Depending on whether the
     * {@code listResult} parameter is null or not, the method delegates to either
     * {@code loopCoreForObject} or {@code loopCoreList}.
     *
     * @param body the command representing the body of the loop
     * @param ctx the context in which the command is executed
     * @param listResult a list to store the results of the loop executions, if applicable.
     *                   If {@code null}, this indicates that the loop does not produce a result list.
     * @return the result of the loop execution, which could be an object representing a single result
     *         or a list of results, depending on the type of the loop.
     */
    public Object loopCore(Command body, LocalContext ctx, LngList listResult) {
        if (listResult == null) {
            return loopCoreForObject(body, ctx);
        } else {
            return loopCoreList(body, ctx, listResult);
        }
    }

    public Object loopCoreForObject(Command body, LocalContext ctx) {
        if (body instanceof BlockCommand block) {
            return loop(ctx, block, false);
        } else {
            return singleCommandExecutionForObject(body, ctx);
        }
    }

    private Object loopCoreList(Command body, LocalContext ctx, LngList listResult) {
        if (body instanceof BlockCommand block) {
            final var conditional = loop(ctx, block, true);
            if (conditional != null) {
                if (conditional.result() != Sentinel.NO_VALUE) {
                    listResult.array.add(conditional.result());
                }
                if (conditional.isDone() && !(conditional instanceof Conditional.ContinueResult)) {
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
     * It means no result, and in case it is a list-producing loop, then no value will be added to the list for this
     * execution.
     */
    private Conditional loop(final LocalContext context, BlockCommand block, boolean resultList) {
        Object result = null;
        for (final var cmd : block.commands) {
            result = cmd.execute(context);

            if (result instanceof Conditional.ContinueResult continueResult) {
                if (continueResult.result() != Sentinel.NO_VALUE) {
                    if (resultList) {
                        return continueResult;
                    } else {
                        throw new ExecutionException("You cannot 'continue' with a value in a non-list resulting loop.");
                    }
                }
                return continueResult;
            }
            if (result instanceof Conditional cResult) {
                if (cResult.isDone()) {
                    return cResult;
                }
                // important if it was the last command
                // to avoid double conditional casting
                result = cResult.result();
            }
        }
        return Conditional.result(result);
    }

    private static LngList singleCommandExecutionForList(Command body, LocalContext ctx, LngList listResult) {
        listResult.array.add(body.execute(ctx));
        return listResult;
    }

    private static Object singleCommandExecutionForObject
            (Command body, LocalContext ctx) {
        return body.execute(ctx);
    }

}
