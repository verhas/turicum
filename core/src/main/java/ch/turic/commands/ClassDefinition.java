package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.ClassContext;
import ch.turic.memory.Context;
import ch.turic.memory.LngClass;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;

public class ClassDefinition extends AbstractCommand {
    final String className;
    final String[] parents;

    public BlockCommand body() {
        return body;
    }

    public String className() {
        return className;
    }

    public String[] parents() {
        return parents;
    }

    public static ClassDefinition factory(final Unmarshaller.Args args) {
        return new ClassDefinition(
                args.str("className"),
                args.get("parents",String[].class),
                args.get("body", BlockCommand.class));
    }

    public ClassDefinition(String className, String[] parents, BlockCommand body) {
        this.body = body;
        this.className = className;
        this.parents = parents;
    }

    final BlockCommand body;
    public static final LngClass[] NO_PARENTS = new LngClass[0];

    @Override
    public Object _execute(final Context context) throws ExecutionException {
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
        klass = new LngClass(ctx, Objects.requireNonNullElse(className, "#undefined"));
        ctx.local("cls", klass);
        body.execute(ctx);
        if (className != null) {
            context.local(className, klass);
        }
        return klass;
    }
}
