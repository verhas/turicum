package javax0.genai.pl.commands;

import javax0.genai.pl.memory.Context;

public record Undefined() implements Command {
    public static final Undefined INSTANCE = new Undefined();

    @Override
    public Object execute(Context context) throws ExecutionException {
        return null;
    }
}
