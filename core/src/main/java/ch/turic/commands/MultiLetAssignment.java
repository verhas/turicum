package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.utils.Unmarshaller;

/**
 * Command implementation for multi-let assignments that handles both list and object assignments.
 * This class supports assigning multiple values from either a list or an object to multiple variables.
 */
public class MultiLetAssignment extends AbstractCommand {
    final AssignmentList.Assignment[] assignments;
    final Command rightHandSide;
    final Type type;
    final boolean mut;

    public enum Type {
        LIST, OBJECT
    }

    public static MultiLetAssignment factory(final Unmarshaller.Args args) {
        return new MultiLetAssignment(
                args.get("assignments", AssignmentList.Assignment[].class),
                args.command("rightHandSide"),
                args.get("type", Type.class),
                args.bool("mut")
        );
    }

    /**
     * Constructs a new MultiLetAssignment command.
     *
     * @param assignments   Array of assignments to be processed
     * @param rightHandSide Command that produces the value to be assigned
     * @param type          Type of assignment (LIST or OBJECT)
     */
    public MultiLetAssignment(AssignmentList.Assignment[] assignments, Command rightHandSide, Type type, boolean mut) {
        this.assignments = assignments;
        this.rightHandSide = rightHandSide;
        this.type = type;
        this.mut = mut;
    }

    /**
     * Executes the multi-let assignment command.
     *
     * @param ctx The execution context
     * @return The value that was assigned
     * @throws ExecutionException if the assignment fails
     */
    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        final var value = rightHandSide.execute(ctx);
        switch (type) {
            case OBJECT:
                handleObjectAssignment(ctx, value);
                break;
            case LIST:
                handleListAssignment(ctx, value);
                break;
        }
        return value;
    }

    /**
     * Handles assignment from an iterable value to multiple variables.
     *
     * @param ctx   The execution context
     * @param value The iterable value to assign from
     * @throws ExecutionException if the value is not iterable or if the number of values doesn't match assignments
     */
    private void handleListAssignment(Context ctx, Object value) {
        if (value instanceof Iterable<?> iterable) {
            final var iterator = iterable.iterator();
            for (var assignment : assignments) {
                ctx.step();
                final String[] typeNames = getTypeNames(ctx, assignment);
                if (!iterator.hasNext()) {
                    throw new ExecutionException("[multi-let] assignment right hand side has too few values", value);
                }
                defineOrUpdate(ctx,assignment.identifier().name(ctx), iterator.next(), typeNames);
            }
            if( iterator.hasNext() ) {
                throw new ExecutionException("[multi-let] assignment right hand side has too many values", value);
            }
        } else {
            throw new ExecutionException("[multi-let] assignment got a %s value not a list", value);
        }
    }


    /**
     * Handles assignment from an object with fields to multiple variables.
     *
     * @param ctx   The execution context
     * @param value The object containing fields to assign from
     * @throws ExecutionException if the value doesn't implement HasFields interface
     */
    private void handleObjectAssignment(Context ctx, Object value) {
        if (value instanceof HasFields fields) {
            for (var assignment : assignments) {
                ctx.step();
                final String[] typeNames = getTypeNames(ctx, assignment);
                defineOrUpdate(ctx, assignment.identifier().name(ctx), fields.getField(assignment.identifier().name(ctx)), typeNames);
            }
        } else {
            throw new ExecutionException("{multi-let} assignment got a %s value does not have fields", value);
        }
    }

    private void defineOrUpdate(final Context ctx, final String identifier, Object value, String[] typeNames) {
        if( mut ){
            if( typeNames != null && typeNames.length > 0 ){
                ctx.defineTypeChecked(identifier, value, typeNames);
            }else{
                if( ctx.contains(identifier)){
                    ctx.update(identifier, value);
                }else{
                    ctx.defineTypeChecked(identifier, value, typeNames);
                }
            }
        }else{
            ctx.defineTypeChecked(identifier, value, typeNames);
            ctx.freeze(identifier);
        }
    }

    /**
     * Calculates type names for an assignment.
     *
     * @param ctx        The execution context
     * @param assignment The assignment to get type names for
     * @return Array of type names, or null if no types specified
     */
    private String[] getTypeNames(Context ctx, AssignmentList.Assignment assignment) {
        final String[] typeNames;
        if (assignment.types() == null) {
            typeNames = null;
        } else {
            typeNames = new String[assignment.types().length];
            for (int i = 0; i < assignment.types().length; i++) {
                final var type = assignment.types()[i];
                typeNames[i] = type.calculateTypeName(ctx);
            }
        }
        return typeNames;
    }
}
