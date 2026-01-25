package ch.turic.analyzer;

import ch.turic.Command;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.ConstantExpression;
import ch.turic.commands.WhileLoop;
import ch.turic.exceptions.BadSyntax;

/**
 * snippet EBNF_WHILE
 * WHILE ::= [ 'loop'  ( BLOCK | ':' COMMAND ) ]
 * 'while' [EXPRESSION] ['list']( BLOCK | ':' COMMAND )
 * [ 'until' EXPRESSION ]
 * [ 'done' ( BLOCK | ':' COMMAND ) ]
 * [ 'otherwise' ( BLOCK | ':' COMMAND ) ]
 * [ 'finally' ( BLOCK | ':' COMMAND ) ]
 * <p>
 * end snippet
 */
public class WhileLoopAnalyzer extends AbstractAnalyzer {
    public static final WhileLoopAnalyzer WHILE_INSTANCE = new WhileLoopAnalyzer();
    public static final WhileLoopAnalyzer LOOP_INSTANCE = new WhileLoopAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final Command initBody;
        if (this == LOOP_INSTANCE) {
            final var wrapped  = LoopAnalyzerUtils.getLoopBody(lexes);
            if( wrapped instanceof BlockCommand block && block.wrap() ){
                initBody = new BlockCommand(block.commands(),false);
            }else{
                initBody = wrapped;
            }
            BadSyntax.when(lexes, !lexes.is(Keywords.WHILE), "while expected after loop keyword.");
            lexes.next();
        } else {
            initBody = null;
        }
        final Command startCondition;
        if (lexes.is("{", ":", Keywords.LIST)) {
            startCondition = new ConstantExpression(true);
        } else {
            startCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }
        final boolean resultList;
        if (lexes.is(Keywords.LIST)) {
            resultList = true;
            lexes.next();
        } else {
            resultList = false;
        }
        final var body = LoopAnalyzerUtils.getLoopBody(lexes);
        final Command exitCondition = LoopAnalyzerUtils.getOptionalExistCondition(lexes);
        final Command doneBody;
        if (lexes.is(Keywords.DONE)) {
            lexes.next();
            doneBody = LoopAnalyzerUtils.getLoopBody(lexes);
        } else {
            doneBody = null;
        }
        final Command otherwiseBody;
        if (lexes.is(Keywords.OTHERWISE)) {
            lexes.next();
            otherwiseBody = LoopAnalyzerUtils.getLoopBody(lexes);
        } else {
            otherwiseBody = null;
        }
        final Command finallyBody;
        if (lexes.is(Keywords.FINALLY)) {
            lexes.next();
            finallyBody = LoopAnalyzerUtils.getLoopBody(lexes);
        } else {
            finallyBody = null;
        }
        return new WhileLoop(initBody, startCondition, exitCondition, resultList, body, doneBody, otherwiseBody, finallyBody);
    }
}
