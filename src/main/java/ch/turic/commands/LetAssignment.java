package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.Context;

public class LetAssignment extends AbstractCommand {
    final AssignmentList.Assignment[] assignments;

    public AssignmentList.Assignment[] assignments() {
        return assignments;
    }

    public LetAssignment(AssignmentList.Assignment[] assignments) {
        this.assignments = assignments;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        Object value = null;
        for (var assignment : assignments) {
            ctx.step();
            if (assignment.expression() == null) {
                    ctx.define(assignment.identifier(),
                            null,
                            assignment.types());
                    ctx.local(assignment.identifier(), null);
            } else {
                ctx.define(assignment.identifier(),
                        null,
                        assignment.types());
                value = assignment.expression().execute(ctx);
                ctx.local(assignment.identifier(), value);
            }
        }
        return value;
    }
}
