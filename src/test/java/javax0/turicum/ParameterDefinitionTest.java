package javax0.turicum;

import javax0.turicum.analyzer.Input;
import javax0.turicum.analyzer.Lexer;
import javax0.turicum.analyzer.ParameterDefinition;
import javax0.turicum.commands.ParameterList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParameterDefinitionTest {

    private ParameterList parseParams(String code) throws Exception {
        final var lexes = new Lexer().analyze(Input.fromString(code));
        return ParameterDefinition.INSTANCE.analyze(lexes);
    }

    @Test
    void positionalAndOptional() throws Exception {
        var list = parseParams("a, b=2");
        assertEquals(2, list.parameters().length);
        assertEquals("a", list.parameters()[0].identifier());
        assertNull(list.parameters()[0].defaultExpression());
        assertEquals("b", list.parameters()[1].identifier());
        assertNotNull(list.parameters()[1].defaultExpression());
    }

    @Test
    void positionalOnly() throws Exception {
        var list = parseParams("!a, b");
        assertEquals(ParameterList.Parameter.Type.POSITIONAL_ONLY, list.parameters()[0].type());
    }

    @Test
    void namedOnly() throws Exception {
        var list = parseParams("@a=1");
        assertEquals(ParameterList.Parameter.Type.NAMED_ONLY, list.parameters()[0].type());
        assertNotNull(list.parameters()[0].defaultExpression());
    }

    @Test
    void restMetaClosureParameters() throws Exception {
        var list = parseParams("a, [r], {m}, |block|");
        assertEquals("r", list.rest());
        assertEquals("m", list.meta());
        assertEquals("block", list.closure());
    }

    @Test
    void restMustComeBeforeMetaAndClosure() {
        assertThrows(BadSyntax.class, () -> parseParams("{m}, [r], |c|"));
    }

    @Test
    void cannotRepeatParameterNames() {
        assertThrows(BadSyntax.class, () -> parseParams("a, a"));
        assertThrows(BadSyntax.class, () -> parseParams("a, [a]"));
        assertThrows(BadSyntax.class, () -> parseParams("a, {a}"));
        assertThrows(BadSyntax.class, () -> parseParams("a, |a|"));
    }

    @Test
    void cannotUsePrefixWithRestMetaClosure() {
        assertThrows(BadSyntax.class, () -> parseParams("@[r]"));
        assertThrows(BadSyntax.class, () -> parseParams("!{m}"));
        assertThrows(BadSyntax.class, () -> parseParams("!|c|"));
    }

    @Test
    void correctTrailingCommaBehavior() throws Exception {
        var list = parseParams("a, b=1, [r], {m}, |c|");
        assertEquals(2, list.parameters().length);
        assertEquals("a", list.parameters()[0].identifier());
        assertEquals("b", list.parameters()[1].identifier());
        assertEquals("r", list.rest());
        assertEquals("m", list.meta());
        assertEquals("c", list.closure());
    }
}
