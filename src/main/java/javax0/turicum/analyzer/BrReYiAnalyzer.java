package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ConstantExpression;

import java.util.function.BiFunction;

public class BrReYiAnalyzer {

    static Command analyze(Lex.List lexes, BiFunction<Command, Command, Command> breyiCommandFunction) throws BadSyntax {
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
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
