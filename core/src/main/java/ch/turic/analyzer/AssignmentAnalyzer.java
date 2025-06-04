package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.Assignment;
import ch.turic.commands.IncrementDecrement;

public class AssignmentAnalyzer extends AbstractAnalyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.is("++")) {
            lexes.next();
            final var leftValue = LeftValueAnalyzer.INSTANCE.analyze(lexes);
            if (leftValue == null) {
                throw lexes.syntaxError("Invalid left value following ++");
            }
            return new IncrementDecrement(leftValue, true, false);
        }
        if (lexes.is("--")) {
            lexes.next();
            final var leftValue = LeftValueAnalyzer.INSTANCE.analyze(lexes);
            if (leftValue == null) {
                throw lexes.syntaxError("Invalid left value following ++");
            }
            return new IncrementDecrement(leftValue, false, false);
        }
        final var leftValue = LeftValueAnalyzer.INSTANCE.analyze(lexes);
        if (leftValue == null) {
            return null;
        }
        if (!lexes.hasNext() || !(lexes.peek().type() == Lex.Type.RESERVED)) {
            return null;
        }
        if (lexes.is("++")) {
            lexes.next();
            return new IncrementDecrement(leftValue, true, true);
        }
        if (lexes.is("--")) {
            lexes.next();
            return new IncrementDecrement(leftValue, false, true);
        }
        if (!isAssignmentOperator(lexes)) {
            return null;
        }
        final var op = lexes.next().text();
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        return new Assignment(leftValue, op.substring(0,op.length()-1), expression);
    }

    private boolean isAssignmentOperator(LexList lexes) {
        return lexes.is(Lexer.ASSIGNMENT_OPERATORS);
    }
}
