package javax0.turicum.commands;


import javax0.turicum.memory.Context;

import java.util.Objects;

public record StringConstant(String value) implements Command {
    public StringConstant {
        Objects.requireNonNull(value);
    }

    @Override
    public String execute(Context ctx) throws ExecutionException {
        return value;
    }
}
