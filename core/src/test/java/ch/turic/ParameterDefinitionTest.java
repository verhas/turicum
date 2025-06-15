package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ParameterDefinition;
import ch.turic.commands.ParameterList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParameterDefinitionTest {

    private ParameterList parseParams(String code) {
        final var lexes = Lexer.analyze((Input)ch.turic.Input.fromString(code));
        return ParameterDefinition.INSTANCE.analyze(lexes);
    }

    @Test
    void positionalAndOptional() {
        var list = parseParams("a, b=2");
        assertEquals(2, list.parameters().length);
        assertEquals("a", list.parameters()[0].identifier().pureName());
        assertNull(list.parameters()[0].defaultExpression());
        assertEquals("b", list.parameters()[1].identifier().pureName());
        assertNotNull(list.parameters()[1].defaultExpression());
    }

    @Test
    void positionalOnly() {
        var list = parseParams("!a, b");
        assertEquals(ParameterList.Parameter.Type.POSITIONAL_ONLY, list.parameters()[0].type());
    }

    @Test
    void namedOnly() {
        var list = parseParams("@a=1");
        assertEquals(ParameterList.Parameter.Type.NAMED_ONLY, list.parameters()[0].type());
        assertNotNull(list.parameters()[0].defaultExpression());
    }

    @Test
    void restMetaClosureParameters() {
        var list = parseParams("a, [r], {m}, ^block");
        assertEquals("r", list.rest().pureName());
        assertEquals("m", list.meta().pureName());
        assertEquals("block", list.closure().pureName());
    }

    @Test
    void restMustComeBeforeMetaAndClosure() {
        assertThrows(BadSyntax.class, () -> parseParams("{m}, [r], ^c"));
    }

    @Test
    void cannotRepeatParameterNames() {
        assertThrows(BadSyntax.class, () -> parseParams("a, a"));
        assertThrows(BadSyntax.class, () -> parseParams("a, [a]"));
        assertThrows(BadSyntax.class, () -> parseParams("a, {a}"));
        assertThrows(BadSyntax.class, () -> parseParams("a, ^a"));
    }

    @Test
    void cannotUsePrefixWithRestMetaClosure() {
        assertThrows(BadSyntax.class, () -> parseParams("@[r]"));
        assertThrows(BadSyntax.class, () -> parseParams("!{m}"));
        assertThrows(BadSyntax.class, () -> parseParams("!^c"));
    }

    @Test
    void correctTrailingCommaBehavior() {
        var list = parseParams("a, b=1, [r], {m}, ^c");
        assertEquals(2, list.parameters().length);
        assertEquals("a", list.parameters()[0].identifier().pureName());
        assertEquals("b", list.parameters()[1].identifier().pureName());
        assertEquals("r", list.rest().pureName());
        assertEquals("m", list.meta().pureName());
        assertEquals("c", list.closure().pureName());
    }
}
