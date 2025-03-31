package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StringFetcherTest {

    @Nested
    @DisplayName("Simple String Tests")
    class SimpleStringTests {

        @Test
        @DisplayName("Empty string returns empty string")
        void emptyString() throws BadSyntax {
            Input input = Input.fromString("\"\"");
            assertEquals("", StringFetcher.getString(input));
            assertTrue(input.isEmpty());
        }

        @Test
        @DisplayName("Simple string without escapes")
        void simpleString() throws BadSyntax {
            Input input = Input.fromString("\"Hello, World!\"");
            assertEquals("Hello, World!", StringFetcher.getString(input));
            assertTrue(input.isEmpty());
        }

        @Test
        @DisplayName("String with remaining input")
        void stringWithRemainingInput() throws BadSyntax {
            Input input = Input.fromString("\"Hello\" remaining");
            assertEquals("Hello", StringFetcher.getString(input));
            assertEquals(" remaining", input.toString());
        }

        @Test
        @DisplayName("Unterminated string throws BadSyntax")
        void unterminatedString() {
            Input input = Input.fromString("\"Hello");
            assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
        }

        @Test
        @DisplayName("String with newline in the middle throws BadSyntax")
        void stringWithNewline() {
            Input input = Input.fromString("\"Hello\nWorld\"");
            BadSyntax ex = assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
            assertTrue(ex.getMessage().startsWith("String not terminated before eol"));
        }

        @Test
        @DisplayName("String with carriage return in the middle throws BadSyntax")
        void stringWithCarriageReturn() {
            Input input = Input.fromString("\"Hello\rWorld\"");
            BadSyntax ex = assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
            assertTrue(ex.getMessage().startsWith("String not terminated before eol"));
        }

    }

    @Nested
    @DisplayName("Escape Sequence Tests")
    class EscapeSequenceTests {

        @ParameterizedTest(name = "Escape sequence \\{0} becomes {1}")
        @MethodSource("escapeSequences")
        void testEscapeSequences(String input, String expected) throws BadSyntax {
            Input in = Input.fromString("\"" + input + "\"");
            assertEquals(expected, StringFetcher.getString(in));
        }

        static Stream<Arguments> escapeSequences() {
            return Stream.of(
                Arguments.of("\\b", "\b"),
                Arguments.of("\\t", "\t"),
                Arguments.of("\\n", "\n"),
                Arguments.of("\\f", "\f"),
                Arguments.of("\\r", "\r"),
                Arguments.of("\\\"", "\""),
                Arguments.of("\\'", "'"),
                Arguments.of("\\\\", "\\"),
                // Octal escapes starting with 0-3 (can be 3 digits)
                Arguments.of("\\0", "\0"),
                Arguments.of("\\07", "\07"),
                Arguments.of("\\377", "\377"),
                // Octal escapes starting with 4-7 (can only be 2 digits)
                Arguments.of("\\47", "\47"),   // '7' character
                Arguments.of("\\52", "\52"),   // '*' character
                Arguments.of("\\65", "\65"),   // '5' character
                Arguments.of("\\77", "\77")    // '?' character
            );
        }

        @Test
        @DisplayName("Invalid escape sequence throws BadSyntax")
        void invalidEscapeSequence() {
            Input input = Input.fromString("\"\\x\"");
            assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Escape at end of string throws BadSyntax")
        void escapeAtEnd() {
            Input input = Input.fromString("\"\\");
            assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Three digit octal escape starting with 4-7 should only use two digits")
        void threeDigitOctalStartingWith4() throws BadSyntax {
            Input input = Input.fromString("\"\\477\"");
            // Should read only "\\47" as octal, leaving "7" as a regular character
            assertEquals("\477", StringFetcher.getString(input));
        }

    }

    @Nested
    @DisplayName("Multi-line String Tests")
    class MultilineStringTests {

        @Test
        @DisplayName("Empty multi-line string")
        void emptyMultilineString() throws BadSyntax {
            Input input = Input.fromString("\"\"\"\"\"\"");
            assertEquals("", StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Multi-line string with actual line breaks")
        void multilineStringWithBreaks() throws BadSyntax {
            Input input = Input.fromString("\"\"\"\nFirst Line\nSecond Line\n\"\"\"");
            assertEquals("\nFirst Line\nSecond Line\n", StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Multi-line string with mixed line endings")
        void mixedLineEndings() throws BadSyntax {
            Input input = Input.fromString("\"\"\"\nFirst\rSecond\r\nThird\"\"\"");
            assertEquals("\nFirst\nSecond\nThird", StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Unterminated multi-line string throws BadSyntax")
        void unterminatedMultilineString() {
            Input input = Input.fromString("\"\"\"Content");
            assertThrows(BadSyntax.class, () -> StringFetcher.getString(input));
        }

    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("String with all types of escapes")
        void complexString() throws BadSyntax {
            Input input = Input.fromString("\"Hello\\n\\t\\\"World\\\"\\077\"");
            assertEquals("Hello\n\t\"World\"?", StringFetcher.getString(input));
        }

        @Test
        @DisplayName("Multi-line string with escapes")
        void multilineWithEscapes() throws BadSyntax {
            Input input = Input.fromString("\"\"\"\n\\t First\n\\\"Second\\\"\\n\"\"\"");
            assertEquals("\n\t First\n\"Second\"\n", StringFetcher.getString(input));
        }

    }

}
