package ch.turic.lsp;

import ch.turic.Input;
import ch.turic.analyzer.Keywords;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import org.eclipse.lsp4j.*;

public class TuriHover {
    final DocumentManager documentManager;

    public TuriHover(DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    public Hover hover_synch(HoverParams params) {
        final var position = params.getPosition();
        final var uri = params.getTextDocument().getUri();
        final var source = documentManager.getContent(uri);
        if( source == null ){
            return new Hover();
        }
        final String id = TuricUtils.getWordAtPosition(source, position, uri);
        if (id == null || id.isEmpty() || !Character.isAlphabetic(id.charAt(0))) {
            final var mc = new MarkupContent();
            mc.setKind(MarkupKind.PLAINTEXT);
            mc.setValue("");
            Hover hover = new Hover();
            hover.setContents(mc);
            return hover;
        }

        final var lexes = Lexer.try_analyze(Input.fromString(source, uri));
        Lex docComment = null;
        String idType = null;
        int line = -1;
        while (lexes.hasNext()) {
            final var lex = lexes.next();
            if (lex.type() == Lex.Type.COMMENT && lex.text().startsWith("/**")) {
                docComment = lex;
                continue;
            }
            if (lex.type() == Lex.Type.IDENTIFIER && lex.text().equals(id)) {
                line = lex.position().line - 1;
                break;
            } else {
                if (lex.type() != Lex.Type.TEXT || !lex.text().isBlank()) {
                    switch (lex.text()) {
                        case Keywords.CLASS:
                            idType = "class";
                            break;
                        case Keywords.FN:
                            idType = "fn";
                            break;
                        case Keywords.LET:
                            idType = "let";
                            break;
                        case Keywords.MUT:
                            idType = "mut";
                            break;
                        case Keywords.GLOBAL:
                            idType = "global";
                            break;
                        default:
                            docComment = null;
                            idType = null;
                            break;
                    }
                }
            }
        }
        if (docComment == null) {
            final var mc = new MarkupContent();
            mc.setKind(MarkupKind.MARKDOWN);
            mc.setValue(idType == null ? "" : "**" + idType + "** " + id + "\n\n");
            Hover hover = new Hover();
            hover.setContents(mc);
            return hover;
        } else {
            final var mc = new MarkupContent();
            final var docLines = docComment.text().split("\n");
            final var sb = new StringBuilder(idType == null ? "" : "**" + idType + "** : " + id + "\n\n");
            final var sourceLines = source.split("\n");
            if( line >= 0 && line < sourceLines.length){
                var sourceLine = sourceLines[line];
                if( sourceLine.endsWith("{")){
                    sourceLine = sourceLine.substring(0, sourceLine.length() - 1);
                }
                sb.append("**").append(sourceLine.trim()).append("**\n");
            }
            for (final var docLine : docLines) {
                var t = docLine.trim();
                if (t.endsWith("*/")) {
                    t = t.substring(0, t.length() - 2);
                }
                if (t.startsWith("/**")) {
                    t = t.substring(3);
                    if (t.isBlank()) {
                        continue;
                    }
                } else if (t.startsWith("/*")) {
                    t = t.substring(2);
                    if (t.isBlank()) {
                        continue;
                    }
                } else if (t.startsWith("*")) {
                    t = t.substring(1);
                }
                sb.append(t).append("\n");
            }

            mc.setKind(MarkupKind.MARKDOWN);
            mc.setValue(sb.toString());
            Hover hover = new Hover();
            hover.setContents(mc);
            return hover;
        }

    }
}