package ch.turic.analyzer;


import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.Print;

import java.util.ArrayList;

public class PrintAnalyzer extends AbstractAnalyzer {
    public static final PrintAnalyzer INSTANCE = new PrintAnalyzer(false);
    public static final PrintAnalyzer INSTANCE_NL = new PrintAnalyzer(true);

    private final boolean nl;

    public PrintAnalyzer(boolean nl) {
        this.nl = nl;
    }

    /**
     * Analyzes a lexical list to parse a 'print' command or expression. It checks for
     * optional parentheses and processes comma-separated expressions or single expressions.
     * Based on the analysis, it creates and returns a corresponding Print command object.
     * <p>
     * The method first checks for an opening parenthesis. If multiple expressions are found
     * (comma-separated), the parentheses are treated as function call syntax and must be
     * properly closed. However, if there is only one expression, any opening parenthesis
     * is considered part of that expression, allowing for expressions that start with
     * but are not fully enclosed by parentheses (e.g., {@code println ({"a":1}).a}).
     *
     * @param lexes the lexical list to analyze for a 'print' command or expression
     * @return a Print command object representing the parsed expressions
     * @throws BadSyntax if there are syntax errors during the analysis
     */
    public Command _analyze(LexList lexes) throws BadSyntax {
        final boolean enclosed = lexes.is("(");
        final var index = lexes.getIndex();
        if (enclosed) {
            lexes.next();
            if (lexes.is(")")) {
                lexes.next();
                return new Print(new Command[0], nl);
            }
        }

        final var expressions = getCommaSeparatedExpressions(lexes);
        if (expressions.size() < 2 && enclosed) {
            lexes.setIndex(index);
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (expression == null) {
                return new Print(new Command[0], nl);
            }
            return new Print(new Command[]{expression}, nl);
        }
        if (enclosed) {
            if (lexes.isNot(")")) {
                throw lexes.syntaxError("parameters following 'print(' should also have a closing ')' or drop the opening '(' also");
            }
            lexes.next();
        }
        return new Print(expressions.toArray(Command[]::new), nl);
    }

    /**
     * Parses a list of comma-separated expressions from the provided lexical list.
     * This method analyzes each expression and adds it to a new list of commands.
     *
     * @param lexes the lexical list to analyze for expressions
     * @return an ArrayList containing the parsed commands
     */
    private ArrayList<Command> getCommaSeparatedExpressions(final LexList lexes) {
        final var expressions = new ArrayList<Command>();
        while (lexes.isNot(";")) {
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (expression != null) {
                expressions.add(expression);
            }
            if (lexes.isNot(",")) {
                break;
            }
            lexes.next();
        }
        return expressions;
    }
}
