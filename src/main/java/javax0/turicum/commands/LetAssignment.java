package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.analyzer.AssignmentList;
import javax0.turicum.memory.Context;

public record LetAssignment(AssignmentList.Assignment[] assignments) implements Command {

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
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
