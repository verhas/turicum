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

    public List<CompletionItem> completion(CompletionParams params) {
        final var uri = params.getTextDocument().getUri();
        final var source = documentManager.getContent(uri);
        if (source == null) {
            return emptyCompletionList();
        }
        final var position = params.getPosition();
        final String startString = TuricUtils.getWordAtPosition(source, position, uri);

        final var lexes = Lexer.try_analyze(new Input(new StringBuilder(source), uri));
        final var items = matchingIdentifiers(lexes, startString, uri);

        items.addAll(keywords());

        items.addAll(languageTemplates());

        setDataForItems(items, new CompletionData(params.getTextDocument().getUri()));

        return items;
    }

    private static List<CompletionItem> languageTemplates() {
        final var items = new ArrayList<CompletionItem>();
        items.add(Snippet.item("class", CompletionItemKind.Class, "Create a new class", "class ${1:name} : ${2:parents} {\n\t$0\n}"));
        items.add(Snippet.item("init", CompletionItemKind.Constructor, "Create a new initializer", "fn init( ${1:fields}) {\n\t$0\n}"));
        items.add(Snippet.item("variable", Keywords.MUT, CompletionItemKind.Variable, "Declare a variable", "mut ${1:name} = $0;"));
        items.add(Snippet.item("constant", Keywords.LET, CompletionItemKind.Constant, "Declare a constant", "let ${1:name} = $0;"));
        items.add(Snippet.item("function", Keywords.FN, CompletionItemKind.Function, "Declare a function", "fn ${1:name}( ${2:args}) {\n\t$0\n}"));
        items.add(Snippet.item("if", Keywords.IF, CompletionItemKind.Function, "if blocks", "if ${1:expression} {\n    ${2:block1}\n}else{\n   ${3:block2}\n}"));
        items.add(Snippet.item("if", Keywords.IF, CompletionItemKind.Function, "if statement", "if ${1:expression}: ${2:statement1} else: ${3:statement2};"));
        items.add(Snippet.item("try", Keywords.TRY, CompletionItemKind.Function, "try block", "try{\n    ${1:block}\n}catch ${2:exception}{\n    ${2:block_catch}\n}finally{\n    ${3:block_finally}}"));
        items.add(Snippet.item("try", Keywords.TRY, CompletionItemKind.Function, "try statement", "try: ${1:block} catch ${2:exception}: ${2:catcher};"));
        items.add(Snippet.item("formatting switch", "format", CompletionItemKind.Function, "format:on/off", "format:${1|on,off|}"));
        return items;
    }

    private static void setDataForItems(List<CompletionItem> items, final CompletionData completionData) {
        for (final var item : items) {
            item.setData(completionData);
        }
    }

    /**
     * Creates a list of `CompletionItem` objects for each reserved keyword
     * in the lexer. Each completion item is configured with its type ({@code Keyword}), detail (empty string),
     * insertion text (the keyword itself).
     *
     * @return A list of `CompletionItem` objects representing reserved keywords.
     */
    private static List<CompletionItem> keywords() {
        final var items = new ArrayList<CompletionItem>();
        CompletionItem item;
        for (final var keyword : Lexer.RESERVED) {
            item = new CompletionItem(keyword);
            item.setKind(CompletionItemKind.Keyword);
            item.setDetail("");
            item.setInsertText(keyword);
            item.setInsertTextFormat(InsertTextFormat.Snippet);
            items.add(item);
        }
        return items;
    }

    private static List<CompletionItem> emptyCompletionList() {
        return List.of(new CompletionItem());
    }

    /**
     * Adds matching identifiers from the provided lexer to a list of completion items.
     * Identifiers are filtered based on whether they start with the given string
     * and have not been added before.
     *
     * @param lexer       A LexList object containing the tokens to be processed for identifiers.
     * @param startString The initial string to match against the identifier's text.
     * @param uri         The URI of the document being processed, used for additional metadata
     *                    in the completion items.
     * @return A list of CompletionItem objects representing the matching identifiers.
     */
    private static List<CompletionItem> matchingIdentifiers(LexList lexer, String startString, String uri) {
        final var items = new ArrayList<CompletionItem>();
        final var identifiers = new HashSet<String>();
        Lex tokenBeforeTheIdentifier = null;
        while (lexer.hasNext()) {
            final var lex = lexer.next();
            if (lex.type() == Lex.Type.IDENTIFIER && lex.text().startsWith(startString)) {
                if (!identifiers.contains(lex.text())) {
                    items.add(itemForNewlyFoundIdentifier(uri, tokenBeforeTheIdentifier, identifiers, lex.text()));
                }
            }
            tokenBeforeTheIdentifier = lex;
        }
        return items;
    }

    /**
     * Creates a `CompletionItem` for a newly found identifier, determines its kind based on the
     * preceding token, and sets various attributes of the completion item.
     *
     * @param uri                      The URI of the document being processed, used to associate additional metadata.
     * @param tokenBeforeTheIdentifier The token immediately preceding the identifier, used to determine the kind of the item.
     * @param identifiers              A set of already processed identifier strings, to which the new identifier will be added.
     * @param text                     The text of the newly found identifier.
     * @return A `CompletionItem` object representing the newly found identifier.
     */
    private static CompletionItem itemForNewlyFoundIdentifier(final String uri,
                                                              final Lex tokenBeforeTheIdentifier,
                                                              final HashSet<String> identifiers,
                                                              final String text) {
        final var kind = tokenBeforeTheIdentifier == null || tokenBeforeTheIdentifier.type() != Lex.Type.RESERVED
                ?
                CompletionItemKind.Text
                :
                switch (tokenBeforeTheIdentifier.text()) {
                    case Keywords.LET, Keywords.MUT, Keywords.FOR, Keywords.GLOBAL, Keywords.PIN
                            -> CompletionItemKind.Variable;
                    case Keywords.FN -> CompletionItemKind.Function;
                    case Keywords.CLASS -> CompletionItemKind.Class;
                    default -> CompletionItemKind.Text;
                };
        identifiers.add(text);
        final var item = new CompletionItem(text);
        item.setKind(kind);
        if (kind == CompletionItemKind.Function || kind == CompletionItemKind.Class) {
            item.setInsertText(text + "( )");
        } else {
            item.setInsertText(text);
        }
        item.setSortText(text);
        item.setFilterText(text);
        item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, "identifier"));
        item.setData(new CompletionData(uri));
        return item;
    }


}
