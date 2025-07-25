package ch.turic.utils;

import ch.turic.ExecutionException;
import ch.turic.memory.LngList;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {


    /**
     * Determines whether the given text matches the specified pattern.
     * The pattern may include the wildcard characters '*' and '?':
     * - '*' matches zero or more characters.
     * - '?' matches exactly one character.
     * - '\' escapes special characters - use '\*' to match literal '*' and '\?' to match literal '?'.
     *
     * @param pattern the pattern string containing characters and optional wildcards '*' and '?'
     * @param text    the text string to be matched against the pattern
     * @return true if the text matches the pattern, false otherwise
     */
    public static boolean matches(String pattern, String text) {
        return matchHelper(pattern, 0, text, 0);
    }

    /**
     * A recursive helper method to determine if a given text matches the specified pattern.
     * The pattern may include the wildcard characters '*' and '?':
     * - '*' matches zero or more characters.
     * - '?' matches exactly one character.
     * - '\' escapes special characters - use '\*' to match literal '*' and '\?' to match literal '?'
     *
     * @param pattern the pattern string containing characters, and the optional wildcards '*' and '?'
     * @param pIdx    the current index in the pattern being processed
     * @param text    the text string to be matched against the pattern
     * @param tIdx    the current index in the text being processed
     * @return true if the text matches the pattern from the current indices onward, false otherwise
     */
    private static boolean matchHelper(String pattern, int pIdx, String text, int tIdx) {
        if (pIdx == pattern.length()) {
            return tIdx == text.length();
        }

        if (pattern.charAt(pIdx) == '*' && pattern.length() > pIdx + 1 && pattern.charAt(pIdx + 1) == '*') {
            // Try to match '**' with 0 or more characters
            for (int k = tIdx; k <= text.length(); k++) {
                if (matchHelper(pattern, pIdx + 2, text, k)) {
                    return true;
                }
            }
            return false;
        }

        if (pattern.charAt(pIdx) == '*') {
            // Try to match '*' with 0 or more characters excluding '/'
            for (int k = tIdx; k <= text.length(); k++) {
                if (matchHelper(pattern, pIdx + 1, text, k)) {
                    return true;
                }
                if (k < text.length() && text.charAt(k) == '/') {
                    return false;
                }
            }
            return false;
        }

        if (pattern.charAt(pIdx) == '?') {
            if (tIdx < text.length()) {
                return matchHelper(pattern, pIdx + 1, text, tIdx + 1);
            }
            return false;
        }
        if (pattern.charAt(pIdx) == '\\') {
            if (tIdx == text.length()) {
                // cannot have a \\ at the end, it will not match anything
                return false;
            }
            tIdx++; // step to the next character and try to match it even if it is '*' or '?' 
        }

        // Normal character must match
        if (tIdx < text.length() && pattern.charAt(pIdx) == text.charAt(tIdx)) {
            return matchHelper(pattern, pIdx + 1, text, tIdx + 1);
        }

        return false;
    }

    /**
     * Computes the hash digest of the input string using the specified algorithm.
     *
     * @param input the string to hash
     * @param type  the name of the hash algorithm (e.g., "MD5", "SHA-1", "SHA-256")
     * @return the hexadecimal string representation of the hash digest
     * @throws ExecutionException if the specified algorithm is not available
     */
    public static String digest(String input, String type) {
        try {
            MessageDigest md = MessageDigest.getInstance(type);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ExecutionException(e);
        }
    }

    public static String swapCase(String input) {
        if (input == null) return null;

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (Character.isUpperCase(cp)) {
                sb.appendCodePoint(Character.toLowerCase(cp));
            } else if (Character.isLowerCase(cp)) {
                sb.appendCodePoint(Character.toUpperCase(cp));
            } else {
                sb.appendCodePoint(cp);
            }

            i += charCount;
        }

        return sb.toString();
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder titleCase = new StringBuilder();
        boolean nextTitle = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                nextTitle = true;
                titleCase.append(c);
            } else if (nextTitle) {
                titleCase.append(Character.toTitleCase(c));
                nextTitle = false;
            } else {
                titleCase.append(Character.toLowerCase(c));
            }
        }

        return titleCase.toString();
    }

    /**
     * Splits the input string into three parts based on the first occurrence of the specified separator.
     * The returned array contains the substring before the separator, the separator itself, and the substring after the separator.
     * If the separator is not found in the input, the entire input string is returned as the first element, and the second and third elements are empty strings.
     *
     * @param input the string to be partitioned; must not be null
     * @param sep   the separator string used for partitioning; must not be null or empty
     * @return a string array of size three, where the first element is the substring before the separator,
     * the second element is the separator, and the third element is the substring after the separator.
     * @throws IllegalArgumentException if the input or separator is null, or if the separator is empty
     */
    public static String[] partition(String input, String sep) {
        if (input == null || sep == null || sep.isEmpty()) {
            throw new IllegalArgumentException("Input and separator must be non-null and separator non-empty.");
        }

        int index = input.indexOf(sep);
        if (index == -1) {
            return new String[]{input, "", ""};
        }

        String before = input.substring(0, index);
        String after = input.substring(index + sep.length());
        return new String[]{before, sep, after};
    }

    /**
     * Splits the input string into three parts based on the first match of the provided regular expression.
     * The returned array contains the substring before the match, the match itself, and the substring after the match.
     * If the regex does not find a match in the input, the entire input string is returned as the first element,
     * and the second and third elements are empty strings.
     *
     * @param input the string to be partitioned; must not be null
     * @param regex the regular expression used for partitioning; must not be null or empty
     * @return a string array of size three, where the first element is the substring before the match,
     * the second element is the matched portion, and the third element is the substring after the match
     * @throws IllegalArgumentException if the input or regex is null, or if the regex is empty
     */
    public static String[] partitionRegex(String input, String regex) throws ExecutionException {
        if (input == null || regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("Input and regex must be non-null and regex non-empty.");
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (end == 0) {
                throw new ExecutionException("Partition regexp matches zero string. '" + regex + "' It is forbidden to avoid infinite loops.");
            }
            return new String[]{
                    input.substring(0, start),
                    input.substring(start, end),
                    input.substring(end)
            };
        } else {
            return new String[]{input, "", ""};
        }
    }


    /**
     * Splits the input string recursively based on the sequence of delimiter characters provided.
     * Each character in the delimiters string represents a level of split, and splits can be controlled
     * by specifying a maximum number of splits for each level.
     *
     * @param input the string to be split; must not be null
     * @param pos the position in the delimiters string indicating the current level of split
     * @param delimiters the string containing delimiter characters for splitting; must not be null or empty
     * @param maxSplits optional arguments specifying the maximum number of splits for each level; if not provided or insufficiently defined, infinite splits are allowed
     * @return a list where each element corresponds to the result of the current level of split, and nested lists represent deeper levels of splits
     * @throws IllegalArgumentException if the input or delimiters are null
     */
    public static LngList msplit(String input, int pos, String delimiters, int... maxSplits) {
        if (input == null || delimiters == null) {
            throw new IllegalArgumentException("Input and delimiters must not be null");
        }
        final int maxSplit;
        if (maxSplits == null || maxSplits.length <= pos) {
            maxSplit = -1;
        } else {
            maxSplit = maxSplits[pos];
        }

        if (delimiters.isEmpty()) {
            return LngList.of(input);
        }

        // Quote the first character for regex to handle special characters
        String regex = Pattern.quote(String.valueOf(delimiters.charAt(pos)));
        String[] firstSplit = input.split(regex,maxSplit);

        if (delimiters.length() == pos + 1) {
            return LngList.ofStrings(firstSplit);
        }

        // Recursive split for remaining characters
        final var result = new LngList();
        for (String part : firstSplit) {
            result.add(msplit(part, pos + 1, delimiters, maxSplits));
        }

        return result;
    }
}
