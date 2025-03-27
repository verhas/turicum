package javax0.turicum.commands;

import javax0.turicum.memory.ClassContext;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngClass;

import java.util.Objects;

public record ClassDefinition(String className, String[] parents, String[] parameters,
                              BlockCommand body) implements Command {
    public static final LngClass[] NO_PARENTS = new LngClass[0];

    @Override
    public Object execute(Context context) throws ExecutionException {
        final LngClass klass;
        final var parentClasses = parents == null ? NO_PARENTS : new LngClass[parents.length];
        for (int i = 0; parents != null && i < parents.length; i++) {
            final var p = context.get(parents[i]);
            if (p instanceof LngClass lngParent) {
                parentClasses[i] = lngParent;
            } else {
                throw new ExecutionException("'" + parents[i] + "' is not defined or not a LngClass");
            }
        }
        final var ctx = new ClassContext(parentClasses);
        klass = new LngClass(ctx, parameters, Objects.requireNonNullElse(className,"#undefined"));
        body.execute(ctx);

        if (className != null) {
            context.local(className, klass);
        }
        return klass;
    }
}
