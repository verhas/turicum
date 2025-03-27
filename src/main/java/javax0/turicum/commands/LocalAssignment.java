package javax0.turicum.commands;


import javax0.turicum.analyzer.AssignmentList;
import javax0.turicum.memory.Context;

public record LocalAssignment(AssignmentList.Pair[] assignments, boolean freeze) implements Command {

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        Object value = null;
        for (var assignment : assignments) {
            ctx.step();
            if (assignment.expression() == null) {
                value = null;
            } else {
                value = assignment.expression().execute(ctx);
            }
            ctx.local(assignment.identifier(), value);
            if (freeze) {
                ctx.freeze(assignment.identifier());
            }
        }
        return value;
    }
}
