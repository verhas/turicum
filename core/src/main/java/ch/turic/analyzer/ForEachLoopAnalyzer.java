package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ForEachLoop;
import ch.turic.commands.Identifier;

import java.util.ArrayList;

/**
 * snippet EBNF_FOR_EACH
 * // the conventional '(' and ')' around the expressions giving the iterable is optional
 * FOR_EACH ::= 'for each' identifier [with identifier]
 * 'in' EXPRESSION ['list']( BLOCK | ':' COMMAND ) |
 * 'for each' '(' identifier ['with' identifier]
 * 'in' EXPRESSION ')' ['list'](  BLOCK | ':' COMMAND )
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

        final Identifier[] identifiers;
        final boolean listLoopVar;
        if (lexes.is("[")) {
            listLoopVar = true;
            lexes.next();
            final var idList = new ArrayList<Identifier>();
            while(true) {
                lexes.peek(Lex.Type.IDENTIFIER, null, "'for each' needs an identifier");
                final var name = lexes.next().text();
                idList.add(new Identifier(name));
                if( lexes.isNot(",") ) {
                    break;
                }
                lexes.next();
            }
            if(lexes.isNot("]")){
                throw lexes.syntaxError( "']' expected following '[' in for each loop" );
            }
            lexes.next();
            identifiers = idList.toArray(Identifier[]::new);
        } else {
            listLoopVar = false;
            lexes.peek(Lex.Type.IDENTIFIER, null, "'for each' needs an identifier");
            final var name = lexes.next().text();
            identifiers = new Identifier[]{new Identifier(name)};
        }
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
        return new ForEachLoop(identifiers, listLoopVar, with, expression, resultList, body, exitCondition);
    }
}
