package javax0.turicum.analyzer;

import javax0.turicum.commands.BreakCommand;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ConstantExpression;

public class BreakAnalyzer implements Analyzer {
    public static final BreakAnalyzer INSTANCE = new BreakAnalyzer();
    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        final Command condition;
        if( lexes.is(Keywords.IF)){
            lexes.next();
            condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }else{
            condition = new ConstantExpression(true);
        }
        return new BreakCommand(expression,condition);
    }
}
