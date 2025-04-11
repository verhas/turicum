package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.memory.ArrayElementLeftValue;
import javax0.turicum.memory.LeftValue;
import javax0.turicum.memory.ObjectFieldLeftValue;
import javax0.turicum.memory.VariableLeftValue;

/**
 * LeftValue ::= Variable
 * | LeftValue '.' Identifier
 * | LeftValue '[' Expression ']'
 */
public class LeftValueAnalyser {
    public static final LeftValueAnalyser INSTANCE = new LeftValueAnalyser();

    public LeftValue analyze(LexList lexes) throws BadSyntax {
        BadSyntax.when(lexes, lexes.isEmpty(), "Left value can't be empty");
        final var lex = lexes.peek();
        if (lex.type()== Lex.Type.IDENTIFIER) {
            LeftValue left = new VariableLeftValue(lex.text());
            lexes.next();
            while (lexes.is(".") || lexes.is("[")) {
                switch (lexes.peek().text()) {
                    case ".":
                        lexes.next();
                        final var fieldName = lexes.next();
                        BadSyntax.when(lexes, fieldName.type()!= Lex.Type.IDENTIFIER, "Field name is invalid");
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
        throw new BadSyntax(lexes.position(), "Left value should start with identifier");

    }
}
