package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

/**
 * An identifier that identifies something, like a variable, a class or object.
 * Executing this "expression" will search the identified object and then return the value.
 * The search very much depends on the context.
 */
public class Identifier extends AbstractCommand {
    final String name;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        return context.get(name);
    }

    public String name() {
        return name;
    }

    public Identifier(String name) {
        this.name = name;
    }

    public static Identifier factory(Unmarshaller.Args args) {
        return new Identifier(args.str("name"));
    }

    @Override
    public String toString() {
        return name;
    }
}
