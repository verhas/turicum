package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;

public class MLetAssignment extends AbstractCommand {
    final AssignmentList.Assignment[] assignments;
    final Command rightHandSide;
    final Type type;

    public enum Type {
        LIST, OBJECT
    }

    public MLetAssignment(AssignmentList.Assignment[] assignments, Command rightHandSide, Type type) {
        this.assignments = assignments;
        this.rightHandSide = rightHandSide;
        this.type = type;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        Object value = rightHandSide.execute(ctx);
        switch (type) {
            case OBJECT:
                if (value instanceof HasFields fields) {
                    for (var assignment : assignments) {
                        ctx.step();
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
                        ctx.define(assignment.identifier(), null, typeNames);
                        ctx.local(assignment.identifier(), fields.getField(assignment.identifier()));
                    }
                } else {
                    throw new ExecutionException("{multi-let} assignment got a %s value does not have fields", value);
                }
                break;
            case LIST:
                if (value instanceof Iterable<?> iterable) {
                    final var iterator = iterable.iterator();
                    for (var assignment : assignments) {
                        ctx.step();
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
                        ctx.define(assignment.identifier(), null, typeNames);
                        if (!iterator.hasNext()) {
                            throw new ExecutionException("[multi-let] assignment right hand side has too few values", value);
                        }
                        ctx.local(assignment.identifier(), iterator.next());
                    }
                    if( iterator.hasNext() ) {
                        throw new ExecutionException("[multi-let] assignment right hand side has too many values", value);
                    }
                } else {
                    throw new ExecutionException("[multi-let] assignment got a %s value not a list", value);
                }
                break;
        }
        return value;
    }
}
