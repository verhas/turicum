package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractNumericOperator implements Operator {

    protected Object binary(final String name,
                            final Object op1, final Object op2,
                            final BiFunction<Long, Long, Long> longOp,
                            final BiFunction<Double, Double, Double> doubleOp
    ) throws ExecutionException {
        ExecutionException.when(op1 == null || op2 == null, "You cannot " + name + " on undefined value");
        if (longOp != null && Cast.isLong(op1) && Cast.isLong(op2)) {
            return longOp.apply(Cast.toLong(op1), Cast.toLong(op2));
        }
        if (doubleOp != null && (Cast.isDouble(op1) || Cast.isDouble(op2))) {
            return doubleOp.apply(Cast.toDouble(op1), Cast.toDouble(op2));
        }

        return Reflect.getBinaryMethod(name, op1, op2).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot '%s' + '%s'", op1, op2));
    }

    protected Object unary(final String name,
                           final Object op,
                           final Function<Long, Long> longOp,
                           final Function<Double, Double> doubleOp
    ) {
        if (Cast.isLong(op)) {
            return longOp.apply(Cast.toLong(op));
        }
        if (Cast.isDouble(op)) {
            return doubleOp.apply(Cast.toDouble(op));
        }

        return Reflect.getUnaryMethod(name, op).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot %s.%s()", op, name));
    }
}

