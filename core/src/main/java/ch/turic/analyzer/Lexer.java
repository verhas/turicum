package ch.turic.analyzer;


import ch.turic.BadSyntax;

import java.util.*;

public class Lexer {

    public static final Set<String> RESERVED = new HashSet<>(Set.of(
            Keywords.CLASS, Keywords.PIN, Keywords.FN, Keywords.LET, Keywords.GLOBAL, Keywords.IF, Keywords.ELSE,
            Keywords.ELSEIF, Keywords.DIE, Keywords.BREAK, Keywords.CONTINUE, Keywords.WHILE, Keywords.WITH, Keywords.UNTIL, Keywords.FOR, Keywords.FLOW,
            Keywords.EACH, Keywords.LIST, Keywords.IN, Keywords.RETURN, Keywords.YIELD, Keywords.WHEN, Keywords.TRY, Keywords.CATCH,
            Keywords.FINALLY, Keywords.ASYNC, Keywords.AWAIT, Keywords.AS, Keywords.PRINT, Keywords.PRINTLN, Keywords.MUT
    ));
    final static private ArrayList<String> _OPERANDS = new ArrayList<>(Arrays.asList(
            // snippet OPERANDS
            "---", "+++", "--", "++", "->", "<-", "(", ")", ",", ".(", ".", "?.",
            "&{", "{", "}", "[", "]", ";", ":", "|", "?", "@", "^", "##", "#", "**"
            // end snippet
    ));
    final static public String[] ASSIGNMENT_OPERATORS = {
            // snipline ASSIGNMENT_OPERATORS
            "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "**=", "or=", "&&=", "||=", "<<=", ">>=", ">>>=",
    };

    static {
        Arrays.stream(BinaryExpressionAnalyzer.binaryOperators).flatMap(Arrays::stream).forEach(
                s -> {
                    if (Character.isAlphabetic(s.charAt(0))) {
                        RESERVED.add(s);
                    } else {
                        _OPERANDS.add(s);
                    }
                }
        );
        Arrays.stream(UnaryExpressionAnalyzer.unaryOperators).forEach(
                s -> {
                    if (Character.isAlphabetic(s.charAt(0))) {
                        RESERVED.add(s);
                    } else {
                        _OPERANDS.add(s);
                    }
                }
        );
        _OPERANDS.addAll(List.of(ASSIGNMENT_OPERATORS));

        _OPERANDS.sort((a, b) -> Integer.compare(b.length(), a.length()));
    }

    private static final String[] OPERANDS = _OPERANDS.toArray(String[]::new);

    private static final String[] uniKeys = {
            "∞", "inf",
            "∅", "none",
            "∈", "in",
            "⇶", "async",
            "⏳", "await",
    };

    private static String getUnicodeKeyword(String ch) {
        for (int i = 0; i < uniKeys.length; i += 2) {
            if (ch.equals(uniKeys[i])) {
                return uniKeys[i + 1];
            }
        }
        return null;
    }

    private static final String[] uniSymbols = {
            "…", "..",
            "→", "->",
            "≠", "!=",
            "≥", ">=",
            "≤", "<=",
    };

    /**
     * Retrieves the ASCII representation of a given symbol, if available.
     * Iterates through a predefined list of symbol mappings to find a match
     * and returns its corresponding multi-character ASCII representation.
     *
     * @param ch the symbol whose Unicode representation is to be retrieved
     * @return the Unicode representation of the given symbol if found; otherwise, null
     */
    private static String getUnicodeSymbol(String ch) {
        for (int i = 0; i < uniSymbols.length; i += 2) {
            if (ch.equals(uniSymbols[i])) {
                return uniSymbols[i + 1];
            }
        }
        return null;
    }

    /****
     * Performs lexical analysis on the provided input, converting source code a into a list of lexical tokens.
     * <p>
     * This method recognizes and tokenizes reserved keywords, identifiers, operands, string and numeric literals, comments (including nested multi-line comments), Unicode keywords and symbols, and handles shebang lines. Throws a BadSyntax exception if an unexpected character is encountered.
     *
     * @param in the input stream containing source code to be tokenized
     * @return a LexList containing the sequence of lexical tokens extracted from the input
     * @throws BadSyntax if an unexpected or invalid character is encountered during tokenization
     */
    public static LexList analyze(Input in) throws BadSyntax {
        final var list = new ArrayList<Lex>();
        return analyze_into(in, list, false);
    }

    public static LexList try_analyze(Input in) {
        final var list = new ArrayList<Lex>();
        try {
            analyze_into(in, list, true);
        } catch (BadSyntax bs) {
            final var result = new LexList(list);
            result.setBs(bs);
            return result;
        }
        return new LexList(list);
    }


    /**
     * Performs lexical analysis on the provided input, tokenizing the source code into lexical tokens
     * and adding them to the provided list. Handles various types of tokens, including identifiers,
     * reserved keywords, comments, strings, numbers, operators, and special symbols. The method
     * also processes shebang lines and can handle nested multi-line comments.
     *
     * @param in         the input source to be analyzed
     * @param list       the list to store the resulting Lex tokens
     * @param collectAll a flag indicating whether to collect all tokens, including whitespace and comments
     * @return a LexList containing the lexical tokens generated from the input source
     * @throws BadSyntax if an unexpected character or syntax error is encountered
     */
    private static LexList analyze_into(Input in, List<Lex> list, boolean collectAll) throws BadSyntax {
        BadSyntax bs = null;
        // honor the shebang
        if (in.startsWith("#!")) {
            final var p = in.position.clone();
            final var sheBangLine = new StringBuilder();
            while (in.startsWith("\n")) {
                in.move(1, sheBangLine);
            }
            if (collectAll) {
                list.add(new Lex(Lex.Type.SPACES, sheBangLine.toString(), true, p));
            }
        }
        boolean nextAtLineStart = false;
        while (!in.isEmpty()) {
            boolean atLineStart = nextAtLineStart;// the first line start does not matter
            final var position = in.position.clone();
            if ((in.startsWith("\n") || in.startsWith("\r"))) {
                if (collectAll) {
                    list.add(new Lex(Lex.Type.SPACES, in.substring(0, 1), atLineStart, position));
                }
                nextAtLineStart = true;
                in.skip(1);
                continue;
            }

            if (Character.isWhitespace(in.charAt(0))) {
                final var sb = new StringBuilder();
                while (!in.isEmpty() && Character.isWhitespace(in.charAt(0))) {
                    in.move(1, sb);
                }
                if (collectAll) {
                    list.add(new Lex(Lex.Type.SPACES, sb.toString(), atLineStart, position));
                }
                continue;
            }
            nextAtLineStart = false;
            if (in.startsWith("/*")) {
                final BadSyntax commentBS;
                if (collectAll) {
                    final var sb = new StringBuilder();
                    commentBS = fetchMLComment(in, sb);
                    list.add(new Lex(Lex.Type.COMMENT, sb.toString(), atLineStart, position));
                } else {
                    commentBS = fetchMLComment(in, new StringBuilder());
                }
                if (bs == null) {
                    bs = commentBS;
                }
                continue;
            }
            if (in.startsWith("//")) {
                final var comment = fetchComment(in);
                if (collectAll) {
                    list.add(new Lex(Lex.Type.COMMENT, comment, atLineStart, position));
                }
                continue;
            }
            if (in.startsWith("`")) {
                final var pair = StringFetcher.fetchQuotedId(in);
                list.add(Lex.identifier(pair.a(), pair.b(), atLineStart, position));
                continue;
            }
            final var uniKeyword = getUnicodeKeyword(in.substring(0, 1));
            if (uniKeyword != null) {
                if (RESERVED.contains(uniKeyword)) {
                    list.add(Lex.reserved(uniKeyword, atLineStart, position));
                } else {
                    list.add(Lex.identifier(uniKeyword, atLineStart, position));
                }
                in.skip(1);
                continue;
            }
            int operandIndex = in.select(OPERANDS);
            if (operandIndex >= 0) {
                final var lex = Lex.reserved(OPERANDS[operandIndex], atLineStart, position);
                list.add(lex);
                in.skip(OPERANDS[operandIndex].length());
                continue;
            }
            if (Input.validId1stChar(in.charAt(0))) {
                final var id = StringFetcher.fetchId(in);
                if (RESERVED.contains(id)) {
                    list.add(Lex.reserved(id, atLineStart, position));
                } else {
                    list.add(Lex.identifier(id, atLineStart, position));
                }
                continue;
            }
            if (in.startsWith("$\"")) {
                in.skip(1);
                final var pair = ch.turic.analyzer.StringFetcher.getPair(in);
                final var lex = Lex.string(pair.a(), "$" + pair.b(), atLineStart, position);
                list.add(lex);
                continue;
            }
            if (in.startsWith("\"")) {
                final var pair = ch.turic.analyzer.StringFetcher.getPair(in);
                final var lex = Lex.string(pair.a(), pair.b(), atLineStart, position);
                list.add(lex);
                continue;
            }
            if (in.length() > 2 && (in.startsWithIgnoreCase("0x"))) {
                final var str = new StringBuilder();
                in.move(2, str);
                str.append(in.fetchHexNumber());
                final var lex = new Lex(Lex.Type.INTEGER, str.toString(), atLineStart, position);
                list.add(lex);
                continue;
            }
            if (Character.isDigit(in.charAt(0))) {
                final var str = new StringBuilder();
                str.append(in.fetchNumber());
                final Lex.Type type;
                if (in.length() >= 2 && (in.startsWith(".") && Character.isDigit(in.charAt(1))) || in.startsWithIgnoreCase("e")) {
                    if (in.startsWith(".")) {
                        in.move(1, str);
                        str.append(in.fetchNumber());
                    }
                    if (in.startsWithIgnoreCase("e")) {
                        in.move(1, str);
                        if (in.startsWithEither("+", "-")) {
                            in.move(1, str);
                        }
                        str.append(in.fetchNumber());
                    }
                    type = Lex.Type.FLOAT;
                } else {
                    type = Lex.Type.INTEGER;
                }
                final var lex = new Lex(type, str.toString(), atLineStart, position);
                list.add(lex);
                continue;
            }
            final var uniSym = getUnicodeSymbol(in.substring(0, 1));
            if (uniSym != null) {
                final var lex = Lex.reserved(uniSym, atLineStart, position);
                list.add(lex);
                in.skip(1);
                continue;
            }
            if (collectAll) {
                list.add(Lex.character(in, atLineStart, position));
                in.skip(1);
                if (bs == null) {
                    bs = new BadSyntax(in.position, "Unexpected character '" + in.charAt(0) + "' in the input");
                }
            } else {
                throw new BadSyntax(in.position, "Unexpected character '" + in.charAt(0) + "' in the input");
            }
        }
        if (bs != null) {
            throw bs;
        }
        return new LexList(list);
    }

    /**
     * Parses and extracts a multi-line comment from the input, considering nested comments if present.
     * The method appends the parsed comment content to the provided StringBuilder.
     * If the comment is not closed correctly, it returns a BadSyntax exception.
     * In this case, the comment in the builder will contain all the characters of the input from the `/*`
     * till the end of the input.
     *
     * @param in the input source to be analyzed for the multi-line comment
     * @param sb the StringBuilder to which the parsed comment content is appended
     * @return null if the multi-line comment is successfully parsed, otherwise throws a BadSyntax exception
     * for unclosed block comments
     * @throws BadSyntax if the multi-line comment is unclosed or contains invalid syntax
     */
    private static BadSyntax fetchMLComment(final Input in, final StringBuilder sb) {
        in.move(2, sb);
        final var pos = in.position.clone();
        while (in.length() >= 2 && !in.startsWith("*/")) {
            if (in.startsWith("/*")) {
                final var bs = fetchMLComment(in, sb);
                if (bs != null) {
                    return bs;
                }
            } else {
                in.move(1, sb);
            }
        }
        if (in.length() < 2) {
            return new BadSyntax(pos, "Unclosed block comment");
        }
        in.try_move(2, sb);
        return null;
    }

    /**
     * Skip the comment until the end of the line. The end of the line is not skipped.
     *
     * @param in the input
     */
    private static String fetchComment(final Input in) {
        final var sb = new StringBuilder();
        while (!in.isEmpty() && !in.startsWith("\n")) {
            in.move(1, sb);
        }
        return sb.toString();
    }
}
