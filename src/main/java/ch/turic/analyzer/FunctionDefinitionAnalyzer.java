package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.commands.*;

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
public class FunctionDefinitionAnalyzer extends AbstractAnalyzer {
    public static final FunctionDefinitionAnalyzer INSTANCE = new FunctionDefinitionAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final String fn;
        if (lexes.is("(")) {
            fn = null;
        } else {
            BadSyntax.when(lexes, !lexes.isIdentifier(), "function name or '(' expected after fn. You cannot omit the '()' for anonymous function.");
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
        final TypeDeclaration[] returnType;
        if (lexes.is(":", "->")) {
            lexes.next();
            returnType = AssignmentList.getTheTypeDefinitions(lexes);
        }else{
            returnType = AssignmentList.EMPTY_TYPE;
        }
        final BlockCommand block;
        if (lexes.is("=")) {
            BadSyntax.when(lexes, !hasParens, "use must use parenthesis in function definition when using '=expression' as body");
            lexes.next();
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            block = new BlockCommand(List.of(expression), false);
        } else {
            block = (BlockCommand) BlockAnalyzer.INSTANCE.analyze(lexes);
        }
        return new FunctionDefinition(fn, arguments, returnType,block);
    }
}
