package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngException;
import ch.turic.utils.Unmarshaller;

public class TryCatch extends AbstractCommand {

    final Command tryBlock;
    final Command catchBlock;
    final Command finallyBlock;

    final String exceptionVariable;

    public static TryCatch factory(final Unmarshaller.Args args) {
        return new TryCatch(
                args.command("try"),
                args.command("catch"),
                args.command("finally"),
                args.str("exception")
        );
    }

    public TryCatch(Command tryBlock, Command catchBlock, Command finallyBlock, String exceptionVariable) {
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.finallyBlock = finallyBlock;
        this.exceptionVariable = exceptionVariable;
    }


    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result;
        // save the position of the stack trace so we can drop the deeper elements when catching
        final int traceSize = context.threadContext.traceSize();
        // create a temporary context: we will export from it if no error occurs
        final var ctx = context.shadow();
        try {
            result = tryBlock.execute(ctx);
            exportFromTemporaryContext(ctx, context);
            if (exceptionVariable != null) {
                context.let0(exceptionVariable, null);
                context.freeze(exceptionVariable);
            }
        } catch (ExecutionException e) {
            if (exceptionVariable != null) {
                final var exception = LngException.build(context, e, context.threadContext.getStackTrace());
                if( context.containsLocal(exceptionVariable) ) {
                    throw new ExecutionException("Variable '%s' used in catch is already defined", exceptionVariable);
                }
                context.local(exceptionVariable, exception);
                context.freeze(exceptionVariable);
            }

            if (catchBlock == null) {
                throw e;
            } else {
                //We reset the stack trace only now
                // even if there is finally, but no catch, the original trace lives on
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


    /**
     * Exports all local variables from a temporary context to another context.
     * This method copies all local variables and their values from the source context
     * to the destination context using let0 operation.
     *
     * @param from source context containing the variables to be exported
     * @param to   destination context where variables will be copied to
     */
    public static void exportFromTemporaryContext(Context from, Context to) {
        for (final var variable : from.allFrameKeys()) {
            if (to.containsFrame(variable)) {
                throw new ExecutionException("Variable '%s' is already defined.", variable);
            }
            to.local(variable, from.get(variable));
        }
    }
}