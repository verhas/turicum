package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.JsonConstant;

import java.util.HashMap;

public class JsonStructureAnalyzer implements Analyzer {
    public static final JsonStructureAnalyzer INSTANCE = new JsonStructureAnalyzer();

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        final var lazy = lexes.is("&{");
        lexes.next(); // step over the '{'
        final var fields = new HashMap<String, Command>();
        while (lexes.isIdentifier() || lexes.peek().type() == Lex.Type.STRING) {
            final var key = lexes.next().text();
            ExecutionException.when(lexes.isNot(":"),": is missing after key in JSON object");
            lexes.next();
            final var value = CommandAnalyzer.INSTANCE.analyze(lexes);
            fields.put(key, value);
            if (lexes.is(",")) {
                lexes.next();
            }
        }
        ExecutionException.when( lexes.isNot("}"),"JSON Structure has to be closed with }");
        lexes.next();
        return new JsonConstant(fields,lazy);
    }

}
