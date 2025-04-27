package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;

public class ArrayAccess extends AbstractCommand {
    final Command object;

    public Command index() {
        return index;
    }

    public Command object() {
        return object;
    }

    public ArrayAccess(Command object, Command index) {
        this.index = index;
        this.object = object;
    }

    final Command index;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        // the execution order is array first, index afterward
        final var lvalValue = object.execute(context);
        final var indexValue = this.index.execute(context);
        final var objectValue = LeftValue.toIndexable(lvalValue, indexValue);
        return objectValue.getIndex(indexValue);
    }
}
