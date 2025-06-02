package ch.turic.utils;

import ch.turic.BuiltIns;
import ch.turic.analyzer.Input;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.Program;
import ch.turic.memory.Context;
import org.junit.jupiter.api.Test;

public class TestMarshalling {

    @Test
    void test() {
        run("""
                async (println "hello")
                """);
    }

    Object run(String s) {
        final var analyzer = new ProgramAnalyzer();
        LexList lexes = Lexer.analyze((Input)ch.turic.Input.fromString(s));
        final var code = analyzer.analyze(lexes);
        if (!(code instanceof Program program)) {
            throw new RuntimeException("code is not a Program");
        }
        final var marshaller = new Marshaller();
        final var bytes = marshaller.serialize(program);
        final var unmarshaller = new Unmarshaller();
        final var descode = unmarshaller.deserialize(bytes);
        final var ctx = new Context();
        BuiltIns.register(ctx);
        return descode.execute(ctx);
    }
}
