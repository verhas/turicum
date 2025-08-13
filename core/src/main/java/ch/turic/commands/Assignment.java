package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Operator;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public class Assignment extends AbstractCommand {
    final LeftValue leftValue;
    final Command expression;
    final String op;

    /**
     * Returns the expression to be assigned in this assignment operation.
     *
     * @return the command representing the right-hand side expression
     */
    public Command expression() {
        return expression;
    }

    /**
     * Returns the left-hand side value of the assignment.
     *
     * @return the target {@code LeftValue} of this assignment
     */
    public LeftValue leftValue() {
        return leftValue;
    }

    /****
     * Creates an `Assignment` instance from the provided unmarshaller arguments.
     * <p>
     * Extracts the left value, operator, and expression from the arguments to construct a new assignment command.
     *
     * @param args the arguments containing the left value, operator, and expression
     * @return a new `Assignment` instance initialized with the extracted values
     */
    public static Assignment factory(Unmarshaller.Args args) {
        return new Assignment(args.get("leftValue", LeftValue.class),
                args.str("op"),
                args.command("expression"));
    }

    /**
     * Constructs an Assignment with the specified left value, operator, and expression.
     *
     * @param leftValue  the target to assign to
     * @param op         the assignment operator (e.g., "=", "+=", etc.)
     * @param expression the expression whose value will be assigned
     */
    public Assignment(LeftValue leftValue, String op, Command expression) {
        this.expression = expression;
        this.op = Objects.requireNonNull(op);
        this.leftValue = leftValue;
    }

    /****
     * Executes the assignment operation within the given context.
     * <p>
     * Performs a simple assignment if the operator is empty, assigning the evaluated expression to the left value.
     * For compound assignments, apply the specified operator to the current value and the evaluated expression.
     *
     * @param ctx the execution context
     * @return the result of the assignment operation
     * @throws ExecutionException if an error occurs during execution or if the operator is unknown
     */
    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return leftValue.reassign(ctx, getOperation(op, ctx));
    }

    /**
     * Returns a function that applies the specified assignment operator to the current value and the assignment expression.
     *
     * @param operator the assignment operator to apply (e.g., "+=", "-=", etc.)
     * @param ctx      the execution context
     * @return a function that takes the current value and returns the result of applying the operator with the assignment expression
     * @throws ExecutionException if the operator is not recognized
     */
    Function<Object, Object> getOperation(final String operator, final Context ctx) {
        if (operator.isEmpty()) {
            return (oldValue) -> expression.execute(ctx);
        }
        if (Operator.OPERATORS.containsKey(operator)) {
            Operator op = Operator.OPERATORS.get(operator);
            return (oldValue) -> op.execute(ctx, context -> oldValue, expression);
        }
        throw new ExecutionException("Unknown operator " + operator);
    }

}
