package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.WithCommand;
import ch.turic.utils.Unmarshaller;

import java.util.ArrayList;

public class WithAnalyzer extends AbstractAnalyzer {
    public static final WithAnalyzer INSTANCE = new WithAnalyzer();

    public record WithPair(
            Command command,
            String alias) {

        public static WithPair factory(final Unmarshaller.Args args) {
            return new WithPair(
                    args.command("command"),
                    args.str("alias")
            );
        }
    }


    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        final var pairs = new ArrayList<WithPair>();
        while (true) {
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (lexes.is("as")) {
                lexes.next();
                if (!lexes.isIdentifier()) {
                    throw new ExecutionException("as has to be followed by an identifier");
                }
                final var id = lexes.next();
                pairs.add(new WithPair(expression, id.text()));
            } else {
                pairs.add(new WithPair(expression, null));
            }
            if (lexes.is(",")) {
                lexes.next();
            } else {
                break;
            }
        }
        checkClosingParen(lexes, withParentheses);
        final Command body = getBody(lexes);
        return new WithCommand(pairs.toArray(WithPair[]::new), body);
    }

    private static void checkClosingParen(LexList lexes, boolean withParentheses) throws BadSyntax {
        if (withParentheses) {
            BadSyntax.when(lexes, lexes.isNot(")"), "You have to close the parentheses in the 'for' or 'while' loop and 'with'");
            lexes.next();
        } else {
            BadSyntax.when(lexes, lexes.isNot(":", "{"), "'for' or 'while' loop and 'with' body has to be after '{' or ':'");
        }
    }

    private static Command getBody(LexList lexes) throws BadSyntax {
        Command body;
        if (lexes.is(":")) {
            lexes.next();
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is("{")) {
            body = BlockAnalyzer.UNWRAPPED.analyze(lexes);
        } else {
            throw lexes.syntaxError( ": or { is expected after the keyword 'with'");
        }
        return body;
    }
}
