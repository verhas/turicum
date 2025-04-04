package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public record ArrayAccess(Command object, Command index) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        // the execution order is array first, index afterward
        final var lvalValue = object.execute(context);
        final var indexValue = this.index.execute(context);
        final var objectValue = LeftValue.toIndexable(lvalValue,indexValue);
        return objectValue.getIndex(indexValue);
    }
}
