package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ConstantExpression;
import javax0.turicum.commands.ForLoop;

public class ForLoopAnalyzer implements Analyzer {
    public static final ForLoopAnalyzer INSTANCE = new ForLoopAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if( lexes.is(Keywords.EACH)){
            lexes.next();
            return ForEachLoopAnalyzer.INSTANCE.analyze(lexes);
        }
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        final Command startCommand = CommandAnalyzer.INSTANCE.analyze(lexes);
        if (lexes.is(";")) {
            lexes.next();
        }
        final Command loopCondition;
        if (lexes.is(";")) {
            loopCondition = new ConstantExpression(true);
            lexes.next();
        } else {
            loopCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (lexes.is(";")) {
                lexes.next();
            }
        }
        Command stepCommand = CommandAnalyzer.INSTANCE.analyze(lexes);
        checkClosingParen(lexes, withParentheses);

        final Command body = getLoopBody(lexes);
        final Command exitCondition = getOptionalExistCondition(lexes);
        return new ForLoop(startCommand, loopCondition, exitCondition, stepCommand, body);
    }

    static void checkClosingParen(Lex.List lexes, boolean withParentheses) throws BadSyntax {
        if (withParentheses) {
            BadSyntax.when(lexes.isNot(")"), "You have to close the parentheses in the 'for' loop");
            lexes.next();
        } else {
            BadSyntax.when(lexes.isNot(":", "{"), "For loop body has to be after '{' or ':'");
        }
    }

    static Command getOptionalExistCondition(Lex.List lexes) throws BadSyntax {
        final Command exitCondition;
        if (lexes.is(Keywords.UNTIL)) {
            lexes.next();
            exitCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            Analyzer.checkCommandTermination(lexes);
        } else {
            exitCondition = new ConstantExpression(false);
        }
        return exitCondition;
    }

    /**
     * Get the loop body, the '{' or ':' starting if needed was already checked
     * @param lexes the current lexical sequence
     * @return the read command
     * @throws BadSyntax if any underlying analysis throws up
     */
    static Command getLoopBody(Lex.List lexes) throws BadSyntax {
        Command body;
        if (lexes.is(":")) {
            lexes.next();
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is("{")) {
            body = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else {
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        }
        return body;
    }
}
