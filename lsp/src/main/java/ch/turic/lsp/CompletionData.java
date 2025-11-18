package ch.turic.lsp;

/**
 * Represents completion data associated with a specific resource.
 * This class encapsulates the uniform resource identifier (URI)
 * for identifying the resource.
 * <p>
 * The {@code CompletionData} record is immutable and provides
 * a compact way to store data related to completions.
 */
public record CompletionData(String uri) {
}
