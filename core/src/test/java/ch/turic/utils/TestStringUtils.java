package ch.turic.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
