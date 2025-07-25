package ch.turic.utils;

import ch.turic.memory.LngList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestStringUtils {

    @Test
    public void testMatchGLob() {

        Assertions.assertTrue(StringUtils.matches("*c", "abc"), "pattern '*c' should match 'abc'");
        Assertions.assertFalse(StringUtils.matches("*c", "a/c"), "pattern '*c' should not match 'a/c'");
        Assertions.assertTrue(StringUtils.matches("**c", "a/c"), "pattern '**c' should match 'a/c'");
        Assertions.assertTrue(StringUtils.matches("**c", "abc"), "pattern '**c' should match 'abc'");
        Assertions.assertTrue(StringUtils.matches("**", "abc"), "pattern '**' should match 'abc'");
        Assertions.assertTrue(StringUtils.matches("**/*/k", "fu/bar/c/k"), "pattern '**/*/k' should match 'fu/bar/c/k'");
        Assertions.assertFalse(StringUtils.matches("*/*/k", "fu/bar/c/k"), "pattern '*/*/k' should not match 'fu/bar/c/k'");
    }

    @Test
    public void testMsplit() {
        // Test basic splitting with a single delimiter
        LngList result1 = StringUtils.msplit("a,b,c", 0, ",");
        assertEquals(LngList.of("a", "b", "c"), result1);

        // Test nested splitting with two delimiters
        LngList result2 = StringUtils.msplit("a,b;c,d;e,f", 0, ";,");
        assertEquals(
            LngList.of(
                LngList.of("a", "b"),
                LngList.of("c", "d"),
                LngList.of("e", "f")
            ),
            result2
        );

        // Test three-level nested splitting
        LngList result3 = StringUtils.msplit("a,b#c,d#e,f|g,h#i,j", 0, "|#,");

        assertEquals(
            LngList.of(
                LngList.of(
                    LngList.of("a", "b"),
                    LngList.of("c", "d"),
                    LngList.of("e", "f")
                ),
                LngList.of(
                    LngList.of("g", "h"),
                    LngList.of("i", "j")
                )
            ),
            result3
        );

        // Test with empty delimiters
        LngList result4 = StringUtils.msplit("test", 0, "");
        assertEquals(LngList.of("test"), result4);

        // Test with single character input
        LngList result5 = StringUtils.msplit("a", 0, ",");
        assertEquals(LngList.of("a"), result5);

        // Test with special regex characters as delimiters
        LngList result6 = StringUtils.msplit("a.b.c|d.e.f", 0, "|.");
        assertEquals(
            LngList.of(
                LngList.of("a", "b", "c"),
                LngList.of("d", "e", "f")
            ),
            result6
        );

        // Test with empty parts
        LngList result7 = StringUtils.msplit("a,,b,", 0, ",",-1);
        assertEquals(LngList.of("a", "", "b", ""), result7);

        // Test null input handling
        assertThrows(IllegalArgumentException.class, () -> StringUtils.msplit(null, 0, ","));
        assertThrows(IllegalArgumentException.class, () -> StringUtils.msplit("test", 0, null));
    }
}