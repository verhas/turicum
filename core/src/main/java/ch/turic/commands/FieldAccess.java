package ch.turic.commands;


import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

/**
 * Represents a command that accesses a specific field from an object.
 * FieldAccess is used to resolve the value of a field identified by its name
 * from an object while optionally handling undefined objects leniently.
 * <p>
 * The operator {@code ?.} will return {@code none} when the value on the left side is {@code none}.
 * This access is supported by the field {@code lenient}.
 */
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

    public static FieldAccess factory(final Unmarshaller.Args args) {
        return new FieldAccess(
                args.command("object"),
                args.str("identifier"),
                args.bool("lenient")
        ).fixPosition(args);
    }

    public FieldAccess(Command object, String identifier, boolean lenient) {
        this.object = object;
        this.identifier = identifier;
        this.lenient = lenient;
    }

    /**
     * A field access created internally by the runtime for method dispatch: a bare method call
     * inside a method is rewritten to a {@code this}/{@code cls} field access (see
     * {@code FunctionCallOrCurry.myFunctionObject}). It is exempt from the veil check, because a
     * method of the object may call the veiled methods and read the veiled fields.
     * <p>
     * Instances are created only during execution; they are never part of the analyzed command
     * tree and never get serialized.
     */
    public static class Internal extends FieldAccess {
        public Internal(Command object, String identifier) {
            super(object, identifier, false);
        }

        @Override
        public Object _execute(final LocalContext context) throws ExecutionException {
            final var raw = object().execute(context);
            return switch (raw) {
                case LngObject o -> o.getFieldUnveiled(identifier());
                case LngClass c -> c.getFieldUnveiled(identifier());
                case null ->
                        throw new ExecutionException("Cannot access the field '%s' because the object it is used on is undefined.", identifier());
                default -> LeftValue.toObject(raw).getField(identifier());
            };
        }
    }

    /**
     * Executes the field access command, retrieving the value of the specified field from the target object.
     * <p>
     * If the target object is undefined and lenient mode is enabled, returns a default empty object; otherwise, throws an ExecutionException.
     * For JavaObject instances, retrieves the field using the context; for other objects, retrieves the field directly.
     *
     * @param context the execution context
     * @return the value of the accessed field, or a default value if lenient and the object is undefined
     * @throws ExecutionException if the object is undefined and lenient mode is disabled
     */
    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        final var rawObject = this.object.execute(context);
        final HasFields object;
        if (rawObject == null) {
            ExecutionException.when(!lenient, "Cannot access the field '%s' because the object it is used on is undefined.", identifier);
            object = LngObject.newEmpty(context);
        } else {
            object = LeftValue.toObject(rawObject);
        }
        if( object instanceof JavaObject jo) {
            return jo.getField(identifier, context);
        }
        return object.getField(identifier);
    }
}
