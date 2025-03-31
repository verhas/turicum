package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ConstantExpression;
import javax0.turicum.commands.WhileLoop;

public class WhileLoopAnalyzer implements Analyzer {

    public static final WhileLoopAnalyzer INSTANCE = new WhileLoopAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        final Command startCondition;
        if (lexes.is("{", ":")) {
            startCondition = new ConstantExpression(true);
        } else {
            startCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }
        ForLoopAnalyzer.checkClosingParen(lexes, withParentheses);
        final Command body = ForLoopAnalyzer.getLoopBody(lexes);
        final Command exitCondition = ForLoopAnalyzer.getOptionalExistCondition(lexes);
        return new WhileLoop(startCondition, exitCondition, body);
    }

}
