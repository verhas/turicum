package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.commands.AbstractCommand;
/*snippet builtin0025

==== `as_object`

This function converts a command into an object.
This function supports reflective programming and is likely used together with the `thunk`, `unthunk`, inside macros.

{%S as_object%}

The field `java$canonicalName` contains the canonical name of the class that implements the command.
The other fields are the fields of the command implementation.
In the example above, the command is a block command.
It is a wrapping command (it sees the variables defined before it outside of the block) and contains an array of commands.
This array has one element, which is an assignment (`LetAssignment`).
One assignment command may contain multiple assignments; hence, the field `assignments` is named plural and is an array.
In this case, it has one element.
It has an identifier, `k`, a types array, and an expression, which is an integer constant with the value `5`.
The assignment uses the `let` keyword; therefore, the `mut` field is set to `false`.

end snippet*/
/**
 * The function retrieves and converts an {@link AbstractCommand} into a language
 * object using the execution context. If the provided argument is not of type
 * {@link AbstractCommand}, an {@link ExecutionException} is thrown.
 * <p>
 * This functionality ensures type safety and proper conversion of commands
 * into objects within the Turi execution environment.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * let z = '{let k = 5}
 * die "" when str(as_object(z)) !=
 *     "{java$canonicalName: ch.turic.commands.BlockCommand, " +
 *      "wrap: true, commands: [{assignments: [{identifier: k, types: [], " +
 *      "expression: {java$canonicalName: ch.turic.commands.IntegerConstant, " +
 *      "value: 5}, " +
 *      "java$canonicalName: ch.turic.analyzer.AssignmentList.Assignment}], " +
 *      "java$canonicalName: ch.turic.commands.LetAssignment, mut: false}]}"
 * }</pre>
 *
 */
@Name("as_object")
public class Command implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments, AbstractCommand.class).toLngObject(FunUtils.ctx(context));
    }
}
