package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.Context;

public class GlobalAssignment extends AbstractCommand {
    public AssignmentList.Assignment[] assignments() {
        return assignments;
    }

    public GlobalAssignment(AssignmentList.Assignment[] assignments) {
        this.assignments = assignments;
    }

    final AssignmentList.Assignment[] assignments;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object value = null;
        for (var assignment : assignments) {
            context.step();
            if (assignment.expression() == null) {
                context.global(assignment.identifier());
            } else {
                value = assignment.expression().execute(context);
                context.global(assignment.identifier(), value);
            }
        }
        return value;
    }
}
