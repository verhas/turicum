package ch.turic.lsp;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentManagerTest {

    @Test
    void tracksVersionsAndClearsThemOnRemove() {
        final var manager = new DocumentManager();
        manager.put("file:///a.turi", "content");
        manager.setVersion("file:///a.turi", 3);
        assertEquals(3, manager.getVersion("file:///a.turi"));
        manager.setVersion("file:///a.turi", null); // null versions are ignored
        assertEquals(3, manager.getVersion("file:///a.turi"));
        manager.remove("file:///a.turi");
        assertNull(manager.getVersion("file:///a.turi"));
        assertNull(manager.getContent("file:///a.turi"));
    }

    @Test
    void appliesFullAndIncrementalChanges() {
        final var manager = new DocumentManager();
        manager.put("u", "hello world");

        final var full = new TextDocumentContentChangeEvent("bye");
        manager.applyChange("u", full);
        assertEquals("bye", manager.getContent("u"));

        manager.put("u", "aa bb\ncc dd");
        final var incremental = new TextDocumentContentChangeEvent(
                new Range(new Position(1, 0), new Position(1, 2)), "XX");
        manager.applyChange("u", incremental);
        assertEquals("aa bb\nXX dd", manager.getContent("u"));
    }

    @Test
    void getWordAtPositionIsBoundsSafe() {
        // stale positions beyond the document must not throw
        assertEquals("", TuricUtils.getFraction("one line", new Position(5, 0)));
    }
}
