package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.Identifier;
import ch.turic.commands.Pin;

import java.util.ArrayList;

public class PinAnalyzer extends AbstractAnalyzer {
    public static final PinAnalyzer INSTANCE = new PinAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var items = new ArrayList<Pin.Item>();
        while (lexes.isIdentifier() || lexes.is("[", "{")) {
            final Pin.Item.Type type;
            if (lexes.isIdentifier()) {
                type = Pin.Item.Type.VARIABLE;
            } else if (lexes.is("{")) {
                type = Pin.Item.Type.OBJECT;
                lexes.next();
            } else if (lexes.is("[")) {
                type = Pin.Item.Type.LIST;
                lexes.next();
            } else {
                throw new RuntimeException("Unreachable 68352");
            }
            if (!lexes.isIdentifier()) {
                throw new ExecutionException("Identifier expected in pin list");
            }
            final var id = new Identifier(lexes.next().text());
            items.add(new Pin.Item(id, type));
            switch (type) {
                case VARIABLE:
                    break;
                case OBJECT:
                    ExecutionException.when(lexes.isNot("}"), "} missing after object identifier in pin list");
                    lexes.next();
                    break;
                case LIST:
                    ExecutionException.when(lexes.isNot("]"), "] missing after list identifier in pin list");
                    lexes.next();
                    break;
                default:
                    throw new ExecutionException("Unreachable 68353");
            }
            if (lexes.isNot(",")) {
                break;
            }
            lexes.next();
        }

        return new Pin(items.toArray(Pin.Item[]::new));
    }
}
