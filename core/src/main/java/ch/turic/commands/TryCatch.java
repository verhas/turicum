package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngException;

public class TryCatch extends AbstractCommand {

    private static final String[] ERR_TYPE = {"err"};
    private static final String[] ERR_NONE_TYPE = {"none"};
    final Command tryBlock;
    final Command catchBlock;
    final Command finallyBlock;

    final String exceptionVariable;

    public TryCatch(Command tryBlock, Command catchBlock, Command finallyBlock, String exceptionVariable) {
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.finallyBlock = finallyBlock;
        this.exceptionVariable = exceptionVariable;
    }


    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result;
        // save the position of the stack trace so we can drop the elements that are deeper when catching
        final int traceSize = context.threadContext.traceSize();
        final var ctx = context.wrap();
        try {
            result = tryBlock.execute(ctx);
            for( final var variable : ctx.allLocalKeys()){
                context.let0(variable, ctx.get(variable));
            }
            if (exceptionVariable != null) {
                context.let0(exceptionVariable, null);
                context.freeze(exceptionVariable);
            }
        } catch (ExecutionException e) {
            if (exceptionVariable != null) {
                final var exception = LngException.build(context, e, context.threadContext.getStackTrace());
                context.let0(exceptionVariable, exception);
                context.freeze(exceptionVariable);
            }

            if (catchBlock == null) {
                throw e;
            } else {
                // we reset the stack trace only now
                // even if there is finally, but no catch the original trace lives on
                context.threadContext.resetTrace(traceSize);
                result = catchBlock.execute(context);
            }
        } finally {
            if (finallyBlock != null) {
                finallyBlock.execute(context);
            }
        }
        return result;
    }
}