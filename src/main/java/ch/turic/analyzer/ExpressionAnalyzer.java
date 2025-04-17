package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.AsyncEvaluation;
import ch.turic.commands.Command;
import ch.turic.commands.Stream;

/**
 * <pre>{@code
 * expression ::= binary_expression[0]
 */
public class ExpressionAnalyzer extends AbstractAnalyzer {

    public final static Analyzer INSTANCE = new ExpressionAnalyzer();

    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.isKeyword()) {
            switch (lexes.peek().text()) {
                case "async":
                    lexes.next();
                    return new AsyncEvaluation(analyze(lexes));
                case "stream":
                    lexes.next();
                    final Command capacityExpression;
                    if( lexes.is("|")){
                        lexes.next();
                        capacityExpression = DefaultExpressionAnalyzer.INSTANCE.analyze(lexes);
                        BadSyntax.when(lexes,lexes.isNot("|"),"Capacity parameter starts with '|' and does not close with one");
                        lexes.next(); // step over the '|'
                    }else{
                        capacityExpression = null;
                    }
                    return new Stream(analyze(lexes), capacityExpression);
                default:
                    break;
            }
        }
        return BinaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }
}
