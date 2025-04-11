package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.ClassContext;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngClass;

import java.util.Objects;

public class ClassDefinition extends AbstractCommand {
    final String className;
    final String[] parents;
    final String[] parameters;

    public BlockCommand body() {
        return body;
    }

    public String className() {
        return className;
    }

    public String[] parameters() {
        return parameters;
    }

    public String[] parents() {
        return parents;
    }

    public ClassDefinition(String className, String[] parents, String[] parameters, BlockCommand body) {
        this.body = body;
        this.className = className;
        this.parents = parents;
        this.parameters = parameters;
    }

    final BlockCommand body;
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
        final var ctx = new ClassContext(context, parentClasses);
        klass = new LngClass(ctx, parameters, Objects.requireNonNullElse(className, "#undefined"));
        ctx.local("cls", klass);
        body.execute(ctx);

        if (className != null) {
            context.local(className, klass);
        }
        return klass;
    }
}
