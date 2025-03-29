package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public record FloatConstant(double value) implements Command {
    public FloatConstant(String value) {
        this(Double.parseDouble(value));
    }

    @Override
    public Double execute(Context ctx) throws ExecutionException {
        return value;
    }
}
