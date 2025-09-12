package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.Command;
import ch.turic.commands.If;

/**
 * snippet EBNF_IF
 * IF ::= 'if' EXPRESSION ( ':' COMMAND | BLOCK ) [ 'else' ( [':'] COMMAND | BLOCK )] ;
 * end snippet
 * <p>
 * Analyzes an 'if' statement.
 * The expression following the statement will be evaluated as a Boolean value, and based on that, the
 * "then block" or "else block" will be executed.
 * <p>
 * An optional 'elseif' statement may follow the 'if' statement.
 * <p>
 * The command following the 'if' and the 'else' may be a block or a single command.
 * Both can be blocks, or none, or only one of them.
 * Using a block after the 'if' part does NOT mean you must also use a block after the 'else' part.
 * <p>
 * When using a single command after the 'if' part, it is necessary to use a ':' before the command.
 * This clearly separates the conditional expression and the command to be executed.
 * <p>
 * To provide symmetry, you can also use a ':' following the 'else' part, but it is optional.
 *
 */
public class IfAnalyzer extends AbstractAnalyzer {
    public static final IfAnalyzer INSTANCE = new IfAnalyzer();

    public If _analyze(LexList lexes) throws BadSyntax {
        // There is no need for '(' and ')' after the 'if'.
        // If there is a pair, it is part of the expression.
        final var condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        final Command thenBlock;
        if (lexes.is("{")) {
            thenBlock = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is(":")) {
            lexes.next();
            final var t = CommandAnalyzer.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, t == null, "Empty command ( ';' ) must not be after 'if expression' or 'elseif expression'");
            thenBlock = t;
        } else {
            throw lexes.syntaxError(": or {", "Expected ':' or '{' after if condition");
        }

        if (lexes.is(Keywords.ELSEIF)) {
            return new If(condition, thenBlock, IfAnalyzer.INSTANCE.analyze(lexes));
        }
        if (lexes.is(Keywords.ELSE)) {
            lexes.next();
            if (lexes.is(Keywords.IF)) { // we allow not only elseif but also 'else if'
                return new If(condition, thenBlock, IfAnalyzer.INSTANCE.analyze(lexes));
            }
            final Command elseBlock;
            if (lexes.is("{")) {
                elseBlock = BlockAnalyzer.INSTANCE.analyze(lexes);
            } else {
                if (lexes.is(":")) {
                    lexes.next();
                }
                final var t = CommandAnalyzer.INSTANCE.analyze(lexes);
                BadSyntax.when(lexes, t == null, "Empty command ( ';' ) must not be after 'else expression'");
                elseBlock = new BlockCommand(new Command[]{t}, true);
            }
            return new If(condition, thenBlock, elseBlock);
        }
        return new If(condition, thenBlock, null);
    }
}
