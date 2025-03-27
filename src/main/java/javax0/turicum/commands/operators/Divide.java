package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("/")
public class Divide extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);

        if (Cast.isLong(op1) && Cast.isLong(op2)) {
            final var lop1 = Cast.toLong(op1);
            final var lop2 = Cast.toLong(op2);
            ExecutionException.when(lop2 == 0, "Cannot divide by zero");
            if (lop1 % lop2 == 0) {
                return lop1 / lop2;
            }
        }
        if (Cast.isDouble(op1) || Cast.isDouble(op2)) {
            final var dop1 = Cast.toDouble(op1);
            final var dop2 = Cast.toDouble(op2);
            ExecutionException.when(dop2 == 0, "Cannot divide by zero");
            return dop1 / dop2;
        }

        return Reflect.getBinaryMethod("divide", op1, op2).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot '%s' + '%s'", op1, op2));
    }
}
