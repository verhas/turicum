package ch.turic.commands;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class ClosureDefinition extends AbstractCommand {
    public final ParameterList arguments;

    public ParameterList arguments() {
        return arguments;
    }

    public BlockCommand body() {
        return body;
    }

    public ClosureDefinition(ParameterList arguments, BlockCommand body) {
        this.arguments = arguments;
        this.body = body;
    }

    public static ClosureDefinition factory(final Unmarshaller.Args args) {
        return new ClosureDefinition(
                args.get("arguments", ParameterList.class),
                args.get("body", BlockCommand.class)
        );
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        return new Closure(null, arguments, context, null, body);
    }
}
