package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.memory.*;

/**
 * LeftValue ::= Variable
 * | LeftValue '.' Identifier
 * | LeftValue '[' Expression ']'
 */
public class LeftValueAnalyzer {
    public static final LeftValueAnalyzer INSTANCE = new LeftValueAnalyzer();

    public LeftValue analyze(LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            return null;
        }
        final var lex = lexes.peek();
        if (lex.type() == Lex.Type.IDENTIFIER) {
            LeftValue left = new VariableLeftValue(lex.text());
            lexes.next();
            return getLeftValueTail(lexes, left);
        }
        if (lex.text.equals("{") && !ClosureAnalyzer.blockStartsClosure(lexes) &&
                !((lexes.isAt(1, Lex.Type.IDENTIFIER) || lexes.isAt(1, Lex.Type.STRING)) &&
                        lexes.isAt(2, ":"))) {
            LeftValue left = new CalculatedLeftValue(BlockAnalyzer.INSTANCE.analyze(lexes));
            if (lexes.isNot(".", "[")) {
                return null;
            }
            return getLeftValueTail(lexes, left);
        }
        return null;

    }

    private LeftValue getLeftValueTail(LexList lexes, LeftValue left) {
        while (lexes.is(".") || lexes.is("[")) {
            switch (lexes.peek().text()) {
                case ".":
                    lexes.next();
                    final var fieldName = lexes.next();
                    BadSyntax.when(lexes, fieldName.type() != Lex.Type.IDENTIFIER, "Field name is invalid");
                    left = new ObjectFieldLeftValue(left, fieldName.text());
                    break;
                case "[":
                    lexes.next();
                    final var indexExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                    lexes.next(Lex.Type.RESERVED, "]", "Array access needs ] at the end.");
                    left = new ArrayElementLeftValue(left, indexExpression);
                    break;
            }
        }
        return left;
    }
}
