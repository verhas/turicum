package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.analyzer.AssignmentList;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class GlobalAssignment extends AbstractCommand {
    final AssignmentList.Assignment[] assignments;

    public static GlobalAssignment factory(final Unmarshaller.Args args) {
        return new GlobalAssignment(
                args.get("assignments", AssignmentList.Assignment[].class)
        );
    }

    public GlobalAssignment(AssignmentList.Assignment[] assignments) {
        this.assignments = assignments;
    }

    public AssignmentList.Assignment[] assignments() {
        return assignments;
    }

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
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
