package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;
import javax0.genai.pl.memory.LeftValue;

public record FieldAccess(Command object, String identifier) implements Command {
    private static final Command[] EMPTY_ARGS = new Command[0];

    @Override
    public Object execute(Context context) throws ExecutionException {
        final var rawObject = this.object.execute(context);
        ExecutionException.when(rawObject == null, "Cannot access field '" + identifier + "' on an undefined value " + this.object);
        final var object = LeftValue.toObject(rawObject);
        return object.getField(identifier);
    }
}
