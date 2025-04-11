package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngException;

public class TryCatch extends AbstractCommand {

    final Command tryBlock;
    final Command catchBlock;
    final Command finallyBlock;

    public Command catchBlock() {
        return catchBlock;
    }

    public String exceptionVariable() {
        return exceptionVariable;
    }

    public Command finallyBlock() {
        return finallyBlock;
    }

    public Command tryBlock() {
        return tryBlock;
    }

    final String exceptionVariable;

    public TryCatch(Command tryBlock, Command catchBlock, Command finallyBlock, String exceptionVariable) {
        this.tryBlock = tryBlock;
        this.catchBlock = catchBlock;
        this.finallyBlock = finallyBlock;
        this.exceptionVariable = exceptionVariable;
    }


    @Override
    public Object execute(Context context) throws ExecutionException {
        Object result = null;
        final var ctx = context.wrap();
        try {
            result = tryBlock.execute(ctx);
        } catch (ExecutionException e) {
            if( catchBlock == null ) {
                throw e;
            }
            final var exception = new LngException(e);
            ctx.let0(exceptionVariable, exception);
            catchBlock.execute(ctx);
        } finally {
            if (finallyBlock != null) {
                result = finallyBlock.execute(ctx);
            }
        }
        return result;
    }
}
/*
// snippet TryCatch_Documentation

{%command_section `try` / `catch` / `finally`%}

The syntax of the command is

[source]
----
  try command1 catch identifier command2 finally command3
----

The part with the `catch` and `finally` are optional.
If the `catch` part is omitted, the exception will not be caught.

If there is a `catch` block, all exceptions are caught.
The exception will be assigned to the variable following the `catch` keyword.
An exception is a Turicum object.

The commands can be blocks or individual commands.
If any of the commands is an individual command, then it must be preceded with a `:` character.

The return value of the command is

* the result of the `try` block.

* unless there is an exception. In that case the result is `null`.

* If there is a `finally` block then the result is the value of the `finally` block.

 */