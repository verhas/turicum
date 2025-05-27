package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;
import ch.turic.analyzer.Input;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Command;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
            case "to_string" -> new TuriMethod<>((args)-> String.format("%s",string));
            case "after" -> new TuriMethod<>((args) -> {
                final var afterWhat = "" + args[0];
                final var pos = string.indexOf(afterWhat);
                if (pos < 0) {
                    return "";
                }
                return string.substring(pos + afterWhat.length());
            });
            case "before" -> new TuriMethod<>((args) -> {
                final var beforeWhat = "" + args[0];
                final var pos = string.indexOf(beforeWhat);
                if (pos < 0) {
                    return "";
                }
                return string.substring(0, pos);
            });
            case "pad_left" -> new TuriMethod<>((args) -> {
                final var pad = args.length == 2 ? ("" + args[0]) : " ";
                ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                final var n = Cast.toLong(args.length == 2 ? args[1] : args[0]).intValue();
                if (n <= string.length()) {
                    return string;
                }
                final var k = (n - string.length()) / pad.length();
                return pad.repeat(k) + string;
            });
            case "pad_right" -> new TuriMethod<>((args) -> {
                final var pad = args.length == 2 ? ("" + args[0]) : " ";
                ExecutionException.when(pad.isEmpty(), "cannot pad with empty string");
                final var n = Cast.toLong(args.length == 2 ? args[1] : args[0]).intValue();
                if (n <= string.length()) {
                    return string;
                }
                final var k = (n - string.length()) / pad.length();
                return string + pad.repeat(k);
            });

            case "between" -> new TuriMethod<>((args) -> {
                final var afterWhat = "" + args[0];
                final var beforeWhat = "" + args[1];
                final var pos1 = string.indexOf(beforeWhat);
                final var pos2 = string.indexOf(afterWhat);
                if (pos1 < 0 || pos2 < 0) {
                    return "";
                }
                return string.substring(pos2 + afterWhat.length(), pos1);
            });
            case "lines" -> new TuriMethod<>((args) -> {
                final var list = new LngList();
                list.array.addAll(Arrays.asList(string.split("\n", -1)));
                return list;
            });
            case "words" -> new TuriMethod<>((args) -> {
                final var list = new LngList();
                list.array.addAll(Arrays.asList(string.split("\\W+", -1)));
                return list;
            });
            case "turi_lex" -> new TuriMethod<>((args) -> Lexer.analyze(Input.fromString(string)));
            case "execute" -> (LngCallable.LngCallableClosure) (context, args) -> {
                final var ctx = FunUtils.ctx(context);
                final var analyzer = new ProgramAnalyzer();
                Command code = analyzer.analyze(Lexer.analyze(Input.fromString(string)));
                Object result = null;
                try {
                    result = code.execute(ctx);
                } catch (ExecutionException e) {
                    final var newStackTrace = new ArrayList<StackTraceElement>();
                    for (final var stackFrame : ctx.threadContext.getStackTrace()) {
                        if (stackFrame.command().startPosition() != null) {
                            newStackTrace.add(new StackTraceElement(
                                    stackFrame.command().getClass().getSimpleName(),
                                    "",
                                    stackFrame.command().startPosition().file,
                                    stackFrame.command().startPosition().line
                            ));
                        }
                    }
                    final var turiException = new ExecutionException(e);
                    turiException.setStackTrace(newStackTrace.toArray(StackTraceElement[]::new));
                    result = turiException;
                }
                return result;
            };
            case "url_encode" -> new TuriMethod<>((args) -> URLEncoder.encode(string, StandardCharsets.UTF_8));
            case "url_decode" -> new TuriMethod<>((args) -> URLDecoder.decode(string, StandardCharsets.UTF_8));
            case "md5" -> new TuriMethod<>((args) -> digest(string, "MD5"));
            case "sha_1" -> new TuriMethod<>((args) -> digest(string, "SHA-1"));
            case "sha_256" -> new TuriMethod<>((args) -> digest(string, "SHA-256"));
            case "sha_512" -> new TuriMethod<>((args) -> digest(string, "SHA-512"));
            case "digest" -> new TuriMethod<>((args) -> digest(string, "" + args[0]));
            case "base64" -> new TuriMethod<>((args) -> {
                Base64.Encoder encoder = Base64.getEncoder();
                return encoder.encodeToString(string.getBytes(StandardCharsets.UTF_8));
            });
            case "from_base64" -> new TuriMethod<>((args) -> {
                Base64.Decoder decoder = Base64.getDecoder();
                final var bytes = decoder.decode(string);
                final var list = new LngList();
                for (byte aByte : bytes) {
                    list.array.add(aByte);
                }
                return list;
            });
            case "from_base64_str" -> new TuriMethod<>((args) -> {
                Base64.Decoder decoder = Base64.getDecoder();
                return new String(decoder.decode(string), StandardCharsets.UTF_8);
            });
            case "contains" -> new TuriMethod<>((args) -> string.contains("" + args[0]));
            case "is_blank" -> new TuriMethod<>((args) -> string.isBlank());
            case "is_empty" -> new TuriMethod<>((args) -> string.isEmpty());
            case "is_numeric" -> new TuriMethod<>((args) -> string.matches("^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$"));
            case "is_digit" -> new TuriMethod<>((args) -> string.matches("^\\d+$"));
            case "is_alpha" -> new TuriMethod<>((args) -> string.matches("^[a-zA-Z]+$"));
            case "is_alphanumeric" -> new TuriMethod<>((args) -> string.matches("^[a-zA-Z0-9]+$"));
            case "is_hex" -> new TuriMethod<>((args) -> string.matches("^(0x)?[a-fA-F0-9]+$"));
            case "hex" -> new TuriMethod<>((args) -> {
                final var hexString = string.trim();
                if (hexString.startsWith("0x") || hexString.startsWith("0X")) {
                    return Long.parseLong(hexString.substring(2), 16);
                }
                return Long.parseLong(hexString, 16);
            });
            case "substring" -> new TuriMethod<>((args) -> {
                if (args.length == 1) {
                    return string.substring(Cast.toLong(args[0]).intValue());
                }
                if (args.length == 2) {
                    return string.substring(Cast.toLong(args[0]).intValue(), Cast.toLong(args[1]).intValue());
                }
                throw new ExecutionException("substring() needs one or two arguments");
            });
            case "remove_prefix" -> new TuriMethod<>((args) -> {
                final var prefix = "" + args[0];
                if (string.startsWith(prefix)) {
                    return string.substring(prefix.length());
                } else {
                    return string;
                }
            });
            case "remove_postfix" -> new TuriMethod<>((args) -> {
                final var postfix = "" + args[0];
                if (string.endsWith(postfix)) {
                    return string.substring(0, string.length() - postfix.length());
                } else {
                    return string;
                }
            });
            case "count_substring" -> new TuriMethod<>((args) -> {
                final String substr = "" + args[0];
                int i = 0;
                long count = 0;
                while ((i = string.indexOf(substr, i)) >= 0) {
                    i += substr.length();
                    count++;
                }
                return count;
            });
            case "left" -> new TuriMethod<>((args) -> string.substring(0, Cast.toLong(args[0]).intValue()));
            case "right" ->
                    new TuriMethod<>((args) -> string.substring(string.length() - Cast.toLong(args[0]).intValue()));
            case "replace_all" -> new TuriMethod<>((args) -> string.replaceAll("" + args[0], "" + args[1]));
            case "quote" -> new TuriMethod<>((args) -> string.replace("\\", "\\\\")
                    .replace("\t", "\\t")
                    .replace("\b", "\\b")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\f", "\\f")
                    .replace("\"", "\\\""));
            case "reverse" -> new TuriMethod<>((args) -> new StringBuilder(string).reverse().toString());
            case "chop" -> new TuriMethod<>((args) -> string.substring(0, string.length() - 1));
            case "chomp" ->
                    new TuriMethod<>((args) -> string.endsWith("\n") ? string.substring(0, string.length() - 1) : string);
            case "times" -> new TuriMethod<>((args) -> string.repeat(Cast.toLong(args[0]).intValue()));
            case "lower_case" -> new TuriMethod<>((args) -> string.toLowerCase());
            case "upper_case" -> new TuriMethod<>((args) -> string.toUpperCase());
            case "trim" -> new TuriMethod<>((args) -> string.trim());
            case "starts_with" -> new TuriMethod<>((args) -> string.startsWith(args[0].toString()));
            case "ends_with" -> new TuriMethod<>((args) -> string.endsWith(args[0].toString()));
            case "split" -> new TuriMethod<>((args) -> {
                final var list = new LngList();
                list.array.addAll(Arrays.asList(string.split(args[0].toString(), -1)));
                return list;
            });
            case "bytes" -> new TuriMethod<>((args) -> {
                final var list = new LngList();
                final var bytes = string.getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    list.array.add(b);
                }
                return list;
            });
            case "join" -> new TuriMethod<>((args) -> {
                if (args.length != 1) {
                    throw new ExecutionException("join() needs one argument, a list");
                }
                if (args[0] instanceof LngList list) {
                    return list.array.stream().map(Object::toString).collect(Collectors.joining(string));
                } else {
                    throw new ExecutionException("join() needs one argument, a list. '%s' is not a list", args[0]);
                }
            });
            case "char_at" -> new TuriMethod<>((args) -> "" + string.charAt(Cast.toLong(args[0]).intValue()));
            case "safe_char_at" -> new TuriMethod<>((args) -> {
                final var index = Cast.toLong(args[0]).intValue();
                if (index < 0 || index >= string.length()) {
                    return "";
                }
                return "" + string.charAt(index);
            });
            case "index_of" -> new TuriMethod<>((args) -> (long) string.indexOf(args[0].toString()));
            case "last_index_of" -> new TuriMethod<>((args) -> (long) string.lastIndexOf(args[0].toString()));
            default -> null;
        };
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
