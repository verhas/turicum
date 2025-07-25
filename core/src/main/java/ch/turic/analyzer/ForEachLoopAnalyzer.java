package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ForEachLoop;
import ch.turic.commands.Identifier;

/**
 * snippet EBNF_FOR_EACH
 * // the conventional '(' and ')' around the expressions giving the iterable is optional
 * FOR_EACH ::= 'for each' identifier [with identifier]
 *                  'in' EXPRESSION ['list']( BLOCK | ':' COMMAND ) |
 *              'for each' '(' identifier ['with' identifier]
 *                  'in' EXPRESSION ')' ['list'](  BLOCK | ':' COMMAND )
 * end snippet
 */
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
        final Identifier with;
        if (lexes.is(Keywords.WITH)) {
            lexes.next();
            lexes.peek(Lex.Type.IDENTIFIER, null, "'for each... with' needs an identifier");
            with = new Identifier(lexes.next().text());
        } else {
            with = null;
        }
        if (lexes.is(Keywords.IN)) {
            lexes.next();
        }
        final Command expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        ForLoopAnalyzer.checkClosingParen(lexes, withParentheses);
        final boolean resultList;
        if (lexes.is(Keywords.LIST)) {
            resultList = true;
            lexes.next();
        } else {
            resultList = false;
        }

        final Command body = ForLoopAnalyzer.getLoopBody(lexes);
        final Command exitCondition = ForLoopAnalyzer.getOptionalExistCondition(lexes);
        return new ForEachLoop(identifier, with, expression, resultList, body,  exitCondition);
    }
}
