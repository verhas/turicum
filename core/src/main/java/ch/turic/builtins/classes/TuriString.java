package ch.turic.builtins.classes;

import ch.turic.*;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Conditional;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;
import ch.turic.utils.CaseFolder;
import ch.turic.utils.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TuriString implements TuriClass {
    @Override
    public Class<?> forClass() {
        return String.class;
    }

    /**
     * Returns a callable object that provides string manipulation methods for the given string target.
     * <p>
     * The returned callable corresponds to the method identified by the provided identifier, enabling operations such as substring extraction, padding, encoding, hashing, splitting, joining, and various string property checks. Throws an ExecutionException if the target is not a String or if invalid arguments are provided for the selected method.
     *
     * @param target     the string instance on which the method will operate
     * @param identifier the name of the string method to retrieve
     * @return a callable object implementing the requested string operation, or null if the identifier is unrecognized
     * @throws ExecutionException if the target is not a String or if invalid arguments are supplied to the method
     */
    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if (!(target instanceof String string)) {
            throw new ExecutionException("Target object is not a String, this is an internal error");
        }

        return switch (identifier) {
            // snippet string_functions_doc
            case "to_string" ->
                // returns the string itself. This method exists so that you can use it like on any other objects.
                    new TuriMethod<>((args) -> String.format("%s", string));
            case "length" ->
                // a convenience method that makes it possible to use it like in Java.
                // The Turicum way is to use `len()` built-in function, but this method also works on strings.
                // {%S string_length%}
                    new TuriMethod<>((args) -> (long) string.length());
            case "after" ->
                // returns the part of the string that follows the argument string.
                // If the argument string is not in the string, then it returns an empty string.
                // {%S string_after%}
                    new TuriMethod<>((args) -> {
                        final var afterWhat = "" + args[0];
                        final var pos = string.indexOf(afterWhat);
                        if (pos < 0) {
                            return "";
                        }
                        return string.substring(pos + afterWhat.length());
                    });
            case "before" ->
                // returns the part of the string that is before the argument string.
                // If the argument string is not in the string, then it returns an empty string.
                // {%S string_before%}
                    new TuriMethod<>((args) -> {
                        final var beforeWhat = "" + args[0];
                        final var pos = string.indexOf(beforeWhat);
                        if (pos < 0) {
                            return "";
                        }
                        return string.substring(0, pos);
                    });
            case "pad_left" ->
                // add characters on the left of the string to get the desired length.
                // You can specify only the number of the characters to pad with spaces.
                // Alternatively, you can also specify a string and the desired total length of the result.
                // The string is supposed to be a single character, but it can be any length.
                // If the necessary number of padding characters is not the multiple of string length, the resulting string may be shorter than desired.
                // {%S string_pad_left%}
                    new TuriMethod<>((args) -> {
                        final var arguments = FunUtils.args("pad_left", args, Object[].class);
                        final var pad = arguments.at(1).getOr(" ");
                        ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                        final var n = arguments.at(0).intValue();
                        if (n <= string.length()) {
                            return string;
                        }
                        final var k = (n - string.length()) / pad.length();
                        return pad.repeat(k) + string;
                    });
            case "pad_right" ->
                // add character to the right of the string to get the desired length.
                // This method works the same way as `pad_left`, but it appends the characters.
                // {%S string_pad_right%}
                    new TuriMethod<>((args) -> {
                        final var arguments = FunUtils.args("pad_right", args, Object[].class);
                        final var pad = arguments.at(1).getOr(" ");
                        ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                        final var n = arguments.at(0).intValue();
                        if (n <= string.length()) {
                            return string;
                        }
                        final var k = (n - string.length()) / pad.length();
                        return string + pad.repeat(k);
                    });

            case "between" ->
                // returns the part of the string that is between two specific strings.
                // if one of the parameter strings is not found in the string, the result is an empty string.
                // {%S string_between%}
                    new TuriMethod<>((args) -> {
                        final var afterWhat = "" + args[0];
                        final var beforeWhat = "" + args[1];
                        final var endIndex = string.indexOf(beforeWhat);
                        final var pos2 = string.indexOf(afterWhat);
                        if (endIndex < 0 || pos2 < 0) {
                            return "";
                        }
                        int beginIndex = pos2 + afterWhat.length();
                        if (endIndex <= beginIndex) {
                            return "";
                        }
                        return string.substring(beginIndex, endIndex);
                    });
            case "lines" ->
                // returns a list containing the lines of the string.
                // {%S string_lines%}
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        list.addAll(Arrays.asList(string.split("\n", -1)));
                        return list;
                    });
            case "words" ->
                // returns a list containing the words of the string.
                // {%S string_words%}
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        list.addAll(Arrays.asList(string.split("\\W+", -1)));
                        return list;
                    });
            case "turi_lex" ->
                // returns a list containing the tokens of the string matching the tokenization of the language (Turicum).
                // {%S string_turi_lex%}
                    new TuriMethod<>((args) -> Lexer.analyze((ch.turic.analyzer.Input) Input.fromString(string)));
            case "execute" -> (LngCallable.LngCallableClosure)// do not remove, it is NOT redundant! I do not know why.
                    // executes the string as Turicum code.
                    // {%S string_execute%}
                    (context, args) -> {
                        final var ctx = FunUtils.ctx(context);
                        final var analyzer = new ProgramAnalyzer();
                        Command code = analyzer.analyze(Lexer.analyze((ch.turic.analyzer.Input) Input.fromString(string)));
                        final var result = code.execute(ctx);
                        if (result instanceof Conditional.Result res) {
                            return res.result();
                        } else {
                            return result;
                        }
                    };
            case "url_encode" ->
                // encode the string for URL
                // {%S string_url_encode%}
                    new TuriMethod<>((args) -> URLEncoder.encode(string, StandardCharsets.UTF_8));
            case "url_decode"
                // decode the string from URL-encoded form
                // {%S string_url_decode%}
                    -> new TuriMethod<>((args) -> URLDecoder.decode(string, StandardCharsets.UTF_8));
            case "md5" ->
                // calculate the md5 hash of the string
                // {%S string_md5%}
                    new TuriMethod<>((args) -> StringUtils.digest(string, "MD5"));
            case "sha_1" ->
                // calculate the sha-1 hash of the string
                // {%S string_sha_1%}
                    new TuriMethod<>((args) -> StringUtils.digest(string, "SHA-1"));
            case "sha_256" ->
                // calculate the sha-256 hash of the string
                // {%S string_sha_256%}
                    new TuriMethod<>((args) -> StringUtils.digest(string, "SHA-256"));
            case "sha_512" ->
                // calculate the sha-512 hash of the string
                // {%S string_sha_512%}
                    new TuriMethod<>((args) -> StringUtils.digest(string, "SHA-512"));
            case "digest" ->
                // calculate the hash of the string. The algorithm name has to be provided as an argument.
                // {%S string_digest%}
                    new TuriMethod<>((args) -> StringUtils.digest(string, "" + args[0]));
            case "base64" ->
                // base64 encode the string
                // {%S string_base64%}
                    new TuriMethod<>((args) -> {
                        Base64.Encoder encoder = Base64.getEncoder();
                        return encoder.encodeToString(string.getBytes(StandardCharsets.UTF_8));
                    });
            case "from_base64" ->
                // create a list of numbers (bytes) from a base64 encoded string
                // {%S string_from_base64%}
                    new TuriMethod<>((args) -> {
                        Base64.Decoder decoder = Base64.getDecoder();
                        final var bytes = decoder.decode(string);
                        final var list = new LngList();
                        for (byte aByte : bytes) {
                            list.array.add(aByte);
                        }
                        return list;
                    });
            case "from_base64_str" ->
                // create a string from a base64 encoded string
                // {%S string_from_base64_str%}
                    new TuriMethod<>((args) -> {
                        Base64.Decoder decoder = Base64.getDecoder();
                        return new String(decoder.decode(string), StandardCharsets.UTF_8);
                    });
            case "contains" ->
                // returns `true` if the string contains the argument.
                // {%S string_contains%}
                    new TuriMethod<>((args) -> {
                        final var arg = FunUtils.arg("contains", args, String.class);
                        return string.contains(arg);
                    });
            case "contains_regex" ->
                // returns `true` if the string contains some substring that matches the argument as a regular expression.
                // {%S string_contains_regex%}
                    new TuriMethod<>((args) -> {
                        final var arg = FunUtils.arg("contains_regex", args, String.class);
                        final var pattern = Pattern.compile(arg);
                        return pattern.matcher(string).find();
                    });
            case "matches" ->
                // returns `true` if the string matches the argument as a regular expression.
                // {%S string_matches%}
                    new TuriMethod<>((args) -> {
                        final var arg = FunUtils.arg("matches", args, String.class);
                        final var pattern = Pattern.compile(arg);
                        return pattern.matcher(string).matches();
                    });
            case "matches_glob" ->
                // returns `true` if the string matches the argument as a glob string (`pass:[*]` matching any substring except `/`,
                // `**` macthes any substring, and `?` matching any single character).
                // {%S string_matches_glob%}
                    new TuriMethod<>((args) -> {
                        final var arg = FunUtils.arg("matches_glob", args, String.class);
                        return StringUtils.matches(arg, string);
                    });
            case "is_blank" ->
                // returns `true` if the string is blank
                // {%S string_is_blank%}
                    new TuriMethod<>((args) -> string.isBlank());
            case "is_not_blank" ->
                // returns `false` if the string is blank
                // {%S string_is_not_blank%}
                    new TuriMethod<>((args) -> !string.isBlank());
            case "is_empty" ->
                // returns `true` if the string is empty
                // {%S string_is_empty%}
                    new TuriMethod<>((args) -> string.isEmpty());
            case "is_not_empty" ->
                // returns `false` if the string is empty
                // {%S string_is_not_empty%}
                    new TuriMethod<>((args) -> !string.isEmpty());
            case "is_numeric" ->
                // returns `true` if the string holds a decimal numeric value, either integer or floating point formatted.
                // {%S string_is_numeric%}
                    new TuriMethod<>((args) -> string.matches("^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$"));
            case "is_not_numeric" ->
                // returns `false` if the string holds a decimal numeric value, either integer or floating point formatted.
                // {%S string_is_not_numeric%}
                    new TuriMethod<>((args) -> !string.matches("^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$"));
            case "is_digit" ->
                // returns `true` if the string contains only digits
                // {%S string_is_digit%}
                    new TuriMethod<>((args) -> string.matches("^\\d+$"));
            case "is_not_digit" ->
                // returns `false` if the string contains only digits
                // {%S string_is_not_digit%}
                    new TuriMethod<>((args) -> !string.matches("^\\d+$"));
            case "is_alpha" ->
                // returns `true` if the string contains only alpha characters
                // {%S string_is_alpha%}
                    new TuriMethod<>((args) -> string.matches("^[a-zA-Z]+$"));
            case "is_not_alpha" ->
                // returns `false` if the string contains only alpha characters
                // {%S string_is_not_alpha%}
                    new TuriMethod<>((args) -> !string.matches("^[a-zA-Z]+$"));
            case "is_alphanumeric" ->
                // returns `true` if the string contains only alphanumeric characters
                // {%S string_is_alphanumeric%}
                    new TuriMethod<>((args) -> string.matches("^[a-zA-Z0-9]+$"));
            case "is_not_alphanumeric" ->
                // returns `false` if the string contains only alphanumeric characters
                // {%S string_is_not_alphanumeric%}
                    new TuriMethod<>((args) -> !string.matches("^[a-zA-Z0-9]+$"));
            case "is_hex" ->
                // returns `true` if the string contains only hexadecimal characters
                // {%S string_is_hex%}
                    new TuriMethod<>((args) -> string.matches("^(0x)?[a-fA-F0-9]+$"));
            case "is_not_hex" ->
                // returns `false` if the string contains only hexadecimal characters
                // {%S string_is_not_hex%}
                    new TuriMethod<>((args) -> !string.matches("^(0x)?[a-fA-F0-9]+$"));
            case "int" ->
                // returns the integer value of the string
                // {%S string_int%}
                    new TuriMethod<>((args) -> Long.parseLong(string.strip()));
            case "float" ->
                // returns the floating number contained in the string
                // {%S string_float%}
                    new TuriMethod<>((args) -> Double.parseDouble(string.strip()));
            case "number" ->
                // returns the number contained in the string encoded with the argument radix
                // {%S string_number%}
                    new TuriMethod<>((args) -> {
                        final var number = FunUtils.arg("number", args);
                        final var radix = Cast.toLong(number).intValue();
                        return Long.parseLong(string.strip(), radix);
                    });
            case "hex" ->
                // returns the number contained in the string as a hexadecimal number.
                // It is not the same as calling `"0AF6".number(16)` because this method respects the leading `0x` if there is any.
                // {%S string_hex%}
                    new TuriMethod<>((args) -> {
                        final var hexString = string.strip();
                        if (hexString.startsWith("0x") || hexString.startsWith("0X")) {
                            return Long.parseLong(hexString.substring(2), 16);
                        }
                        return Long.parseLong(hexString, 16);
                    });
            case "substring" ->
                // retruns a substring of the string.
                // There has to be at least one (`a`), and there can be at most two (`a`,`b`) integer arguments.
                // The substring starts at the character indexed `a` and finishes before the character `b`.
                // If there is no `b`, the substring lasts till the end of the string.
                // The implementation honors overindexing and returns characters only to the end of the string or an empty string.
                // {%S string_substring%}
                    new TuriMethod<>((args) -> {
                        final var arguments = FunUtils.args("substring", args, Long.class, Object[].class);
                        int beginIndex = arguments.at(0).intValue();
                        if (beginIndex < 0) {
                            int n = -beginIndex / string.length();
                            beginIndex = beginIndex + (n + 1) * string.length();
                        }
                        if (beginIndex >= string.length()) {
                            return "";
                        }
                        if (arguments.N == 1) {
                            return string.substring(beginIndex);
                        }
                        if (arguments.N == 2) {
                            int endIndex = arguments.at(1).intValue();
                            if (endIndex < 0) {
                                int n = -endIndex / string.length();
                                endIndex = endIndex + (n + 1) * string.length();
                            }
                            if (endIndex > string.length()) {
                                return string.substring(beginIndex);
                            }
                            return string.substring(beginIndex, endIndex);
                        }
                        throw new ExecutionException("substring() needs one or two arguments");
                    });
            case "remove_prefix" ->
                // removes the argument from the start of the string if that is a prefix of the string.
                // {%S string_remove_prefix%}
                    new TuriMethod<>((args) -> {
                        final var prefix = "" + args[0];
                        if (string.startsWith(prefix)) {
                            return string.substring(prefix.length());
                        } else {
                            return string;
                        }
                    });
            case "remove_postfix" ->
                // removes the argument from the end of the string if that is a postfix of the string.
                // {%S string_remove_postfix%}
                    new TuriMethod<>((args) -> {
                        final var postfix = "" + args[0];
                        if (string.endsWith(postfix)) {
                            return string.substring(0, string.length() - postfix.length());
                        } else {
                            return string;
                        }
                    });
            case "count_substring" ->
                // counts the occurrences of the argument string.
                // Note that when an occurrence is found the other occurrences are sought for after that occurrence.
                // It means that, for example, `("a"*6).count_substring("aa")` is 3 and not 5.
                // {%S string_count_substring%}
                    new TuriMethod<>((args) -> {
                        final String substr = "" + args[0];
                        int i = 0;
                        long count = 0;
                        while ((i = string.indexOf(substr, i)) >= 0) {
                            i += substr.length();
                            count++;
                        }
                        return count;
                    });
            case "count_substring_overlap" ->
                // counts all the occurrences of the argument string, even if they overlap
                // It means that, for example, `("a"*6).count_substring("aa")` is 5.
                // {%S string_count_substring_overlap%}
                    new TuriMethod<>((args) -> {
                        final String substr = "" + args[0];
                        int i = 0;
                        long count = 0;
                        while ((i = string.indexOf(substr, i)) >= 0) {
                            i++;
                            count++;
                        }
                        return count;
                    });
            case "left" ->
                // return the left of the string.
                // This method uses the argument as a number and returns the string that contains at most that number of characters.
                // Essentially, the `string.left(n)` is the `n` leftmost characters if `n` is smaller than the number of characters in the string.
                // If `n` is equal to, or larger than the number of characters in the string, then the whole string is the result.
                // {%S string_left%}
                    new TuriMethod<>((args) -> {
                        final var endIndex = Cast.toLong(args[0]).intValue();
                        if (endIndex > string.length()) {
                            return string;
                        }
                        return string.substring(0, endIndex);
                    });
            case "right" ->
                // return the right of the string.
                // This method uses the argument as a number and returns the string that contains at most that number of characters.
                // Essentially, the `string.right(n)` is the `n` rightmost characters if `n` is smaller than the number of characters in the string.
                // If `n` is equal to, or larger than the number of characters in the string, then the whole string is the result.
                // {%S string_right%}
                    new TuriMethod<>((args) -> {
                        int n = Cast.toLong(args[0]).intValue();
                        if (n >= string.length()) {
                            return string;
                        }
                        return string.substring(string.length() - n);
                    });
            case "replace_all" ->
                // replaces the occurrences of the first argument interpreted as regular expression with the second argument interpreted as string.
                // {%S string_replace_all%}
                    new TuriMethod<>((args) -> string.replaceAll("" + args[0], "" + args[1]));
            case "quote" ->
                // quotes the special characters in the string.
                // The result is a string that can be used in a string literal to represent the original string.
                // {%S string_quote%}
                    new TuriMethod<>((args) -> string.replace("\\", "\\\\")
                            .replace("\t", "\\t")
                            .replace("\b", "\\b")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\f", "\\f")
                            .replace("\"", "\\\""));
            case "reverse" ->
                // returns the string that contains the same characters as the original string, but in reverse order.
                // {%S string_reverse%}
                    new TuriMethod<>((args) -> new StringBuilder(string).reverse().toString());
            case "chop" ->
                // returns string without the last character
                // {%S string_chop%}
                    new TuriMethod<>((args) -> {
                        if (string.isEmpty()) {
                            return "";
                        }
                        return string.substring(0, string.length() - 1);
                    });
            case "chomp" ->
                // returns the string without the last new-line character.
                // If the last character is not a new line character, the original string is returned.
                // {%S string_chomp%}
                    new TuriMethod<>((args) -> string.endsWith("\n") ? string.substring(0, string.length() - 1) : string);
            case "times" ->
                // repeat the string as specified by the argument.
                // {%S string_times%}
                    new TuriMethod<>((args) -> string.repeat(Cast.toLong(args[0]).intValue()));
            case "lower_case" ->
                // return the string lower-cased character.
                // {%S string_lower_case%}
                    new TuriMethod<>((args) -> string.toLowerCase());
            case "upper_case" ->
                // return the string upper-cased characters.
                // {%S string_upper_case%}
                    new TuriMethod<>((args) -> string.toUpperCase());
            case "swap_case" ->
                // swaps case of each letter.
                // {%S string_swap_case%}
                    new TuriMethod<>((args) -> StringUtils.swapCase(string));
            case "capitalize" ->
                // return the string with the upper-cased first character.
                // {%S string_capitalize%}
                    new TuriMethod<>((args) -> {
                        if (string.isEmpty()) {
                            return "";
                        }
                        if (string.length() == 1) {
                            return string.toUpperCase();
                        }
                        return string.substring(0, 1).toUpperCase() + string.substring(1);

                    });
            case "title" ->
                // return the string with the upper-cased first character of each word.
                // A word is a group of alpha characters that are at the start of the string or follow a white space.
                // {%S string_title%}
                    new TuriMethod<>((args) -> StringUtils.toTitleCase(string));
            case "casefold" ->
                // return the string with cases folded to the lower case.
                // This transformation is aggressive and to be used for case-insensitive comparison.
                // For most of the characters it is the same as `lower_case`, but it also converts many Unicode characters, like `ß` to `ss`.
                // {%S string_casefold%}
                    new TuriMethod<>((args) -> CaseFolder.casefold(string));
            case "trim" ->
                // return the string with spaces removed from the start and the end.
                // {%S string_trim%}
                    new TuriMethod<>((args) -> string.trim());
            case "strip" ->
                // return the string with spaces removed from the start and the end.
                // While `trim()` removes only characters with Unicode code points ≤ U+0020 (i.e., ASCII control characters and basic whitespace like:
                // space ' ', tab '\t', newline '\n', carriage return '\r', etc.) `strip()` removes all Unicode whitespace, using Character.isWhitespace(int), which includes:
                //
                // * ASCII spaces
                // * Unicode spaces like:
                // * U+00A0 (non-breaking space)
                // * U+2003 (em space)
                // * U+202F (narrow no-break space)
                // * U+3000 (ideographic space)
                // {%S string_strip%}
                    new TuriMethod<>((args) -> string.strip());
            case "strip_leading" ->
                // return the string with spaces removed from the start.
                // {%S string_strip_leading%}
                    new TuriMethod<>((args) -> string.stripLeading());
            case "strip_trailing" ->
                // return the string with spaces removed from the end.
                // {%S string_strip_trailing%}
                    new TuriMethod<>((args) -> string.stripTrailing());
            case "strip_indent" ->
                // return the string with some spaces removed from the start of each line, in the case of a multi-line string.
                // It removes so many spaces that the line that starts at the leftmost position will have no leading spaces.
                // This function also removes the spaces from the end of the lines.
                // {%S string_strip_indent%}
                    new TuriMethod<>((args) -> string.stripIndent());
            case "starts_with" ->
                // return `true` if the string starts with the specified string.
                // {%S string_starts_with%}
                    new TuriMethod<>((args) -> string.startsWith(args[0].toString()));
            case "ends_with" ->
                // return `true` if the string ends with the specified string.
                // {%S string_ends_with%}
                    new TuriMethod<>((args) -> string.endsWith(args[0].toString()));
            case "partition" ->
                // Splits a string into three parts and retuns a three-element list.
                // If the string has the format `xxxZZZyyy` where `ZZZ` is the partitioning element, then
                // the list will be `["xxx", "ZZZ", "yyy"]`.
                // If `ZZZ` is not found in the string, then the first element of the list will contain the whole string, and the second and last elements will be empty strings.
                // {%S string_partition%}
                    new TuriMethod<>((args) -> {
                        final var separator = FunUtils.arg("partition", args, String.class);
                        final var result = new LngList();
                        result.addAll(List.of(StringUtils.partition(string, separator)));
                        return result;
                    });
            case "partition_regex" ->
                // Splits a string into three parts and retuns a three-element list.
                // If the string has the format `xxxZZZyyy` where `ZZZ` is a substring that matches the partitioning regular expression, then
                // the list will be `["xxx", "ZZZ", "yyy"]`.
                // If `ZZZ` is not found in the string, then the first element of the list will contain the whole string, and the second and last elements will be empty strings.
                //
                // It is an error to specify a regular expression that matches a zero-length string.
                // {%S string_partition_regex%}
                    new TuriMethod<>((args) -> {
                        final var separator = FunUtils.arg("partition_regex", args, String.class);
                        final var result = new LngList();
                        result.addAll(List.of(StringUtils.partitionRegex(string, separator)));
                        return result;
                    });
            case "split" ->
                // split the string into a list using the argument as a regular expression.
                // If there is no argument, the function uses `"\n"` making it behave the same as `lines()`.
                // Optionally there is a second argument that can limit how many pieces the string is split.
                // If it is `0` then there is no limit, but the trailing empty strings are discarded.
                // It the limit is -1 the trailing empty elements are kept.
                // {%S string_split%}
                    new TuriMethod<>((args) -> {
                        final String splitter;
                        if (args.length == 0) {
                            splitter = "\n";
                        } else {
                            splitter = args[0].toString();
                        }
                        final int limit;
                        if (args.length < 2) {
                            limit = -1;
                        } else {
                            limit = Cast.toLong(args[1]).intValue();
                        }
                        final var list = new LngList();
                        list.array.addAll(Arrays.asList(string.split(splitter, limit)));
                        return list;
                    });
            case "bytes" ->
                // return a list that contains the bytes of the string using UTF-8 character encoding.
                // {%S string_bytes%}
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        final var bytes = string.getBytes(StandardCharsets.UTF_8);
                        for (byte b : bytes) {
                            list.array.add(b);
                        }
                        return list;
                    });
            case "join" ->
                // joins the elements of the argument list using the string as a separator/joiner character.
                // {%S string_join%}
                    new TuriMethod<>((args) -> {
                        if (args.length != 1) {
                            throw new ExecutionException("join() needs one argument, a list");
                        }
                        if (args[0] instanceof LngList list) {
                            return list.array.stream().map(Object::toString).collect(Collectors.joining(string));
                        } else {
                            throw new ExecutionException("join() needs one argument, a list. '%s' is not a list", args[0]);
                        }
                    });
            case "char_at" ->
                // get the character at the given position.
                // {%S string_char_at%}
                    new TuriMethod<>((args) -> "" + string.charAt(Cast.toLong(args[0]).intValue()));
            case "safe_char_at" ->
                // get the character at the given position or an empty string if the index is out of range.
                // {%S string_safe_char_at%}
                    new TuriMethod<>((args) -> {
                        final var arg = FunUtils.arg("safe_char_at", args, Long.class);
                        final var index = arg.intValue();
                        if (index < 0 || index >= string.length()) {
                            return "";
                        }
                        return "" + string.charAt(index);
                    });
            case "index_of" ->
                // return the first position of the argument string, or -1 if the argument string cannot be found in the original string.
                // {%S string_index_of%}
                    new TuriMethod<>((args) -> {
                        final var arguments = FunUtils.args("index_of", args, String.class, Object[].class);
                        final var str = arguments.at(0).as(String.class);
                        if (arguments.N == 2) {
                            final var index = arguments.at(1).as(Integer.class);
                            if (index < 0 || index >= str.length()) {
                                return null;
                            } else {
                                return (long) string.indexOf(str, index);
                            }
                        } else {
                            return (long) string.indexOf(str);
                        }
                    });
            case "last_index_of" ->
                // return the last position of the argument string, or -1 if the argument string cannot be found in the original string.
                // {%S string_last_index_of%}
                    new TuriMethod<>((args) -> {
                        final var arguments = FunUtils.args("index_of", args, String.class, Object[].class);
                        final var str = arguments.at(0).as(String.class);
                        if (arguments.N == 2) {
                            final var index = arguments.at(1).as(Integer.class);
                            if (index < 0 || index >= str.length()) {
                                return null;
                            } else {
                                return (long) string.lastIndexOf(str, index);
                            }
                        } else {
                            return (long) string.lastIndexOf(str);
                        }
                    });
            // end snippet
            default -> null;
        }

                ;
    }


}
