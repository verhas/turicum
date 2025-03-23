package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;

public record FloatConstant(double value) implements Command {
    public FloatConstant(String value) {
        this(Double.parseDouble(value));
    }

    @Override
    public Double execute(Context ctx) throws ExecutionException {
        return value;
    }
}
