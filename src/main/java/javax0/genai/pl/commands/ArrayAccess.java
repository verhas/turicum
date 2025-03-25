package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;
import javax0.genai.pl.memory.LeftValue;

public record ArrayAccess(Command object, Command index) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        // the execution order is array first, index afterward
        final var lvalValue = object.execute(context);
        final var indexValue = this.index.execute(context);
        final var objectValue = LeftValue.toIndexable(lvalValue, indexValue);
        return objectValue.getIndex(indexValue);
    }
}
