package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

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
