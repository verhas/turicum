package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.commands.FunctionDefinition;

/**
 * <pre>{@code
 * fn myFunction a,b,c,d {
 * commands
 * }
 * }</pre>
 *
 * OR
 * <pre>{@code
 * fn myFunction (a,b,c,d) {
 * commands
 * }
 * }</pre>
 *
 * OR
 * <pre>{@code
 * fn (a,b,c,d) {
 * commands
 * }
 * }</pre>
 */
public class FunctionAnalyzer implements Analyzer {
    public static final FunctionAnalyzer INSTANCE = new FunctionAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final String fn;
        if (lexes.is("(")) {
            fn = null;
        } else {
            BadSyntax.when(!lexes.isIdentifier(), "function name expected after fn");
            fn = lexes.next().text;
        }
        final boolean needsParenthesis = lexes.is("(");
        if (needsParenthesis) {
            lexes.next();
        }
        final var arguments = IdentifierList.INSTANCE.analyze(lexes);
        if (needsParenthesis) {
            ExecutionException.when(lexes.isNot(")"), "Function parameter list is opened, but not closed using parenthesis");
            lexes.next();
        }
        final var block = BlockAnalyzer.INSTANCE.analyze(lexes);
        return new FunctionDefinition(fn, arguments, block);
    }
}
