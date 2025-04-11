package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.ExecutionException;
import javax0.turicum.commands.BlockCommand;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.FunctionDefinition;
import javax0.turicum.commands.ParameterList;

import java.util.List;

/**
 * <pre>{@code
 * fn myFunction a,b,c,d {
 * commands
 * }
 * }</pre>
 * <p>
 * OR
 * <pre>{@code
 * fn myFunction (a,b,c,d) {
 * commands
 * }
 * }</pre>
 * <p>
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
    public Command analyze(LexList lexes) throws BadSyntax {
        final String fn;
        if (lexes.is("(")) {
            fn = null;
        } else {
            BadSyntax.when(lexes, !lexes.isIdentifier(), "function name expected after fn");
            fn = lexes.next().text();
        }
        final boolean hasParens = lexes.is("(");
        if (hasParens) {
            lexes.next();
        }
        final ParameterList arguments;
        if (hasParens || lexes.isNot("{")) {
            arguments = ParameterDefinition.INSTANCE.analyze(lexes);
        } else {
            arguments = ParameterList.EMPTY;
        }
        if (hasParens) {
            ExecutionException.when(lexes.isNot(")"), "Function parameter list is opened, but not closed using parenthesis");
            lexes.next();
        }
        final BlockCommand block;
        if (lexes.is("=")) {
            BadSyntax.when(lexes, !hasParens, "use must use parenthesis in function definition when using =expression as body");
            lexes.next();
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            block = new BlockCommand(List.of(expression), false);
        } else {
            block = BlockAnalyzer.INSTANCE.analyze(lexes);
        }
        return new FunctionDefinition(fn, arguments, block);
    }
}
