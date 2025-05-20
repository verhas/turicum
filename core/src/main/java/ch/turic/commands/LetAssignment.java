package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.Context;

public class LetAssignment extends AbstractCommand {
    final AssignmentList.Assignment[] assignments;
    final boolean mut;

    public AssignmentList.Assignment[] assignments() {
        return assignments;
    }

    public LetAssignment(AssignmentList.Assignment[] assignments, boolean mut) {
        this.assignments = assignments;
        this.mut = mut;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        Object value = null;
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
            if (assignment.expression() == null) {
                ctx.define(assignment.identifier(),
                        null,
                        typeNames);
                ctx.local(assignment.identifier(), null);
                if (!mut) {
                    ctx.freeze(assignment.identifier());
                }
            } else {
                value = assignment.expression().execute(ctx);
                ctx.defineTypeChecked(assignment.identifier(),
                        value,
                        typeNames);
                if (!mut) {
                    ctx.freeze(assignment.identifier());
                }
            }
        }
        return value;
    }
}
