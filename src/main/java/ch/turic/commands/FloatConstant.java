package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class FloatConstant extends AbstractCommand {

    public double value() {
        return value;
    }

    final double value;

    public FloatConstant(double value) {
        this.value = value;
    }

    public FloatConstant(String value) {
        this(Double.parseDouble(value));
    }

    @Override
    public Double _execute(Context ctx) throws ExecutionException {
        return value;
    }
}
