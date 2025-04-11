package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

/**
 * An identifier that identifies something, like a variable, a class or object.
 * Executing this "expression" will search the identified object and then return the value.
 * The search very much depends on the context.
 */
public class Identifier extends AbstractCommand {
    final String name;

    @Override
    public Object execute(Context ctx) throws ExecutionException {
        return ctx.get(name);
    }

    public String name() {
        return name;
    }

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
