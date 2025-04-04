package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.*;
import javax0.turicum.memory.LeftValue;
import javax0.turicum.memory.VariableLeftValue;

public class ForLoopAnalyzer implements Analyzer {
    public static final ForLoopAnalyzer INSTANCE = new ForLoopAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if (lexes.is(Keywords.EACH)) {
            lexes.next();
            return ForEachLoopAnalyzer.INSTANCE.analyze(lexes);
        }
        final boolean withParentheses = lexes.is("(");
        if (withParentheses) {
            lexes.next();
        }
        final Command startCommand = getInitialAssignmentCommand(lexes);
        if (lexes.is(";")) {
            lexes.next();
        }
        final Command loopCondition;
        if (lexes.is(";")) {
            loopCondition = new ConstantExpression(true);
            lexes.next();
        } else {
            loopCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (lexes.is(";")) {
                lexes.next();
            }
        }
        Command stepCommand = CommandAnalyzer.INSTANCE.analyze(lexes);
        checkClosingParen(lexes, withParentheses);

        final Command body = getLoopBody(lexes);
        final Command exitCondition = getOptionalExistCondition(lexes);
        return new ForLoop(startCommand, loopCondition, exitCondition, stepCommand, body);
    }

    /**
     * Parses and returns the initialization command of a 'for' loop.
     * <p>
     * If the command following the 'for' keyword is a simple variable assignment (i.e., assigning a value to a standalone
     * variable, not to a field, array element, or any other complex expression), then the assignment is converted into
     * a {@link LetAssignment}. Otherwise, the original command is returned unchanged.
     * </p>
     * That way you do not need to write '{@code for let i = 0 ; ...}' instead of '{@code for i = 0 ; ...}'
     * @param lexes the list of lexical tokens representing the initialization part of the 'for' loop
     * @return a {@code LetAssignment} if the command is a simple variable assignment, or the original command otherwise
     * @throws BadSyntax if the initialization command contains invalid syntax
     */
    private Command getInitialAssignmentCommand(Lex.List lexes) throws BadSyntax {
        final Command startCommand = CommandAnalyzer.INSTANCE.analyze(lexes);
        if (startCommand instanceof Assignment(LeftValue leftValue, Command expression)
                && leftValue instanceof VariableLeftValue(String variable)) {
            return new LetAssignment(new AssignmentList.Pair[]{
                    new AssignmentList.Pair(variable, expression)
            }, false);
        } else {
            return startCommand;
        }
    }

    static void checkClosingParen(Lex.List lexes, boolean withParentheses) throws BadSyntax {
        if (withParentheses) {
            BadSyntax.when(lexes.isNot(")"), "You have to close the parentheses in the 'for' loop");
            lexes.next();
        } else {
            BadSyntax.when(lexes.isNot(":", "{"), "For loop body has to be after '{' or ':'");
        }
    }

    static Command getOptionalExistCondition(Lex.List lexes) throws BadSyntax {
        final Command exitCondition;
        if (lexes.is(Keywords.UNTIL)) {
            lexes.next();
            exitCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            Analyzer.checkCommandTermination(lexes);
        } else {
            exitCondition = new ConstantExpression(false);
        }
        return exitCondition;
    }

    /**
     * Get the loop body, the '{' or ':' starting if needed was already checked
     *
     * @param lexes the current lexical sequence
     * @return the read command
     * @throws BadSyntax if any underlying analysis throws up
     */
    static Command getLoopBody(Lex.List lexes) throws BadSyntax {
        Command body;
        if (lexes.is(":")) {
            lexes.next();
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is("{")) {
            body = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else {
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        }
        return body;
    }
}
