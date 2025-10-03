package ch.turic.commands;

import ch.turic.Command;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

/**
 * A type declaration.
 * <p>
 * A type declaration is either an identifier, like {@code str}, {@code lst} or an expression between '{@code (}' and
 * '{@code )}' characters.
 *
 * @param identifier the identifier string
 * @param expression the expression
 */
public record TypeDeclaration(String identifier, Command expression) {

    public static TypeDeclaration factory(final Unmarshaller.Args args) {
        return new TypeDeclaration(
                args.str("identifier"),
                args.command("expression")
        );
    }

    /**
     * Calculate the type name. If a name gives the type, it is just the name given; otherwise, call the expression.
     * evaluate it and return the result as a string.
     *
     * @param context the context used to execute the expression in
     * @return the name of the type
     */
    public String calculateTypeName(LocalContext context) {
        if (expression == null) {
            return identifier;
        } else {
            final var tValue = expression.execute(context);
            return tValue == null ? "none" : tValue.toString();
        }
    }

    @Override
    public String toString() {
        return expression == null ? identifier : "(" + expression + ")";
    }
}
