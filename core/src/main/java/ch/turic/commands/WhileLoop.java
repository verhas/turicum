package ch.turic.commands;

import ch.turic.Command;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;
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
        while (Cast.toBoolean(startCondition.execute(loopContext))) {
            wasExecuted = true;
            final var innerContext = loopContext.wrap();
            scalarResult = loopCore(body, innerContext, listResult);
            if (breakLoop(scalarResult)) {
                if (finallyBody != null) {
                    if (resultIsList) {
                        loopContext.define("it", scalarResult, null);
                        loopContext.freeze("it");
                    }
                    final var finallyResult = finallyBody.execute(loopContext);
                    if (finallyResult instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                        return finallyResult;
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
        scalarResult = executeDoneOrOtherwise(wasExecuted, loopContext, listResult, scalarResult);

        if (finallyBody != null) {
            if (!wasExecuted) {// was already set for the 'done' block
                setVariableIT(loopContext, listResult, scalarResult);
            }
            final var finallyResult = finallyBody.execute(loopContext);
            if (finallyResult instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                return finallyResult;
            }
        }
        return resultIsList ? listResult : scalarResult;
    }

    /**
     * Executes the "done" or "otherwise" logic of the loop based on whether the loop's execution was successful.
     * If execution was successful and the "done" body is present, it sets up the loop context and executes the "done" body.
     * Otherwise, if the "otherwise" body is present, it executes the "otherwise" body to determine a new scalar result.
     *
     * @param wasExecuted    indicates whether the loop execution was successful
     * @param loopContext    the local context of the loop
     * @param listResult     the list-result to be set in the context, if applicable
     * @param scalarResult   the resulting scalar value
     * @return the resulting scalar value; may be updated if "otherwise" body logic is executed
     */
    private Object executeDoneOrOtherwise(final boolean wasExecuted,
                                          final LocalContext loopContext,
                                          final LngList listResult,
                                          final Object scalarResult) {
        if (wasExecuted) {
            if (doneBody != null) {
                setVariableIT(loopContext, listResult, scalarResult);
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
     * @param loopContext the local context of the loop where the variable is to be set
     * @param listResult the scalarResult list to assign to the "it" variable if the result is a list
     * @param scalarResult the value to assign to the "it" variable if the result is not a list
     */
    private void setVariableIT(LocalContext loopContext, LngList listResult, Object scalarResult) {
        if (resultIsList) {
            loopContext.define("it", listResult, null);
            listResult.pinned.set(true);
        } else {
            loopContext.define("it", scalarResult, null);
            switch(scalarResult){
                case LngList list -> list.pinned.set(true);
                case LngObject obj -> obj.pinned.set(true);
                default -> { }
            }
        }
        loopContext.freeze("it");
    }
}
