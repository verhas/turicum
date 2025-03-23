package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;
import javax0.genai.pl.memory.LeftValue;

public record ArrayAccess(Command object, Command index) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        final var object = LeftValue.toArray(this.object.execute(context));
        final var index = this.index.execute(context);
        return object.getIndex(index);
    }
}
