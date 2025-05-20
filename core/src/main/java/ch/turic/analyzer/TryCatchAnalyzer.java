package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Command;
import ch.turic.commands.EmptyObject;
import ch.turic.commands.TryCatch;

import java.util.Objects;

public class TryCatchAnalyzer extends AbstractAnalyzer {
    public static final TryCatchAnalyzer INSTANCE = new TryCatchAnalyzer();

    public TryCatch _analyze(final LexList lexes) throws BadSyntax {
        final Command tryCommand;
        final Command catchCommand;
        final String exception;
        tryCommand = getCommand(lexes, Keywords.TRY);
        if (lexes.is(Keywords.CATCH)) {
            lexes.next();
            exception = getExceptionVariableIdentifier(lexes);
            catchCommand = getCommand(lexes, Keywords.CATCH);
        } else {
            catchCommand = null;
            exception = null;
        }

        final Command finallyBlock;
        if (lexes.is(Keywords.FINALLY)) {
            lexes.next();
            finallyBlock = getCommand(lexes, Keywords.FINALLY);
        } else {
            finallyBlock = null;
        }
        return new TryCatch(tryCommand, catchCommand, finallyBlock, exception);
    }

    /**
     * Get the identifier where the exception will be stored if an error happens.
     *
     * @param lexes the token list
     * @return the name of the variable, or {@code null} if there is no variable
     */
    private String getExceptionVariableIdentifier(LexList lexes) {
        // '(' and ')' around the exception name is optional to give a bit of ease for old style Java/C ... programmers
        final var usesParen = lexes.is("(");
        if (usesParen) {
            lexes.next();
        }
        final String exception;
        if (lexes.isIdentifier()) {
            exception = lexes.next().text;
            if (usesParen) {
                BadSyntax.when(lexes, lexes.isNot(")"), "Missing ')' after the exception ");
                lexes.next();
            }
            return exception;
        } else {
            return null;
        }
    }

    /**
     * Get a command. If it starts with '{' then it is a block command. If it starts with a ':' it is a normal command.
     *
     * @param lexes the list of tokens
     * @param msg   message to put into error if there is any error
     * @return the command
     * @throws BadSyntax if there was a syntax error
     */
    private Command getCommand(LexList lexes, String msg) throws BadSyntax {
        final Command command;
        if (lexes.is("{")) {
            command = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is(":")) {
            lexes.next();
            command = Objects.requireNonNullElse(CommandAnalyzer.INSTANCE.analyze(lexes), BlockCommand.EMPTY_BLOCK);
        } else {
            throw lexes.syntaxError(": or {", "Expected ':' or '{'");
        }
        return command;
    }
}
