package ch.turic.commands;


import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LeftValue;
import ch.turic.utils.Unmarshaller;

public class ArrayAccess extends AbstractCommand {
    final Command object;
    final Command index;

    public Command index() {
        return index;
    }

    public Command object() {
        return object;
    }


    public static ArrayAccess factory(Unmarshaller.Args args) {
        return new ArrayAccess(args.command("object"), args.command("index"));
    }

    public ArrayAccess(Command object, Command index) {
        this.index = index;
        this.object = object;
    }


    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        // the execution order is array first, index afterward
        final var lvalValue = object.execute(context);
        final var indexValue = this.index.execute(context);
        final var objectValue = LeftValue.toIndexable(lvalValue, indexValue);
        return objectValue.getIndex(indexValue);
    }
}
