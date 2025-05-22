package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LeftValue;
import ch.turic.memory.LngObject;

public class FieldAccess extends AbstractCommand {
    final Command object;
    final String identifier;
    final boolean lenient;

    public String identifier() {
        return identifier;
    }

    public Command object() {
        return object;
    }

    public FieldAccess(Command object, String identifier, boolean lenient) {
        this.identifier = identifier;
        this.object = object;
        this.lenient = lenient;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final var rawObject = this.object.execute(context);
        final HasFields object;
        if (rawObject == null) {
            ExecutionException.when(!lenient, "Cannot access the field '%s' because the object it is used on is undefined.", identifier);
            object = LngObject.newEmpty(context);
        } else {
            object = LeftValue.toObject(rawObject);
        }
        return object.getField(identifier);
    }
}
