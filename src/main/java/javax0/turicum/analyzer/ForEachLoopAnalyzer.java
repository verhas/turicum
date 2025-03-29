package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.*;

public class ForEachLoopAnalyzer implements Analyzer {
    public static final ForEachLoopAnalyzer INSTANCE = new ForEachLoopAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        lexes.peek(Lex.Type.IDENTIFIER, null, "'for each' needs an identifier");
        final var name = lexes.next().text;
        final var identifier = new Identifier(name);
        if (lexes.is(Keywords.IN)) {
            lexes.next();
        }
        final Command expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        ForLoopAnalyzer.checkClosingParen(lexes, withParentheses);

        final Command body = ForLoopAnalyzer.getLoopBody(lexes);
        final Command exitCondition = ForLoopAnalyzer.getOptionalExistCondition(lexes);
        return new ForEachLoop(identifier, expression, body, exitCondition);
    }
}
