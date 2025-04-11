package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public class FieldAccess extends AbstractCommand {
    final Command object;
    final String identifier;

    public String identifier() {
        return identifier;
    }

    public Command object() {
        return object;
    }

    public FieldAccess(Command object, String identifier) {
        this.identifier = identifier;
        this.object = object;
    }

    @Override
    public Object execute(Context context) throws ExecutionException {
        final var rawObject = this.object.execute(context);
        ExecutionException.when(rawObject == null, "Cannot access field '%s' because '%s' is undefined.", identifier, this.object);
        final var object = LeftValue.toObject(rawObject);
        return object.getField(identifier);
    }
}
