package ch.turic.builtins.classes;

import ch.turic.*;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class TuriString implements TuriClass {
    @Override
    public Class<?> forClass() {
        return String.class;
    }

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
            case "after" ->
                // returns the part of the string that follows the argument string.
                // If the argument string is not in the string, then it returns an empty string.
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
                    new TuriMethod<>((args) -> {
                        final var pad = args.length == 2 ? ("" + args[0]) : " ";
                        ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                        final var n = Cast.toLong(args.length == 2 ? args[1] : args[0]).intValue();
                        if (n <= string.length()) {
                            return string;
                        }
                        final var k = (n - string.length()) / pad.length();
                        return pad.repeat(k) + string;
                    });
            case "pad_right" ->
                // add character to the right of the string to get the desired length.
                // This method works the same way as `pad_left`, but it appends the characters.
                    new TuriMethod<>((args) -> {
                        final var pad = args.length == 2 ? ("" + args[0]) : " ";
                        ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                        final var n = Cast.toLong(args.length == 2 ? args[1] : args[0]).intValue();
                        if (n <= string.length()) {
                            return string;
                        }
                        final var k = (n - string.length()) / pad.length();
                        return string + pad.repeat(k);
                    });

            case "between" ->
                // returns the part of the string that is between two specific strings.
                // if one of the parameter strings is not found in the string, the result is empty string.
                    new TuriMethod<>((args) -> {
                        final var afterWhat = "" + args[0];
                        final var beforeWhat = "" + args[1];
                        final var endIndex = string.indexOf(beforeWhat);
                        final var pos2 = string.indexOf(afterWhat);
                        if (endIndex < 0 || pos2 < 0) {
                            return "";
                        }
                        int beginIndex = pos2 + afterWhat.length();
                        if( endIndex <= beginIndex) {
                            return "";
                        }
                        return string.substring(beginIndex, endIndex);
                    });
            case "lines" ->
                // returns a list containing the lines of the string.
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        list.array.addAll(Arrays.asList(string.split("\n", -1)));
                        return list;
                    });
            case "words" ->
                // returns a list containing the words of the string.
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        list.array.addAll(Arrays.asList(string.split("\\W+", -1)));
                        return list;
                    });
            case "turi_lex" ->
                // returns a list containing the tokens of the string matching the tokenization of the language (Turicum).
                    new TuriMethod<>((args) -> Lexer.analyze((ch.turic.analyzer.Input) Input.fromString(string)));
            case "execute" -> (LngCallable.LngCallableClosure)// do not remove, it is NOT redundant!
                    // executes the string as Turicum code.
                    (context, args) -> {
                        final var ctx = FunUtils.ctx(context);
                        final var analyzer = new ProgramAnalyzer();
                        Command code = analyzer.analyze(Lexer.analyze((ch.turic.analyzer.Input) Input.fromString(string)));
                        return code.execute(ctx);
                    };
            case "url_encode" ->
                // encode the string for URL
                    new TuriMethod<>((args) -> URLEncoder.encode(string, StandardCharsets.UTF_8));
            case "url_decode"
                // decode the string from URL-encoded form
                    -> new TuriMethod<>((args) -> URLDecoder.decode(string, StandardCharsets.UTF_8));
            case "md5" ->
                // calculate the md5 hash of the string
                    new TuriMethod<>((args) -> digest(string, "MD5"));
            case "sha_1" ->
                // calculate the sha-1 hash of the string
                    new TuriMethod<>((args) -> digest(string, "SHA-1"));
            case "sha_256" ->
                // calculate the sha-256 hash of the string
                    new TuriMethod<>((args) -> digest(string, "SHA-256"));
            case "sha_512" ->
                // calculate the sha-512 hash of the string
                    new TuriMethod<>((args) -> digest(string, "SHA-512"));
            case "digest" ->
                // calculate the hash of the string. The algorithm name has to be provided as argument.
                    new TuriMethod<>((args) -> digest(string, "" + args[0]));
            case "base64" ->
                // base64 encode the string
                    new TuriMethod<>((args) -> {
                        Base64.Encoder encoder = Base64.getEncoder();
                        return encoder.encodeToString(string.getBytes(StandardCharsets.UTF_8));
                    });
            case "from_base64" ->
                // create a list of numbers (bytes) from a base64 encoded string
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
                    new TuriMethod<>((args) -> {
                        Base64.Decoder decoder = Base64.getDecoder();
                        return new String(decoder.decode(string), StandardCharsets.UTF_8);
                    });
            case "contains" ->
                // returns `true` if the string contains the argument.
                    new TuriMethod<>((args) -> string.contains("" + args[0]));
            case "is_blank" ->
                // returns `true` if the string is blank
                    new TuriMethod<>((args) -> string.isBlank());
            case "is_empty" ->
                // returns `true` if the string is empty
                    new TuriMethod<>((args) -> string.isEmpty());
            case "is_numeric" ->
                // returns `true` if the string holds a decimal numeric value, either integer or floating point formatted.
                    new TuriMethod<>((args) -> string.matches("^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$"));
            case "is_digit" ->
                // returns `true` if the string contains only digits
                    new TuriMethod<>((args) -> string.matches("^\\d+$"));
            case "is_alpha" ->
                // returns `true` if the string contains only alpha characters
                    new TuriMethod<>((args) -> string.matches("^[a-zA-Z]+$"));
            case "is_alphanumeric" ->
                // returns `true` if the string contains only alphanumeric characters
                    new TuriMethod<>((args) -> string.matches("^[a-zA-Z0-9]+$"));
            case "is_hex" ->
                // returns `true` if the string contains only hexadecimal characters
                    new TuriMethod<>((args) -> string.matches("^(0x)?[a-fA-F0-9]+$"));
            case "int" ->
                // returns the integer value of the string
                    new TuriMethod<>((args) -> Long.parseLong(string.trim()));
            case "float" ->
                // returns the floating number contained in the string
                    new TuriMethod<>((args) -> Double.parseDouble(string.trim()));
            case "number" ->
                // returns the number contained in the string encoded with the argument radix
                    new TuriMethod<>((args) -> {
                        FunUtils.oneArg("number", args);
                        final var radix = Cast.toLong(args[0]).intValue();
                        return Long.parseLong(string.trim(), radix);
                    });
            case "hex" ->
                // returns the number contained in the string as a hexadecimal number.
                // It is not the same as calling `"0AF6"number(16)` because this method respects the leading `0x` if there is any.
                    new TuriMethod<>((args) -> {
                        final var hexString = string.trim();
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
                    new TuriMethod<>((args) -> {
                        int beginIndex = Cast.toLong(args[0]).intValue();
                        if (beginIndex >= string.length()) {
                            return "";
                        }
                        if (args.length == 1) {
                            return string.substring(beginIndex);
                        }
                        if (args.length == 2) {
                            int endIndex = Cast.toLong(args[1]).intValue();
                            if (endIndex > string.length()) {
                                return string.substring(beginIndex);
                            }
                            return string.substring(beginIndex, endIndex);
                        }
                        throw new ExecutionException("substring() needs one or two arguments");
                    });
            case "remove_prefix" ->
                // removes the argument from the string if that is a prefix of the string.
                    new TuriMethod<>((args) -> {
                        final var prefix = "" + args[0];
                        if (string.startsWith(prefix)) {
                            return string.substring(prefix.length());
                        } else {
                            return string;
                        }
                    });
            case "remove_postfix" ->
                // removes the argument from the string if that is a postfix of the string.
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
                    new TuriMethod<>((args) -> {
                        int n = Cast.toLong(args[0]).intValue();
                        if (n >= string.length()) {
                            return string;
                        }
                        return string.substring(string.length() - n);
                    });
            case "replace_all" ->
                // replaces the occurrences of the first argument interpreted as regular expression with the second argument interpreted as string.
                    new TuriMethod<>((args) -> string.replaceAll("" + args[0], "" + args[1]));
            case "quote" ->
                // quotes the special characters in the string.
                // The result is a string that can be used in a string literal to represent the original string.
                    new TuriMethod<>((args) -> string.replace("\\", "\\\\")
                            .replace("\t", "\\t")
                            .replace("\b", "\\b")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\f", "\\f")
                            .replace("\"", "\\\""));
            case "reverse" ->
                // returns the string that contains the same characters as the original string, but in reverse order.
                    new TuriMethod<>((args) -> new StringBuilder(string).reverse().toString());
            case "chop" ->
                // returns string without the last character
                    new TuriMethod<>((args) -> {
                        if (string.isEmpty()) {
                            return "";
                        }
                        return string.substring(0, string.length() - 1);
                    });
            case "chomp" ->
                // returns the string without the last new-line character.
                // If the last character is not a new line character, the original string is returned.
                    new TuriMethod<>((args) -> string.endsWith("\n") ? string.substring(0, string.length() - 1) : string);
            case "times" ->
                // repeat the string as specified by the argument. `"x".times(3)` is `"xxx"`.
                    new TuriMethod<>((args) -> string.repeat(Cast.toLong(args[0]).intValue()));
            case "lower_case" ->
                // return the string lower-cased character.
                    new TuriMethod<>((args) -> string.toLowerCase());
            case "upper_case" ->
                // return the string upper-cased characters.
                    new TuriMethod<>((args) -> string.toUpperCase());
            case "trim" ->
                // return the string with spaces removed from the start and the end.
                    new TuriMethod<>((args) -> string.trim());
            case "starts_with" ->
                // return `true` if the string starts with the specified string.
                    new TuriMethod<>((args) -> string.startsWith(args[0].toString()));
            case "ends_with" ->
                // return `true` if the string ends with the specified string.
                    new TuriMethod<>((args) -> string.endsWith(args[0].toString()));
            case "split" ->
                // split the string into a list using the argument as a regular expression.
                    new TuriMethod<>((args) -> {
                        final var list = new LngList();
                        list.array.addAll(Arrays.asList(string.split(args[0].toString(), -1)));
                        return list;
                    });
            case "bytes" ->
                // return a list that contains the bytes of the string using UTF-8 character encoding.
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
                    new TuriMethod<>((args) -> "" + string.charAt(Cast.toLong(args[0]).intValue()));
            case "safe_char_at" ->
                // get the character at the given position or an empty string if the index is out of range.
                    new TuriMethod<>((args) -> {
                        final var index = Cast.toLong(args[0]).intValue();
                        if (index < 0 || index >= string.length()) {
                            return "";
                        }
                        return "" + string.charAt(index);
                    });
            case "index_of" ->
                // return the first position of the argument string, or -1 if the argument string cannot be found in the original string.
                    new TuriMethod<>((args) -> (long) string.indexOf(args[0].toString()));
            case "last_index_of" ->
                // return the last position of the argument string, or -1 if the argument string cannot be found in the original string.
                    new TuriMethod<>((args) -> (long) string.lastIndexOf(args[0].toString()));
            // end snippet
            default -> null;
        }

                ;
    }

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
}
