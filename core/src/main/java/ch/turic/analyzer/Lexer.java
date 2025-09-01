package ch.turic.analyzer;


import ch.turic.BadSyntax;

import java.util.*;

public class Lexer {

    final static public Set<String> RESERVED = new HashSet<>(Set.of(
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

    private static String getUnicodeSymbol(String ch) {
        for (int i = 0; i < uniSymbols.length; i += 2) {
            if (ch.equals(uniSymbols[i])) {
                return uniSymbols[i + 1];
            }
        }
        return null;
    }

    /****
     * Performs lexical analysis on the provided input, converting source code text into a list of lexical tokens.
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


    private static LexList analyze_into(Input in, List<Lex> list, boolean collectAll) throws BadSyntax {
        // honor the shebang
        if (in.startsWith("#!") == 0) {
            final var p = in.position.clone();
            final var sheBangLine = new StringBuilder();
            while (!in.isEmpty() && in.charAt(0) != '\n') {
                sheBangLine.append(in.charAt(0));
                in.skip(1);
            }
            if (collectAll) {
                list.add(new Lex(Lex.Type.TEXT, sheBangLine.toString(), true, p));
            }
        }
        boolean nextAtLineStart = false;
        while (!in.isEmpty()) {
            boolean atLineStart = nextAtLineStart;// the first line start does not matter
            final var position = in.position.clone();
            if ((in.charAt(0) == '\n' || in.charAt(0) == '\r')) {
                if (collectAll) {
                    list.add(new Lex(Lex.Type.TEXT, in.substring(0, 1), atLineStart, position));
                }
                nextAtLineStart = true;
                in.skip(1);
                continue;
            }

            if (Character.isWhitespace(in.charAt(0))) {
                final var sb = new StringBuilder();
                while (!in.isEmpty() && Character.isWhitespace(in.charAt(0))) {
                    sb.append(in.charAt(0));
                    in.skip(1);
                }
                if (collectAll) {
                    list.add(new Lex(Lex.Type.TEXT, sb.toString(), atLineStart, position));
                }
                continue;
            }
            nextAtLineStart = false;
            if (in.length() >= 2 && in.charAt(0) == '/' && in.charAt(1) == '*') {
                in.skip(2);
                final var mlComment = fetchMLComment(in);
                if (collectAll) {
                    list.add(new Lex(Lex.Type.COMMENT, mlComment, atLineStart, position));
                }
                continue;
            }
            if (in.length() >= 2 && in.charAt(0) == '/' && in.charAt(1) == '/') {
                final var comment = fetchComment(in);
                if (collectAll) {
                    list.add(new Lex(Lex.Type.TEXT, comment, atLineStart, position));
                }
                continue;
            }
            if (in.charAt(0) == '`') {
                final var id = StringFetcher.fetchId(in);
                list.add(new Lex(Lex.Type.IDENTIFIER, id, atLineStart, position));
                continue;
            }
            final var uniKeyword = getUnicodeKeyword("" + in.charAt(0));
            if (uniKeyword != null) {
                if (RESERVED.contains(uniKeyword)) {
                    list.add(new Lex(Lex.Type.RESERVED, uniKeyword, atLineStart, position));
                } else {
                    list.add(new Lex(Lex.Type.IDENTIFIER, uniKeyword, atLineStart, position));
                }
                in.skip(1);
                continue;
            }
            int operandIndex = in.startsWith(OPERANDS);
            if (operandIndex >= 0) {
                final var lex = new Lex(Lex.Type.RESERVED, OPERANDS[operandIndex], atLineStart, position);
                list.add(lex);
                in.skip(OPERANDS[operandIndex].length());
                continue;
            }
            if (Input.validId1stChar(in.charAt(0))) {
                final var id = in.fetchId();
                if (RESERVED.contains(id)) {
                    list.add(new Lex(Lex.Type.RESERVED, id, atLineStart, position));
                } else {
                    list.add(new Lex(Lex.Type.IDENTIFIER, id, atLineStart, position));
                }
                continue;
            }
            if (in.charAt(0) == '$' && in.length() >= 2 && in.charAt(1) == '"') {
                in.skip(1);
                final var str = ch.turic.analyzer.StringFetcher.getString(in);
                final var lex = new Lex(Lex.Type.STRING, str, atLineStart, position, true);
                list.add(lex);
                continue;
            }
            if (in.charAt(0) == '"') {
                final var str = ch.turic.analyzer.StringFetcher.getString(in);
                final var lex = new Lex(Lex.Type.STRING, str, atLineStart, position, false);
                list.add(lex);
                continue;
            }
            if (in.length() > 2 && in.charAt(0) == '0' && (in.charAt(1) == 'x' || in.charAt(1) == 'X')) {
                final var str = new StringBuilder(in.substring(0, 2));
                in.skip(2);
                str.append(in.fetchHexNumber());
                final var lex = new Lex(Lex.Type.INTEGER, str.toString(), atLineStart, position);
                list.add(lex);
                continue;
            }
            if (Character.isDigit(in.charAt(0))) {
                final var str = new StringBuilder();
                str.append(in.fetchNumber());
                final Lex.Type type;
                if (in.length() >= 2 && (in.charAt(0) == '.' && Character.isDigit(in.charAt(1))) || (!in.isEmpty() && (in.charAt(0) == 'e' || in.charAt(0) == 'E'))) {
                    if (in.charAt(0) == '.') {
                        str.append('.');
                        in.skip(1);
                        str.append(in.fetchNumber());
                    }
                    if (!in.isEmpty() && (in.charAt(0) == 'e' || in.charAt(0) == 'E')) {
                        str.append('e');
                        in.skip(1);
                        if (!in.isEmpty() && (in.charAt(0) == '+' || in.charAt(0) == '-')) {
                            str.append(in.charAt(0));
                            in.skip(1);
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
            final var uniSym = getUnicodeSymbol("" + in.charAt(0));
            if (uniSym != null) {
                final var lex = new Lex(Lex.Type.RESERVED, uniSym, atLineStart, position);
                list.add(lex);
                in.skip(1);
                continue;
            }
            throw new BadSyntax(in.position, "Unexpected character '" + in.charAt(0) + "' in the input");
        }
        return new LexList(list);
    }

    /**
     * Skip the multi-line comment, can be nested
     *
     * @param in the input
     */
    private static String fetchMLComment(final Input in) {
        final var sb = new StringBuilder("/*");
        final var pos = in.position.clone();
        while (in.length() >= 2 && (in.charAt(0) != '*' || in.charAt(1) != '/')) {
            if (in.charAt(0) == '/' && in.charAt(1) == '*') {
                in.skip(2);
                sb.append(fetchMLComment(in));
            } else {
                sb.append(in.charAt(0));
                in.skip(1);
            }
        }
        if( in.length() < 2){
            throw new BadSyntax(pos, "Unclosed block comment");
        }
        sb.append("*/");
        if (!in.isEmpty()) {
            in.skip(1);
        }
        if (!in.isEmpty()) {
            in.skip(1);
        }
        return sb.toString();
    }

    /**
     * Skip the comment until the end of the line. The end of the line is not skipped.
     *
     * @param in the input
     */
    private static String fetchComment(final Input in) {
        final var sb = new StringBuilder("//");
        while (!in.isEmpty() && in.charAt(0) != '\n') {
            sb.append(in.charAt(0));
            in.skip(1);
        }
        return sb.toString();
    }
}
