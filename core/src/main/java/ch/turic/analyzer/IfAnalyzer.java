package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Command;
import ch.turic.commands.If;

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
            } else if (lexes.is(":")) {
                lexes.next();
                final var t = CommandAnalyzer.INSTANCE.analyze(lexes);
                BadSyntax.when(lexes, t == null, "Empty command ( ';' ) must not be after 'else expression'");
                elseBlock = new BlockCommand(new Command[]{t}, true);
            } else {
                throw lexes.syntaxError(": or {", "Expected '{' after if condition");
            }
            return new If(condition, thenBlock, elseBlock);
        }
        return new If(condition, thenBlock, null);
    }
}
