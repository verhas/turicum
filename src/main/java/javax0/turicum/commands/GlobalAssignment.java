package javax0.turicum.commands;

import javax0.turicum.analyzer.AssignmentList;
import javax0.turicum.memory.Context;

public record GlobalAssignment(AssignmentList.Pair[] assignments) implements Command {
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
