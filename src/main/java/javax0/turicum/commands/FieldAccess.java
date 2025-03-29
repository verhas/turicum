package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public record FieldAccess(Command object, String identifier) implements Command {

    @Override
    public Object execute(Context context) throws ExecutionException {
        final var rawObject = this.object.execute(context);
        ExecutionException.when(rawObject == null, "Cannot access field '" + identifier + "' on an undefined value " + this.object);
        final var object = LeftValue.toObject(rawObject);
        return object.getField(identifier);
    }
}
