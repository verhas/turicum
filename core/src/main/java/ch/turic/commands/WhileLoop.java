package ch.turic.commands;

import ch.turic.Command;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.InterpreterHalt;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;
import ch.turic.memory.Sentinel;
import ch.turic.utils.Unmarshaller;

public class WhileLoop extends Loop {

    public final Command initBody;
    public final Command startCondition;
    public final Command exitCondition;
    public final boolean resultIsList;
    public final Command body;
    public final Command doneBody;
    public final Command otherwiseBody;
    public final Command finallyBody;

    public Command body() {
        return body;
    }

    public Command exitCondition() {
        return exitCondition;
    }

    public static WhileLoop factory(final Unmarshaller.Args args) {
        return new WhileLoop(
                args.command("initBody"),
                args.command("startCondition"),
                args.command("exitCondition"),
                args.bool("resultIsList"),
                args.command("body"),
                args.command("doneBody"),
                args.command("otherwiseBody"),
                args.command("finallyBody")
        );
    }

    public WhileLoop(Command initBody, Command startCondition, Command exitCondition, boolean resultIsList, Command body, Command doneBody, Command otherwiseBody, Command finallyBody) {
        this.body = body;
        this.resultIsList = resultIsList;
        this.startCondition = startCondition;
        this.exitCondition = exitCondition;
        this.doneBody = doneBody;
        this.otherwiseBody = otherwiseBody;
        this.finallyBody = finallyBody;
        this.initBody = initBody;
    }

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        Object scalarResult = null;
        final var listResult = resultIsList ? new LngList() : null;
        context.step();
        final var loopContext = context.wrap();
        if (initBody != null) {
            initBody.execute(loopContext);
        }
        var wasExecuted = false;
        try {
            while (Cast.toBoolean(startCondition.execute(loopContext))) {
                wasExecuted = true;
                final var innerContext = loopContext.wrap();
                scalarResult = loopCore(body, innerContext, listResult);
                if (breakLoop(scalarResult)) {
                    if (finallyBody != null) {
                        final var result = (Conditional.Result) scalarResult;
                        // will throw if 'it' already exists, user must not define
                        loopContext.define("it", result.result() == Sentinel.NO_VALUE ? null : result.result());
                        loopContext.freeze("it");
                        final var finallyResult = finallyBody.execute(loopContext);
                        if (finallyResult instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                            throw new ExecutionException("Must not return/break/continue in finally block of a while loop");
                        }
                    }
                    return normalize(scalarResult);
                } else {
                    scalarResult = normalize(scalarResult);
                }
                if (exitLoop(innerContext)) {
                    break;
                }
            }
        } catch (InterpreterHalt halt) {
            // a halt (step limit / abort) propagated out of the loop body itself - the two
            // finallyBody invocations above (break path) and below (normal-completion path)
            // are unreachable in this case, since a Java exception skips straight past them;
            // this is the loop's only chance to release resources, under a bounded grace
            // window that can never suppress the halt itself (see Grace.beginCleanup())
            if (finallyBody != null) {
                context.threadContext.grace().beginCleanup();
                finallyBody.execute(loopContext);
            }
            throw halt;
        }
        setVariableIT(loopContext, listResult, scalarResult);
        scalarResult = executeDoneOrOtherwise(wasExecuted, loopContext, scalarResult);

        if (scalarResult instanceof Conditional.BreakResult || scalarResult instanceof Conditional.ContinueResult) {
            throw new ExecutionException("done block of a while loop must not break or continue");
        }

        if (finallyBody != null) {
            final var finallyResult = finallyBody.execute(loopContext);
            if (finallyResult instanceof Conditional.Result returnResult && returnResult.isDone()) {
                throw new ExecutionException("Must not return/break/continue in finally block of a while loop");
            }
        }
        // if the done block contains a "return", then we will return that value as the result of the loop
        // even if it is a list resulting loop, because this may return from the surrounding function
        if (scalarResult instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
            return scalarResult;
        }
        return resultIsList ? listResult : scalarResult;
    }

    /**
     * Executes the "done" or "otherwise" logic of the loop based on whether the loop's execution was successful.
     * If execution was successful and the "done" body is present, it sets up the loop context and executes the "done" body.
     * Otherwise, if the "otherwise" body is present, it executes the "otherwise" body to determine a new scalar result.
     *
     * @param wasExecuted  indicates whether the loop execution was successful
     * @param loopContext  the local context of the loop
     * @param scalarResult the resulting scalar value
     * @return the resulting scalar value; may be updated if "otherwise" body logic is executed
     */
    private Object executeDoneOrOtherwise(final boolean wasExecuted,
                                          final LocalContext loopContext,
                                          final Object scalarResult) {
        if (wasExecuted) {
            if (doneBody != null) {
                return doneBody.execute(loopContext);
            }
        } else {
            if (otherwiseBody != null) {
                return otherwiseBody.execute(loopContext);
            }
        }
        return scalarResult;
    }

    /**
     * Sets the variable "it" in the specified loop context.
     * If the scalarResult is a list, it assigns the provided listResult. Otherwise, it assigns the provided
     * {@code scalarResult}.
     * After setting the variable, it freezes the "it" variable in the loop context.
     *
     * @param loopContext  the local context of the loop where the variable is to be set
     * @param listResult   the scalarResult list to assign to the "it" variable if the result is a list
     * @param scalarResult the value to assign to the "it" variable if the result is not a list
     */
    private void setVariableIT(LocalContext loopContext, LngList listResult, Object scalarResult) {
        if (resultIsList) {
            loopContext.define("it", listResult);
            listResult.pinned.set(true);
        } else {
            final Object realResult;
            if (scalarResult instanceof Conditional.Result result) {
                realResult = result.result();
            } else {
                realResult = scalarResult;
            }
            loopContext.define("it", realResult);
            switch (realResult) {
                case LngList list -> list.pinned.set(true);
                case LngObject obj -> obj.pinned.set(true);
                case null, default -> {
                }
            }
        }
        loopContext.freeze("it");
    }
}
