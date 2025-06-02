package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ConstantExpression;
import ch.turic.commands.WhileLoop;

public class WhileLoopAnalyzer extends AbstractAnalyzer {
    public static final WhileLoopAnalyzer INSTANCE = new WhileLoopAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final Command startCondition;
        if (lexes.is("{", ":", Keywords.LIST)) {
            startCondition = new ConstantExpression(true);
        } else {
            startCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }
        final boolean resultList;
        if( lexes.is(Keywords.LIST)) {
            resultList = true;
            lexes.next();
        }else{
            resultList = false;
        }
        final Command body = ForLoopAnalyzer.getLoopBody(lexes);
        final Command exitCondition = ForLoopAnalyzer.getOptionalExistCondition(lexes);
        return new WhileLoop(startCondition, exitCondition, resultList, body);
    }
}
