package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.ConstantExpression;
import ch.turic.commands.WhileLoop;
import ch.turic.commands.WithCommand;

import java.util.ArrayList;

public class WithAnalyzer extends AbstractAnalyzer {
    public static final WithAnalyzer INSTANCE = new WithAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        final var commands = new ArrayList<Command>();
        while(true) {
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            commands.add(expression);
            if(lexes.is(",")){
                lexes.next();
            }else{
                break;
            }
        }
        ForLoopAnalyzer.checkClosingParen(lexes,withParentheses);
        final Command body = ForLoopAnalyzer.getLoopBody(lexes);
        return new WithCommand(commands.toArray(Command[]::new),body);
    }
}
