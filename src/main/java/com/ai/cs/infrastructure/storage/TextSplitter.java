package com.ai.cs.infrastructure.storage;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextSplitter {
    private static final int MAX_CHUNK_SIZE = 1000;
    private static final int MIN_CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // Split by paragraphs first
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > MAX_CHUNK_SIZE && currentChunk.length() >= MIN_CHUNK_SIZE) {
                chunks.add(currentChunk.toString().trim());
                // Keep overlap
                String overlapText = currentChunk.substring(Math.max(0, currentChunk.length() - OVERLAP));
                currentChunk = new StringBuilder(overlapText);
            }
            if (currentChunk.length() > 0) currentChunk.append("\n\n");
            currentChunk.append(trimmed);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
