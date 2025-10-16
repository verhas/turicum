package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.*;

/**
 * The FunctionDefinitionAnalyzer is responsible for analyzing function definitions
 * in a lexical context. It parses function signatures, parameter lists, return types,
 * and function bodies, which may include block commands or expressions.
 * This class is a singleton and operates as an analyzer within the framework.
 * <p>
 * It extends the AbstractAnalyzer and utilizes a LexList to read and interpret
 * a sequence of lexemes for the purpose of creating a FunctionDefinition object.
 * <p>
 * Key operations performed by this class include:
 * - Determining whether the function being analyzed has a name or is anonymous.
 * - Parsing parameter lists and handling optional parentheses.
 * - Parsing and validating function return types.
 * - Handling both short-form function bodies and block bodies.
 * - Ensuring proper syntax for function definitions.
 * <p>
 * The resulting FunctionDefinition object encapsulates all necessary elements
 * of the function's structure for further interpretation or execution.
 * <p>
 * Responsibilities handled by this class may throw exceptions where syntax
 * errors or structural issues are identified during analysis.
 * <pre>
 * snippet EBNF_FN
 * FUNCTION ::= 'fn' (NAMED_FUNCTION | ANONYMOUS_FUNCTION)
 *
 * NAMED_FUNCTION ::= IDENTIFIER FUNCTION_BODY
 *                   | IDENTIFIER '(' PARAM_LIST ')' [RETURN_TYPE] FUNCTION_BODY
 *
 * ANONYMOUS_FUNCTION ::= '(' PARAMETER_LIST ')' [RETURN_TYPE] FUNCTION_BODY
 *
 * RETURN_TYPE ::=':'IDENTIFIER (','IDENTIFIER)*
 *
 * FUNCTION_BODY ::= BLOCK_BODY
 *                  |'='EXPRESSION
 *                  |';'
 *
 * BLOCK_BODY ::= '{' COMMAND* '}'
 *
 * end snippet
 * </pre>
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
        if (lexes.is(":")) {
            lexes.next();
            returnType = AssignmentList.getTheTypeDefinitions(lexes);
        } else {
            returnType = AssignmentList.EMPTY_TYPE;
        }
        final BlockCommand block;
        if (lexes.is("=")) {
            BadSyntax.when(lexes, !hasParens, "use must use parenthesis in function definition when using '=expression' as body");
            lexes.next();
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            block = new BlockCommand(new Command[]{expression}, false);
        } else if (lexes.is(";")) {
            BadSyntax.when(lexes, !hasParens, "use must use parenthesis in initialized without body");
            BadSyntax.when(lexes, !"init".equals(fn), "Only initializer can have no body");
            block = new BlockCommand(new Command[0], false);
        } else {
            block = (BlockCommand) BlockAnalyzer.INSTANCE.analyze(lexes);
        }
        return new FunctionDefinition(fn, arguments, returnType, block);
    }
}
