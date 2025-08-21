package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.commands.AbstractCommand;

 /**
 * The function retrieves and converts an {@link AbstractCommand} into a language
 * object using the execution context. If the provided argument is not of type
 * {@link AbstractCommand}, an {@link ExecutionException} is thrown.
 * <p>
 * This functionality ensures type safety and proper conversion of commands
 * into objects within the Turi execution environment.
 *
 * Example usage:
 *
 * <pre>{@code
 * let z = thunk({let k = 5})
 * die "" if str(as_object(z)) !=
  *     "{java$canonicalName: ch.turic.commands.BlockCommand, " +
 *      "wrap: true, commands: [{assignments: [{identifier: k, types: [], " +
 *      "expression: {java$canonicalName: ch.turic.commands.IntegerConstant, " +
 *      "value: 5}, " +
 *      "java$canonicalName: ch.turic.analyzer.AssignmentList.Assignment}], " +
 *      "java$canonicalName: ch.turic.commands.LetAssignment, mut: false}]}"
 * }</pre>
 *
 */
@Name( "as_object")
public class Command implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments, AbstractCommand.class).toLngObject(FunUtils.ctx(context));
    }
}
