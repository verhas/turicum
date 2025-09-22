package ch.turic.lsp;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.Keywords;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Text Document Service - handles document-related operations

class TuriTextDocumentService implements TextDocumentService {
    final DocumentManager documentManager = new DocumentManager();
    private LanguageClient client;
    private TuriSyntaxErrorReporter errorReporter;


    public void connect(LanguageClient client) {
        this.client = client;
        errorReporter = new TuriSyntaxErrorReporter(client, documentManager);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        // Called when a document is opened
        String uri = params.getTextDocument().getUri();
        String content = params.getTextDocument().getText();
        documentManager.put(uri, content);
        errorReporter.analyzeAndReportErrors(uri);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
            // Apply incremental or full changes
            documentManager.applyChange(uri, change);
        }
        errorReporter.analyzeAndReportErrors(uri);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documentManager.remove(uri);
        errorReporter.clearDiagnostics(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        errorReporter.analyzeAndReportErrors(uri);
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        // This method is called when the client needs additional details about a completion item
        // that was previously returned from the completion method
        try {
            // Get the completion kind to determine how to resolve it
            CompletionItemKind kind = unresolved.getKind();
            String label = unresolved.getLabel();

            // Resolve based on the type of the completion item
            switch (kind) {
                case Function:
                case Method:
                    resolveFunctionDetails(unresolved);
                    break;

                case Variable:
                case Field:
                    resolveVariableDetails(unresolved);
                    break;

                case Class:
                case Interface:
                    resolveTypeDetails(unresolved);
                    break;

                case Keyword:
                    resolveKeywordDetails(unresolved);
                    break;

                case Snippet:
                    resolveSnippetDetails(unresolved);
                    break;

                default:
                    // For other types, just add basic documentation if not present
                    if (unresolved.getDocumentation() == null) {
                        unresolved.setDocumentation(new MarkupContent(
                                MarkupKind.MARKDOWN,
                                "Additional information for: `" + label + "`"
                        ));
                    }
                    break;
            }

            return CompletableFuture.completedFuture(unresolved);

        } catch (Exception e) {
            // If resolution fails, return the item as-is
            return CompletableFuture.completedFuture(unresolved);
        }
    }

    /**
     * Resolve details for function/method completions
     */
    private void resolveFunctionDetails(CompletionItem item) {
        String functionName = item.getLabel();
        final var cData = (CompletionData) item.getData();
        final var source = documentManager.getContent(cData.uri());
        Map<String, FunctionInfo> functions = getFunctionDatabase(source, functionName);
        FunctionInfo info = functions.get(functionName);

        if (info != null) {
            item.setDetail(info.signature);
            MarkupContent docs = new MarkupContent();
            docs.setKind(MarkupKind.MARKDOWN);

            docs.setValue("**" + functionName + info.signature + "**\n\n");
            item.setDocumentation(docs);
        }
    }

    /**
     * Resolve details for variable/field completions
     */
    private void resolveVariableDetails(CompletionItem item) {
        String varName = item.getLabel();

        // Look up variable information
        VariableInfo varInfo = getVariableInfo(varName);

        if (varInfo != null) {
            // Set type information
            item.setDetail(varInfo.type);

            // Set documentation
            MarkupContent docs = new MarkupContent();
            docs.setKind(MarkupKind.MARKDOWN);

            StringBuilder docBuilder = new StringBuilder();
            docBuilder.append("**").append(varName).append("** : `").append(varInfo.type).append("`\n\n");
            docBuilder.append(varInfo.description);

            if (varInfo.scope != null) {
                docBuilder.append("\n\n*Scope: ").append(varInfo.scope).append("*");
            }

            docs.setValue(docBuilder.toString());
            item.setDocumentation(docs);
        }
    }

    /**
     * Resolve details for class/interface completions
     */
    private void resolveTypeDetails(CompletionItem item) {
        String typeName = item.getLabel();

        TypeInfo typeInfo = getTypeInfo(typeName);

        if (typeInfo != null) {
            item.setDetail(typeInfo.kind + " " + typeName);

            MarkupContent docs = new MarkupContent();
            docs.setKind(MarkupKind.MARKDOWN);

            StringBuilder docBuilder = new StringBuilder();
            docBuilder.append("**").append(typeInfo.kind).append(" ").append(typeName).append("**\n\n");
            docBuilder.append(typeInfo.description);

            if (typeInfo.methods != null && !typeInfo.methods.isEmpty()) {
                docBuilder.append("\n\n**Methods:**\n");
                for (String method : typeInfo.methods) {
                    docBuilder.append("- `").append(method).append("`\n");
                }
            }

            docs.setValue(docBuilder.toString());
            item.setDocumentation(docs);
        }
    }

    /**
     * Resolve details for keyword completions
     */
    private void resolveKeywordDetails(CompletionItem item) {
        String keyword = item.getLabel();

        Map<String, String> keywordDocs = getKeywordDocumentation();
        String documentation = keywordDocs.get(keyword);

        if (documentation != null) {
            item.setDetail("Keyword");

            MarkupContent docs = new MarkupContent();
            docs.setKind(MarkupKind.MARKDOWN);
            docs.setValue("**" + keyword + "** (keyword)\n\n" + documentation);
            item.setDocumentation(docs);
        }
    }

    /**
     * Resolve details for snippet completions
     */
    private void resolveSnippetDetails(CompletionItem item) {
        String snippetName = item.getLabel();

        // Add detailed documentation for snippets
        MarkupContent docs = new MarkupContent();
        docs.setKind(MarkupKind.MARKDOWN);

        StringBuilder docBuilder = new StringBuilder();
        docBuilder.append("**").append(snippetName).append("** (snippet)\n\n");
        docBuilder.append("```\n").append(item.getInsertText()).append("\n```\n\n");
        docBuilder.append("Code snippet that expands to the above template.");

        docs.setValue(docBuilder.toString());
        item.setDocumentation(docs);
    }

    // Helper classes for storing completion information
    private static class FunctionInfo {
        String signature;

    }

    private static class VariableInfo {
        String type;
        String description;
        String scope;

    }

    private static class TypeInfo {
        String kind; // "class", "interface", etc.
        String description;
        java.util.List<String> methods;

    }
    // Mock data methods - replace these with real data from your language

    private Map<String, FunctionInfo> getFunctionDatabase(String source, String functionName) {
        Map<String, FunctionInfo> functions = new HashMap<>();
        try {
            final var lexer = Lexer.try_analyze(new Input(new StringBuilder(source), ""));
            Lex prior = null;
            if (lexer.hasNext()) {
                prior = lexer.next();
            }
            while (lexer.hasNext()) {
                final var lex = lexer.next();
                if (prior.is("fn") && lex.type() == Lex.Type.IDENTIFIER && lex.text().equals(functionName)) {
                    FunctionInfo func = new FunctionInfo();
                    final var sb = new StringBuilder();
                    while (lexer.hasNext() && lexer.isNot("=", "{")) {
                        sb.append(lexer.next().text());
                    }
                    func.signature = sb.toString();
                    functions.put(functionName, func);
                    break;
                }
                prior = lex;
            }
        } catch (Exception ignore) {
        }
        // Example function

        // Add more functions...

        return functions;
    }

    private VariableInfo getVariableInfo(String varName) {
        // Mock implementation - look up variable from symbol table
        VariableInfo info = new VariableInfo();
        info.type = "string"; // Example
        info.description = "A variable of type string";
        info.scope = "local";
        return info;
    }

    private TypeInfo getTypeInfo(String typeName) {
        // Mock implementation - look up type information
        TypeInfo info = new TypeInfo();
        info.kind = "class";
        info.description = "A user-defined class";
        info.methods = java.util.Arrays.asList("toString()", "equals(obj)");
        return info;
    }

    private Map<String, String> getKeywordDocumentation() {
        Map<String, String> docs = new HashMap<>();
        docs.put("if", "Conditional statement that executes code based on a boolean condition.");
        docs.put("while", "Loop that continues executing while a condition is true.");
        docs.put("for", "Loop that iterates over a sequence or range.");
        docs.put("return", "Returns a value from a function and exits the function.");
        return docs;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            var list = new TuriCompletion(documentManager).completion_synch(params);
            return Either.<List<CompletionItem>, CompletionList>forLeft(list);
        }, TuriLanguageServer.VIRTUAL_EXECUTOR);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFutures.computeAsync(TuriLanguageServer.VIRTUAL_EXECUTOR, cancelChecker -> new TuriHover(documentManager, cancelChecker).hover_synch(params));
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        return CompletableFutures.computeAsync(TuriLanguageServer.VIRTUAL_EXECUTOR, cancelChecker -> {
            final var uri = params.getTextDocument().getUri();
            final var content = documentManager.getContent(uri);

            final var lexes = Lexer.try_analyze(new Input(new StringBuilder(content), uri));

            List<FoldingRange> ranges = new ArrayList<>();

            while (lexes.hasNext()) {
                if (cancelChecker.isCanceled()) {
                    return null;
                }
                final var lex = lexes.next();
                if (lex.type() == Lex.Type.COMMENT && lex.text().startsWith("/**")) {
                    FoldingRange range = new FoldingRange();
                    range.setStartLine(lex.startPosition().line - 1);

                    int newLines = 0;
                    final var text = lex.text().toCharArray();
                    for (final var ch : text) {
                        if (ch == '\n') {
                            newLines++;
                        }
                    }
                    range.setEndLine(lex.startPosition().line - 1 + newLines);
                    range.setKind(FoldingRangeKind.Comment);
                    ranges.add(range);
                }
            }
            return ranges;

        });
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            final var uri = params.getTextDocument().getUri();
            final var content = documentManager.getContent(uri);

            final var lexes = Lexer.try_analyze(new Input(new StringBuilder(content), uri));

            List<CodeLens> lenses = new ArrayList<>();

            while (lexes.hasNext()) {
                final var lex = lexes.next();
                if (lexes.hasNext() && lex.type() == Lex.Type.COMMENT && lex.text().startsWith("/**") && lexes.peek().is(Keywords.FN)) {
                    lexes.next();// step over the 'fn' keyword
                    if (lexes.hasNext() && lexes.peek().type() == Lex.Type.IDENTIFIER) {
                        final var fn = lexes.next();
                        CodeLens lens = new CodeLens();
                        lens.setRange(new Range(new Position(fn.startPosition().line - 1, fn.startPosition().column), new Position(fn.startPosition().line - 1, fn.startPosition().column + fn.text().length())));

                        Command command = new Command();
                        command.setTitle("📖"); // Documentation icon
                        command.setCommand("turicum.showDocumentation");
                        lens.setCommand(command);
                        lenses.add(lens);
                    }
                }
            }

            return lenses;
        });
    }


    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {

        return CompletableFutures.computeAsync(TuriLanguageServer.VIRTUAL_EXECUTOR, cancelChecker -> {
            final var uri = params.getTextDocument().getUri();
            final var content = documentManager.getContent(uri);

            final var lexes = Lexer.try_analyze(new Input(new StringBuilder(content), uri));
            List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();

            Lex prior = null;
            while (lexes.hasNext()) {
                if (cancelChecker.isCanceled()) {
                    return null;
                }
                final var lex = lexes.next();
                if (lex.type() == Lex.Type.IDENTIFIER) {
                    DocumentSymbol symbol = new DocumentSymbol();
                    symbol.setName(lex.text());
                    if (prior != null) {
                        if (prior.is(Keywords.FN)) {
                            symbol.setKind(SymbolKind.Function);
                        } else if (prior.is(Keywords.LET, Keywords.PIN)) {
                            symbol.setKind(SymbolKind.Constant);
                        } else if (prior.is(Keywords.MUT, Keywords.GLOBAL)) {
                            symbol.setKind(SymbolKind.Variable);
                        } else if (prior.is(Keywords.CLASS)) {
                            symbol.setKind(SymbolKind.Class);
                        }
                        symbol.setRange(new Range(new Position(prior.startPosition().line - 1, prior.startPosition().column), new Position(lex.startPosition().line - 1, lex.startPosition().column + 1)));
                        symbol.setSelectionRange(new Range(new Position(prior.startPosition().line - 1, prior.startPosition().column), new Position(lex.startPosition().line - 1, lex.startPosition().column + lex.text().length())));
                    }
                    symbols.add(Either.forRight(symbol));
                }
                prior = lex;
            }
            return symbols;
        });
    }

    @Override
    public CompletableFuture<DocumentDiagnosticReport> diagnostic(DocumentDiagnosticParams params) {
        final var diagnostics = new DocumentDiagnosticReport(new RelatedFullDocumentDiagnosticReport());

        // Example diagnostic analysis
        TextDocumentIdentifier document = params.getTextDocument();
        // You can implement your actual diagnostic logic here
        // For now, returning an empty list of diagnostics

        return CompletableFuture.completedFuture(diagnostics);
    }


    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        return CompletableFutures.computeAsync(TuriLanguageServer.VIRTUAL_EXECUTOR, cancelChecker -> {
            final var locations = new ArrayList<Location>();
            String uri = params.getTextDocument().getUri();
            final var source = new Input(new StringBuilder(documentManager.getContent(uri)), uri);
            final var lexes = Lexer.try_analyze(source);
            final var srcLine = params.getPosition().getLine();
            final var srcCharacter = params.getPosition().getCharacter();
            // first find the thing that we want to find
            String id = null;
            Lex prior = null;
            Lex lex = null;
            while (lexes.hasNext()) {
                lex = lexes.next();
                if (lex.type() == Lex.Type.SPACES) {
                    continue;
                }
                final var pos = lex.startPosition();
                if (lex.type() == Lex.Type.IDENTIFIER) {
                    id = lex.text();
                } else {
                    prior = lex;
                    id = null;
                }
                if (srcLine == pos.line - 1 && lex.startPosition().column - 1 <= srcCharacter && srcCharacter <= pos.column + lex.lexeme().length()) {
                    break;
                }
                prior = lex;
            }
            if (id != null) {
                lexes.setIndex(0);
                while (lexes.hasNext()) {
                    final var ref = lexes.next();
                    if (lex != ref && ref.text().equals(id)) {
                        final var pos = ref.startPosition();
                        final var location = new Location();
                        location.setUri(uri);
                        location.setRange(new Range(new Position(pos.line - 1, pos.column), new Position(pos.line - 1, pos.column + ref.lexeme().length())));
                        locations.add(location);
                    }
                }
            }
            Either<List<? extends Location>, List<? extends LocationLink>> rv = Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(locations);
            return rv;
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return CompletableFutures.computeAsync(TuriLanguageServer.VIRTUAL_EXECUTOR, cancelChecker -> {
            final var locations = new ArrayList<Location>();
            String uri = params.getTextDocument().getUri();
            final var source = new Input(new StringBuilder(documentManager.getContent(uri)), uri);
            final var lexes = Lexer.try_analyze(source);
            final var srcLine = params.getPosition().getLine();
            final var srcCharacter = params.getPosition().getCharacter();
            // first find the thing that we want to find
            String id = null;
            Lex prior = null;
            while (lexes.hasNext()) {
                final var lex = lexes.next();
                if (lex.type() == Lex.Type.SPACES) {
                    continue;
                }
                final var pos = lex.startPosition();
                if (lex.type() == Lex.Type.IDENTIFIER) {
                    id = lex.text();
                } else {
                    prior = lex;
                    id = null;
                }
                if (srcLine == pos.line - 1 && lex.startPosition().column - 1 <= srcCharacter && srcCharacter <= pos.column + lex.lexeme().length()) {
                    break;
                }
                prior = lex;
            }
            final var listUses = prior != null && prior.is(Keywords.FN, Keywords.LET, Keywords.PIN, Keywords.MUT, Keywords.GLOBAL, Keywords.CLASS);
            if (id != null) {
                lexes.setIndex(0);
                while (lexes.hasNext()) {
                    final var lex = lexes.next();
                    if (lex.text().equals(id)) {
                        final var pos = lex.startPosition();
                        final var location = new Location();
                        location.setUri(uri);
                        location.setRange(new Range(new Position(pos.line - 1, pos.column), new Position(pos.line - 1, pos.column + lex.lexeme().length())));
                        locations.add(location);
                        if (!listUses) {
                            break;
                        }
                    }
                }
            }
            return locations;
        });
    }

    private List<TextEdit> createMinimalEdits(String original, String formatted) {
        List<TextEdit> edits = new ArrayList<>();

        // Simple approach: find differences line by line
        String[] originalLines = original.split("\n");
        String[] formattedLines = formatted.split("\n");

        // Use a diff algorithm or simple comparison
        for (int i = 0; i < Math.max(originalLines.length, formattedLines.length); i++) {
            String origLine = i < originalLines.length ? originalLines[i] : "";
            String formLine = i < formattedLines.length ? formattedLines[i] : "";

            if (!origLine.equals(formLine)) {
                Position start = new Position(i, 0);
                Position end = new Position(i, origLine.length());
                Range range = new Range(start, end);

                TextEdit edit = new TextEdit(range, formLine);
                edits.add(edit);
            }
        }
        return edits;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        String uri = params.getTextDocument().getUri();
        String currentContent = documentManager.getContent(uri);
        if (currentContent == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        // Now format the current content
        String formattedContent = TuriFormatter.formatDocument(currentContent);

        // Create precise edits instead of replacing everything
        List<TextEdit> edits = createMinimalEdits(currentContent, formattedContent);

        return CompletableFuture.completedFuture(edits);
    }

    private void sendDiagnostics(TextDocumentItem document) {
        if (client == null) return;

        List<Diagnostic> diagnostics = new ArrayList<>();

        // Example diagnostic - you'd implement actual analysis here
        if (document.getText().contains("error")) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 5)));
            diagnostic.setSeverity(DiagnosticSeverity.Error);
            diagnostic.setMessage("Example error found");
            diagnostic.setSource("my-language-server");
            diagnostics.add(diagnostic);
        }

        PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        diagnosticsParams.setUri(document.getUri());
        diagnosticsParams.setDiagnostics(diagnostics);

        client.publishDiagnostics(diagnosticsParams);
    }
}
