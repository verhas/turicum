package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.analyzer.AssignmentList;
import javax0.turicum.memory.Context;

public class GlobalAssignment extends AbstractCommand {
    public AssignmentList.Assignment[] assignments() {
        return assignments;
    }

    public GlobalAssignment(AssignmentList.Assignment[] assignments) {
        this.assignments = assignments;
    }

    final AssignmentList.Assignment[] assignments;

    @Override
    public Object execute(Context ctx) throws ExecutionException {
        Object value = null;
        for (var assignment : assignments) {
            ctx.step();
            if (assignment.expression() == null) {
                ctx.global(assignment.identifier());
            } else {
                value = assignment.expression().execute(ctx);
                ctx.global(assignment.identifier(), value);
            }
        }
        return value;
    }
}
