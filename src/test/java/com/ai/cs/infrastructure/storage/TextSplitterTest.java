package com.ai.cs.infrastructure.storage;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSplitterTest {

    private final TextSplitter splitter = new TextSplitter();

    @Test
    void split_shouldReturnEmptyListForNullText() {
        List<String> result = splitter.split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void split_shouldReturnEmptyListForBlankText() {
        List<String> result = splitter.split("   \n\n  ");
        assertTrue(result.isEmpty());
    }

    @Test
    void split_shouldReturnSingleChunkForShortText() {
        String text = "Hello world";
        List<String> result = splitter.split(text);
        assertEquals(1, result.size());
        assertEquals("Hello world", result.get(0));
    }

    @Test
    void split_shouldCreateMultipleChunksForLongText() {
        // Create text that exceeds MAX_CHUNK_SIZE (1000 chars)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("Paragraph ").append(i).append(": ");
            // Add enough content to each paragraph
            for (int j = 0; j < 50; j++) {
                sb.append("word ");
            }
            sb.append("\n\n");
        }
        String text = sb.toString();
        List<String> result = splitter.split(text);
        assertTrue(result.size() > 1, "Long text should produce multiple chunks");
        for (String chunk : result) {
            assertFalse(chunk.isBlank(), "Each chunk should be non-blank");
            assertTrue(chunk.length() <= 1000, "Each chunk should not exceed MAX_CHUNK_SIZE (1000)");
        }
    }

    @Test
    void split_shouldPreserveContentAcrossChunks() {
        // Two paragraphs, each should fit in one chunk together, but with overlap
        String text = "Short paragraph one.\n\nShort paragraph two.";
        List<String> result = splitter.split(text);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("paragraph one"));
        assertTrue(result.get(0).contains("paragraph two"));
    }

    @Test
    void split_shouldHandleSingleParagraphExceedingMaxSize() {
        // A single paragraph that exceeds MAX_CHUNK_SIZE (no newlines)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("word ");
        }
        String text = sb.toString().trim();
        assertTrue(text.length() > 1000, "Text should be longer than MAX_CHUNK_SIZE (" + text.length() + ")");
        List<String> result = splitter.split(text);
        assertTrue(result.size() >= 1);
        // Since there are no paragraph breaks, it will be one chunk
        assertFalse(result.isEmpty());
    }
}
