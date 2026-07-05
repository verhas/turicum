package ch.turic.lsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Formatter regression tests for the review findings: unmatched token pairs must preserve
 * the original gap instead of gluing the tokens together, and a line ending with {@code ':'}
 * indents the <em>next</em> line, not itself.
 */
class TuriFormatterTest {

    @Test
    void doesNotGlueTokensTogether() {
        final var formatted = TuriFormatter.formatDocument("let x = 5\n");
        assertTrue(formatted.contains("let x"),
                "the space after 'let' must survive formatting, got: " + formatted);
        assertFalse(formatted.contains("letx"), "tokens must not be glued: " + formatted);
    }

    @Test
    void colonIndentsTheNextLineNotItself() {
        final var formatted = TuriFormatter.formatDocument("if a :\nb()\nc()\n");
        final var lines = formatted.split("\n", -1);
        assertFalse(lines[0].startsWith(" "),
                "the ':'-ending line itself must not be indented: '" + lines[0] + "'");
        assertTrue(lines[1].startsWith("    "),
                "the line after the ':'-ending line must be indented: '" + lines[1] + "'");
        assertFalse(lines[2].startsWith(" "),
                "the indent must reset after the continuation line: '" + lines[2] + "'");
    }

    @Test
    void formattingIsIdempotentOnSimpleInput() {
        final var once = TuriFormatter.formatDocument("fn f(a, b) {\nlet x = a + b\nx\n}\n");
        final var twice = TuriFormatter.formatDocument(once);
        assertEquals(once, twice, "formatting must be idempotent");
    }
}
