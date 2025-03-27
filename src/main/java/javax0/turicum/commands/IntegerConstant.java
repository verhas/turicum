package javax0.turicum.commands;


import javax0.turicum.memory.Context;

public record IntegerConstant(long value) implements Command {
    public IntegerConstant(String value) {
        this(Long.parseLong(value));
    }

    @Override
    public Long execute(Context ctx) throws ExecutionException {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
