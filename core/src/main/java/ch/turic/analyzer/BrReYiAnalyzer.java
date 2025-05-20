package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.ConstantExpression;

import java.util.function.BiFunction;

public class BrReYiAnalyzer {

    static Command analyze(LexList lexes, BiFunction<Command, Command, Command> breyiCommandFunction) throws BadSyntax {
        final Command expression;
        if (lexes.is(Keywords.IF, Keywords.WHEN, ";" , "}")) {
            expression = null;
        } else {
            expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }
        final Command condition;
        if (lexes.is(Keywords.IF, Keywords.WHEN)) {
            lexes.next();
            condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        } else {
            condition = new ConstantExpression(true);
        }
        return breyiCommandFunction.apply(expression, condition);
    }
}
