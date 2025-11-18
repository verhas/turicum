package ch.turic.lsp;

import ch.turic.Input;
import ch.turic.analyzer.Keywords;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class TuriHover {
    final DocumentManager documentManager;
    final CancelChecker cancelChecker;

    public TuriHover(DocumentManager documentManager, CancelChecker cancelChecker) {
        this.documentManager = documentManager;
        this.cancelChecker = cancelChecker;
    }

    /**
     * Provides hover information for a given position in a text document.
     * Based on the document's content and the specific position, this method
     * determines the relevant identifier and its associated documentation or information,
     * if available. If no relevant identifier or documentation is found, an empty hover object
     * is returned.
     *
     * @param params the hover parameters, including the position in the document and the text document details
     * @return a Hover object containing the relevant documentation or information in a specific format
     */
    public Hover hover(HoverParams params) {
        final var position = params.getPosition();
        final var uri = params.getTextDocument().getUri();

        final var source = documentManager.getContent(uri);
        if (source == null) {
            return new Hover();
        }

        final String id = TuricUtils.getWordAtPosition(source, position, uri);
        if (id == null || id.isEmpty() || !Character.isAlphabetic(id.charAt(0))) {
            return emptyHover();
        }

        final var triplet = getHoverTriplet(source, uri, id);
        if (triplet.docComment() == null) {
            return noDocumentedHover(triplet, id);
        } else {
            return documentedHover(triplet, id, source);
        }

    }

    /**
     * Generates a Hover object with markdown-formatted content based on the provided input data.
     * The hover content is constructed using the identifier type, documentation comments, and source context.
     *
     * @param result a Triple object containing the documentation comment, identifier type, and line number
     * @param id the identifier associated with the hover content
     * @param source the source code text containing the relevant context
     * @return a Hover object containing the formatted documentation or source information in markdown format
     */
    private static Hover documentedHover(Triple result, String id, String source) {
        final var mc = new MarkupContent();
        final var sb = new StringBuilder(result.idType() == null ? "" : "**" + result.idType() + "** : " + id + "\n\n");
        final var docLines = result.docComment().text().split("\n");
        final var sourceLines = source.split("\n");
        if (result.line() >= 0 && result.line() < sourceLines.length) {
            var sourceLine = sourceLines[result.line()];
            if (sourceLine.endsWith("{")) {
                sourceLine = sourceLine.substring(0, sourceLine.length() - 1);
            }
            sb.append("**").append(sourceLine.trim()).append("**\n");
        }
        for (final var docLine : docLines) {
            var trimmed = docLine.trim();
            trimmed = chopOffCommentEnd(trimmed);
            if (trimmed.startsWith("/*")) {
                trimmed = chopOffCommentStart(trimmed);
                if (isSkipEmptyFirstLine(trimmed)) {
                    continue;
                }
            } else if (trimmed.startsWith("*")) {
                trimmed = chopLeadingCommentStar(trimmed);
            }
            sb.append(trimmed).append("\n");
        }

        Hover hover = new Hover();
        mc.setKind(MarkupKind.MARKDOWN);
        mc.setValue(sb.toString());
        hover.setContents(mc);
        return hover;
    }

    /**
     * Removes the leading star (*) character from a given trimmed comment line.
     * This method assumes the input string starts with a '*' character
     * and performs the removal by taking a substring starting from the second character.
     *
     * @param trimmed the input string, typically a trimmed line of a comment, starting with '*'
     * @return the input string with the leading '*' character removed
     */
    private static String chopLeadingCommentStar(String trimmed) {
        return trimmed.substring(1);
    }

    /**
     * Determines if the first line should be skipped based on whether it is empty or contains only whitespace.
     *
     * @param trimmed the string representing the first line, which has been trimmed of leading and trailing whitespace
     * @return true if the first line is empty or contains only whitespace; false*/
    private static boolean isSkipEmptyFirstLine(String trimmed) {
        return trimmed.isBlank();
    }

    /**
     * Removes the comment starting token from a given trimmed comment line.
     * If the line starts with "/**", the first three characters are removed.
     * Otherwise, the first two characters are removed.
     *
     * @param trimmed the input string, expected to be a trimmed comment line starting with "/**" or "/*"
     * @return the input string with the comment starting token removed
     */
    private static String chopOffCommentStart(String trimmed) {
        if (trimmed.startsWith("/**")) {
            return trimmed.substring(3);
        }else{
            return trimmed.substring(2);
        }
    }

    /**
     * Removes the ending token of a comment from a given trimmed comment line.
     * If the line ends with {@code *}{@code /} it get removed.
     * Otherwise, the line is returned unchanged.
     *
     * @param trimmed the input string, expected to be a trimmed comment line ending with {@code *}{@code /}
     *
     */
    private static String chopOffCommentEnd(String trimmed) {
        if (trimmed.endsWith("*/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        return trimmed;
    }

    /**
     * Generates a Hover object containing markdown-formatted content based on the provided result and identifier.
     * If the identifier type in the provided result is null, the hover will contain an empty string as its content.
     * Otherwise, the hover will include the identifier type in bold followed by the identifier.
     *
     * @param result a Triple containing metadata including the identifier type, documentation comment, and line number
     * @param id the identifier associated with the hover content
     * @return a Hover object with markdown-formatted content based on the provided identifier and result information
     */
    private static Hover noDocumentedHover(Triple result, String id) {
        final var mc = new MarkupContent();
        mc.setKind(MarkupKind.MARKDOWN);
        mc.setValue(result.idType() == null ? "" : "**" + result.idType() + "** " + id + "\n\n");
        Hover hover = new Hover();
        hover.setContents(mc);
        return hover;
    }


    /**
     * Extracts and constructs a {@code Triple} object containing documentation, identifier type,
     * and line number information based on the source text, URI, and identifier.
     * This method processes tokens from the given source to find relevant documentation comments
     * and identifier metadata associated with the provided identifier.
     *
     * @param source the source code as a string to be analyzed
     * @param uri    the URI of the source code, used to identify the context
     * @param id     the identifier to search for within the source
     * @return a {@code Triple} object containing the documentation comment, identifier type,
     * and the line number associated with the specified identifier
     */
    private static Triple getHoverTriplet(String source, String uri, String id) {
        final var tokens = Lexer.try_analyze(Input.fromString(source, uri));
        var t = new Triple(null, null, -1);
        while (tokens.hasNext()) {
            final var lex = tokens.next();
            if (lex.type() == Lex.Type.COMMENT && lex.text().startsWith("/**")) {
                t = t.doc(lex);
                continue;
            }
            if (lex.type() == Lex.Type.IDENTIFIER && lex.text().equals(id)) {
                t = t.line(lex.startPosition().line - 1);
                break;
            } else {
                if (lex.type() != Lex.Type.SPACES || !lex.text().isBlank()) {
                    t = switch (lex.text()) {
                        case Keywords.CLASS -> t.idType("class");
                        case Keywords.FN -> t.idType("fn");
                        case Keywords.LET -> t.idType("let");
                        case Keywords.MUT -> t.idType("mut");
                        case Keywords.GLOBAL -> t.idType("global");
                        default -> t.doc(null).idType(null);
                    };
                }
            }
        }
        return t;
    }

    /**
     * Represents a record structure containing a documentation comment, identifier type, and line number.
     * This is a utility structure used to encapsulate relevant metadata derived from source analysis.
     */
    private record Triple(Lex docComment, String idType, int line) {
        /**
         * Creates a new instance of the Triple record with a specified documentation comment.
         *
         * @param docComment the documentation comment to be associated with the Triple instance
         * @return a new Triple instance containing the given documentation comment, retaining the current identifier type and line number
         */
        public Triple doc(Lex docComment) {
            return new Triple(docComment, idType, line);
        }

        /**
         * Creates a new instance of the Triple record with the specified identifier type.
         *
         * @param idType the identifier type to be associated with the Triple instance
         * @return a new Triple instance containing the given identifier type, retaining the current documentation comment and line number
         */
        public Triple idType(String idType) {
            return new Triple(docComment, idType, line);
        }

        /**
         * Creates a new instance of the Triple record with the specified line number.
         *
         * @param line the line number to associate with the Triple instance
         * @return a new Triple instance containing the given line number, retaining the current documentation comment and identifier type
         */
        public Triple line(int line) {
            return new Triple(docComment, idType, line);
        }
    }

    /**
     * Creates and returns an empty Hover object with plain text content.
     * The Hover object contains a MarkupContent instance set to an empty string
     * and uses the MarkupKind.PLAINTEXT format.
     * This method is typically used to return a non-alphabetic or default hover object
     * when no relevant hover information is available.
     *
     * @return a Hover object containing plain text content with an empty value.
     */
    private static Hover emptyHover() {
        final var mc = new MarkupContent();
        mc.setKind(MarkupKind.PLAINTEXT);
        mc.setValue("");
        final var hover = new Hover();
        hover.setContents(mc);
        return hover;
    }
}