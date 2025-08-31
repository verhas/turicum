package ch.turic.lsp;

import ch.turic.analyzer.*;
import org.eclipse.lsp4j.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TuriCompletion {

    private final DocumentManager documentManager;

    public TuriCompletion(DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    private record Snippet(String label, String filter, CompletionItemKind kind, String detail, String insertText) {
        Snippet(String label, CompletionItemKind kind, String detail, String insertText) {
            this(label, label, kind, detail, insertText);
        }

        CompletionItem toItem() {
            final var item = new CompletionItem(label);
            item.setFilterText(filter);
            item.setKind(kind);
            item.setDetail(detail);
            item.setInsertText(insertText);
            item.setInsertTextFormat(InsertTextFormat.Snippet);
            return item;
        }

        static CompletionItem item(String label, CompletionItemKind kind, String detail, String insertText) {
            return new Snippet(label, kind, detail, insertText).toItem();
        }

        static CompletionItem item(String label, String filter, CompletionItemKind kind, String detail, String insertText) {
            return new Snippet(label, filter, kind, detail, insertText).toItem();
        }
    }

    public List<CompletionItem> completion_synch(CompletionParams params) {
        final var uri = params.getTextDocument().getUri();
        final var source = documentManager.getContent(uri);
        final var position = params.getPosition();
        List<CompletionItem> items = new ArrayList<>();
        final String startString = getStartString(source, position, uri);

        final var lexes = Lexer.try_analyze(new Input(new StringBuilder(source), uri));
        addIdentifiers(lexes, startString, uri, items);

        // add keywords
        CompletionItem item;
        for (final var kw : Lexer.RESERVED) {
            item = new CompletionItem("keyword");
            item.setKind(CompletionItemKind.Keyword);
            item.setDetail("");
            item.setInsertText(kw);
            item.setInsertTextFormat(InsertTextFormat.Snippet);
            items.add(item);
        }


        items.add(Snippet.item("class", CompletionItemKind.Class, "Create a new class", "class ${1:name} : ${2:parents} {\n\t$0\n}"));
        items.add(Snippet.item("init", CompletionItemKind.Constructor, "Create a new initializer", "fn init( ${1:fields}) {\n\t$0\n}"));
        items.add(Snippet.item("variable", Keywords.MUT, CompletionItemKind.Variable, "Declare a variable", "mut ${1:name} = $0;"));
        items.add(Snippet.item("constant", Keywords.LET, CompletionItemKind.Constant, "Declare a constant", "let ${1:name} = $0;"));
        items.add(Snippet.item("function", Keywords.FN, CompletionItemKind.Function, "Declare a function", "fn ${1:name}( ${2:args}) {\n\t$0\n}"));
        items.add(Snippet.item("if", Keywords.IF, CompletionItemKind.Function, "if blocks", "if ${1:expression} {\n    ${2:block1}\n}else{\n   ${3:block2}\n}"));
        items.add(Snippet.item("if", Keywords.IF, CompletionItemKind.Function, "if statement", "if ${1:expression}: ${2:statement1} else: ${3:statement2};"));
        items.add(Snippet.item("try", Keywords.TRY, CompletionItemKind.Function, "try block", "try{\n    ${1:block}\n}catch ${2:exception}{\n    ${2:block_catch}\n}finally{\n    ${3:block_finally}}"));
        items.add(Snippet.item("try", Keywords.TRY, CompletionItemKind.Function, "try statement", "try: ${1:block} catch ${2:exception}: ${2:catcher;"));

        for (final var i : items) {
            i.setData(new CompletionData(params.getTextDocument().getUri()));
        }

        return items;
    }

    /**
     * Adds identifiers to the provided list of completion items by parsing the given lexer.
     * Identifiers are matched against a starting string and only added if they haven't
     * already been processed. Each identifier is added as a `CompletionItem` with specific attributes
     * set, such as its kind, text, sort text, and documentation.
     *
     * @param lexer       The lexer containing the sequence of tokens to parse for identifiers.
     * @param startString The starting string to match against token identifiers in the lexer.
     * @param uri         The URI of the document being processed, used to associate additional metadata.
     * @param items       The list to which matching identifier completion items will be added.
     */
    private static void addIdentifiers(LexList lexer, String startString, String uri, List<CompletionItem> items) {
        final var ids = new HashSet<String>();
        Lex prior = null;
        while (lexer.hasNext()) {
            final var lex = lexer.next();
            if (lex.type() == Lex.Type.IDENTIFIER && lex.text().startsWith(startString)) {
                if (!ids.contains(lex.text())) {
                    final var kind = prior == null || prior.type() != Lex.Type.RESERVED ? CompletionItemKind.Text :
                            switch (prior.text()) {
                                case Keywords.LET -> CompletionItemKind.Variable;
                                case Keywords.MUT -> CompletionItemKind.Variable;
                                case Keywords.FN -> CompletionItemKind.Function;
                                case Keywords.CLASS -> CompletionItemKind.Class;
                                case Keywords.FOR -> CompletionItemKind.Variable;
                                case Keywords.GLOBAL -> CompletionItemKind.Variable;
                                case Keywords.PIN -> CompletionItemKind.Variable;
                                default -> CompletionItemKind.Text;
                            };
                    ids.add(lex.text());
                    final var item = new CompletionItem(lex.text());
                    item.setKind(kind);
                    if (kind == CompletionItemKind.Function || kind == CompletionItemKind.Class) {
                        item.setInsertText(lex.text() + "( )");
                    } else {
                        item.setInsertText(lex.text());
                    }
                    item.setSortText(lex.text());
                    item.setFilterText(lex.text());
                    item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, "identifier"));
                    item.setData(new CompletionData(uri));
                    items.add(item);
                }
            }
            prior = lex;
        }
    }

    /**
     * Retrieves a specific substring from the given source text based on the provided position
     * and additional logic applied to the extracted fraction of text.
     * The returned string represents a parsed identifier, single character, or similar substring
     * derived from the fraction of the line.
     *
     * @param source   The complete source text, where a newline character separates each line.
     * @param position The position object indicating the line index and character index in the source text.
     * @param uri      The URI of the file or document being processed, used to instantiate the input.
     * @return A string representing the initial substring derived from the fraction of the line starting
     * at the specified position. The string could be an identifier, a single character,
     * or other relevant substring based on the logic applied.
     */
    private static String getStartString(String source, Position position, String uri) {
        String fraction = getFraction(source, position);
        if (fraction.isEmpty()) {
            return "";
        }
        final var input = new Input(new StringBuilder(fraction), uri);
        final String startString;
        if (input.charAt(0) == '`') {
            startString = StringFetcher.fetchId(input);
        } else if (input.charAt(0) == '_' || Character.isAlphabetic(input.charAt(0))) {
            startString = input.fetchId();
        } else {
            startString = input.substring(0, 1);
        }
        return startString;
    }

    /**
     * Extracts a specific fraction of a line from the source text based on the given position.
     * The fraction typically corresponds to an identifier or a substring starting from a point
     * in the line to the end of the line.
     *
     * @param source   The complete source text, where a newline character separates each line.
     * @param position The position object indicating the line index and character index in the source text.
     * @return A string representing the fraction of the line starting from the computed position.
     * Returns an empty string if the line is empty or the position results in an invalid calculation.
     */
    private static String getFraction(String source, Position position) {
        final var lines = source.split("\n", -1);
        final var line = lines[position.getLine()];
        if (!line.isEmpty()) {
            // find the start of the identifier if we are standing on something that looks like an id
            int j = position.getCharacter();
            if (j >= line.length()) {
                j--;
            }
            while (j >= 0 && (line.charAt(j) == '_' || Character.isAlphabetic(line.charAt(j)) || Character.isDigit(line.charAt(j)))) {
                j--;
            }
            j++;// step back on the first character
            return line.substring(j);
        } else {
            return "";
        }
    }


}
