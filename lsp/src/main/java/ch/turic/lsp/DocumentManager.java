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

    /**
     * Stores the provided content associated with the specified URI in the document collection.
     * If a document with the same URI already exists, its content is updated.
     *
     * @param uri the unique identifier of the document, typically a URI or path-like string
     * @param content the text content to associate with the specified URI
     */
    public void put(String uri, String content) {
        documents.put(uri, content);
    }

    /**
     * Removes the document associated with the specified URI from the document collection.
     *
     * @param uri the unique identifier of the document to be removed, typically a URI or path-like string
     */
    public void remove(String uri) {
        documents.remove(uri);
    }

    /**
     * Retrieves the content of a document associated with the specified URI.
     * If the URI is not found in the document collection, this method returns null.
     *
     * @param uri the unique identifier of the document to retrieve, typically a URI or path-like string
     * @return the content of the document as a string, or null if no document exists for the given URI
     */
    public String getContent(String uri) {
        return documents.get(uri);
    }

    public void applyChange(String uri, TextDocumentContentChangeEvent change) {
        final var currentContent = getContent(uri);
        if (currentContent != null) {
            if (change.getRange() == null) {
                // Full document update
                put(uri, change.getText());
            } else {
                // Incremental update - apply the change to the specific range
                put(uri, applyIncrementalChange(currentContent, change));
            }
        }
    }

    /**
     * Applies an incremental text change to the provided content as specified by the change event.
     * The method creates the modified content based on the range and the new text defined in the
     * TextDocumentContentChangeEvent object.
     *
     * @param content the original content of the document as a string
     * @param change the change event containing the range where the modification should occur and the new content to apply
     * @return the updated content of the document after applying the change
     */
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