package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.TryCatch;

public class TryCatchAnalyzer extends AbstractAnalyzer {
    public static final TryCatchAnalyzer INSTANCE = new TryCatchAnalyzer();

    public TryCatch _analyze(final LexList lexes) throws BadSyntax {
        final Command tryBlock;
        tryBlock = getCommand(lexes, Keywords.TRY);
        final Command catchBlock;
        final String exception;
        if (lexes.is(Keywords.CATCH)) {
            lexes.next();
            final var usesParen = lexes.is("(");
            if (usesParen) {
                lexes.next();
            }
            if (lexes.isIdentifier()) {
                exception = lexes.next().text;
                if (usesParen) {
                    BadSyntax.when(lexes, lexes.isNot(")"), "Missing ')' after the exception ");
                    lexes.next();
                }
            }else{
                exception = null;
            }
            catchBlock = getCommand(lexes, Keywords.CATCH);
        } else {
            catchBlock = null;
            exception = null;
        }

        final Command finallyBlock;
        if (lexes.is(Keywords.FINALLY)) {
            lexes.next();
            finallyBlock = getCommand(lexes, Keywords.FINALLY);
        } else {
            finallyBlock = null;
        }
        return new TryCatch(tryBlock, catchBlock, finallyBlock, exception);
    }

    private Command getCommand(LexList lexes, String msg) throws BadSyntax {
        final Command command;
        if (lexes.is("{")) {
            command = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is(":")) {
            lexes.next();
            command = CommandAnalyzer.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, command == null, "Empty command ( ';' ) following " + msg);
        } else {
            throw lexes.syntaxError( ": or {", "Expected ':' or '{'");
        }
        return command;
    }
}
