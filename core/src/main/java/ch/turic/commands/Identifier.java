package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.Input;
import ch.turic.analyzer.BlockAnalyzer;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;

/**
 * An identifier that identifies something, like a variable, a class or object.
 * Executing this "expression" will search the identified object and then return the value.
 * The search very much depends on the context.
 */
public class Identifier extends AbstractCommand {
    final private String name;
    final private Command[] commands;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        return context.get(name(context));
    }

    public boolean isInterpolated(){
        return commands != null;
    }

    public String pureName(){
        if( isInterpolated() ){
            throw new ExecutionException("Identifier %s is interpolated",name);
        }
        return name;
    }

    public String name(final Context context) {
        if (isInterpolated()) {
            final var sb = new StringBuilder();
            for (Command command : commands) {
                sb.append(Objects.requireNonNullElse(command.execute(context), "none"));
            }
            return sb.toString();
        } else {
            return name;
        }
    }

    public Identifier(String name, Command[] commands) {
        this.name = name;
        this.commands = commands;
    }

    public Identifier(Lex lex) {
        this(lex.text(),  lex.interpolated);
    }
    public Identifier(String name) {
        this(name,  false);
    }

    public Identifier(String name, boolean interpolated) {
        Objects.requireNonNull(name);
        if (interpolated) {
            final var parts = StringConstant.split(name);
            commands = new Command[parts.length];
            for (int i = 0; i < parts.length; i += 2) {
                commands[i] = new StringConstant(parts[i], false);
            }
            for (int i = 1; i < parts.length; i += 2) {
                final var lexes = Lexer.analyze((ch.turic.analyzer.Input) Input.fromString(parts[i]));
                commands[i] = lexes.is("(") ? BlockAnalyzer.FLAT.analyze(lexes) : BlockAnalyzer.INSTANCE.analyze(lexes);
            }
            this.name = name;
        } else {
            this.name = name;
            this.commands = null;
        }
    }

    public static Identifier factory(Unmarshaller.Args args) {
        return new Identifier(args.str("name"),
                args.commands());
    }

    @Override
    public String toString() {
        return name;
    }
}
