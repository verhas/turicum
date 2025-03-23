package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;

public record IntegerConstant(long value) implements Command {
    public IntegerConstant(String value) {
        this(Long.parseLong(value));
    }

    @Override
    public Long execute(Context ctx) throws ExecutionException {
        return value;
    }
}
