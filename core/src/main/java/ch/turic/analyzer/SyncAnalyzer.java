package ch.turic.analyzer;

import ch.turic.Command;
import ch.turic.commands.SyncCommand;
import ch.turic.exceptions.BadSyntax;

/**
 * Analyzes the {@code sync} command. The grammar is
 * {@code sync expression '{' commands '}'} or {@code sync expression ':' command}.
 * <p>
 * The expression has to evaluate to a mutex during execution. The expression is parsed with the
 * expression analyzer (not the command analyzer), so the opening {@code '{'} of the body is not
 * mistaken for a block argument of a parenthesis-less function call.
 */
public class SyncAnalyzer extends AbstractAnalyzer {
    public static final SyncAnalyzer INSTANCE = new SyncAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var mutexExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        final Command body;
        if (lexes.is("{")) {
            body = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is(":")) {
            lexes.next();
            final var command = CommandAnalyzer.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, command == null, "Empty command ( ';' ) must not be after 'sync expression :'");
            body = command;
        } else {
            throw lexes.syntaxError("Expected ':' or '{' after 'sync expression'");
        }
        return new SyncCommand(mutexExpression, body);
    }
}
