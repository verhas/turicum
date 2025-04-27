package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Analyze a series of options for the async command. These are `id=expression` pairs separated by commans and
 * enclosed between [ and ]. For example:
 *
 * <pre>
 *     {@code
 *     async[in=10,out=100,limit=0.01] {for i=1 ; i < 10 ; i=i+1 : print i }
 *     }
 * </pre>
 * <p>
 * Here `in`, `out` and `limit` are parameters.
 */
public class OptionListAnalyzer {
    public static Map<String, Command> analyze(LexList lexes, Set<String> acceptedParameters) throws BadSyntax {
        final var map = new HashMap<String, Command>();
        if( lexes.is("]")){
            return Map.of();
        }
        if (acceptedParameters.size() == 1 && !lexes.isIdentifier()) {
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            for (final var key : acceptedParameters) {
                map.put(key, expression);
            }
            return map;
        }

        while (lexes.isIdentifier() || lexes.isKeyword()) {// we allow words like 'in' without ` quoted to be used as options
            final var id = lexes.next().text();
            if (acceptedParameters.contains(id)) {
                BadSyntax.when(lexes, map.containsKey(id), "The key '%s' is double defined", id);
                BadSyntax.when(lexes, lexes.isNot("="), "The '=' is missing after an async option");
                lexes.next();
                final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                map.put(id, expression);
            } else {
                throw new BadSyntax(lexes.position(), "'%s' is not an accepted parameter", id);
            }
        }
        return map;
    }
}
