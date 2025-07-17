package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.Assignment;
import ch.turic.commands.IncrementDecrement;

public class AssignmentAnalyzer extends AbstractAnalyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    /**
     * Analyzes the given lexical tokens to identify and construct assignment or increment/decrement commands.
     * <p>
     * Recognizes and returns commands for prefix and postfix increment (`++`) and decrement (`--`) operations,
     * as well as assignment operations using any supported assignment operator. Throws a syntax error if an
     * increment or decrement operator is not followed by a valid left value.
     *
     * @param lexes the list of lexical tokens to analyze
     * @return a command representing an assignment or increment/decrement operation, or {@code null} if no valid operation is found
     * @throws BadSyntax if a prefix increment or decrement operator is not followed by a valid left value
     */
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

    /**
     * Determines whether the current token in the given lexical list is an assignment operator.
     *
     * @param lexes the list of lexical tokens to check
     * @return true if the current token is an assignment operator; false otherwise
     */
    private boolean isAssignmentOperator(LexList lexes) {
        return lexes.is(Lexer.ASSIGNMENT_OPERATORS);
    }
}
