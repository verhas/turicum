package ch.turic.lsp;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a collection of text documents identified by their URI.
 * Provides operations to store, retrieve, remove, and apply changes to document content.
 */
public class DocumentManager {
    private final Map<String, String> documents = new ConcurrentHashMap<>();

    public void put(String uri, String content) {
        documents.put(uri, content);
    }

    public void remove(String uri) {
        documents.remove(uri);
    }

    public String getContent(String uri) {
        return documents.get(uri);
    }

    public void applyChange(String uri, TextDocumentContentChangeEvent change) {
        final var currentContent = documents.get(uri);
        if (currentContent != null) {
            if (change.getRange() == null) {
                // Full document update
                documents.put(uri, change.getText());
            } else {
                // Incremental update - apply the change to the specific range
                final var updatedContent = applyIncrementalChange(currentContent, change);
                documents.put(uri, updatedContent);
            }
        }
    }

    private String applyIncrementalChange(String content, TextDocumentContentChangeEvent change) {
        Range range = change.getRange();
        Position start = range.getStart();
        Position end = range.getEnd();

        // Convert the content into lines for easier manipulation
        String[] lines = content.split("\n", -1);

        // Calculate absolute offsets
        int startOffset = getOffset(lines, start);
        int endOffset = getOffset(lines, end);

        // Apply the change
        return content.substring(0, startOffset) +
                change.getText() +
                content.substring(endOffset);
    }

    private int getOffset(String[] lines, Position position) {
        int offset = 0;
        // Add up lengths of all previous lines
        for (int i = 0; i < position.getLine(); i++) {
            offset += lines[i].length() + 1; // +1 for the newline character
        }
        // Add the characters in the current line
        offset += position.getCharacter();
        return offset;
    }

}