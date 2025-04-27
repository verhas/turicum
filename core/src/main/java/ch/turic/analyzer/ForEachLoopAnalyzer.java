package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.*;

public class ForEachLoopAnalyzer extends AbstractAnalyzer {
    public static final ForEachLoopAnalyzer INSTANCE = new ForEachLoopAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        lexes.peek(Lex.Type.IDENTIFIER, null, "'for each' needs an identifier");
        final var name = lexes.next().text();
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
