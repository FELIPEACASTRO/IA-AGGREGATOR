package com.ia.aggregator.infrastructure.knowledge;

import com.ia.aggregator.application.knowledge.port.out.DocumentChunkerPort;
import com.ia.aggregator.domain.knowledge.ChunkingStrategy;
import com.ia.aggregator.domain.knowledge.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Document chunker supporting multiple strategies.
 */
@Component
public class FixedSizeDocumentChunker implements DocumentChunkerPort {

    @Override
    public List<DocumentChunk> chunk(UUID documentId, String content, ChunkingStrategy strategy,
                                      int chunkSize, int overlap) {
        return switch (strategy) {
            case FIXED_SIZE -> chunkFixedSize(documentId, content, chunkSize, overlap);
            case SENTENCE -> chunkBySentence(documentId, content, chunkSize);
            case MARKDOWN_HEADER -> chunkByMarkdownHeader(documentId, content);
            case SEMANTIC -> chunkBySentence(documentId, content, chunkSize); // Simplified
            case NONE -> List.of(new DocumentChunk(UUID.randomUUID(), documentId, 0,
                    content, estimateTokens(content), null, Map.of()));
        };
    }

    private List<DocumentChunk> chunkFixedSize(UUID documentId, String content, int chunkSize, int overlap) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] words = content.split("\\s+");
        int wordsPerChunk = chunkSize; // Approximate: 1 token ≈ 1 word (simplified)
        int overlapWords = overlap;

        int start = 0;
        int index = 0;
        while (start < words.length) {
            int end = Math.min(start + wordsPerChunk, words.length);
            String chunkText = String.join(" ", Arrays.copyOfRange(words, start, end));

            chunks.add(new DocumentChunk(UUID.randomUUID(), documentId, index,
                    chunkText, estimateTokens(chunkText), null, Map.of("start_word", String.valueOf(start))));

            start = end - overlapWords;
            if (start >= end) break;
            index++;
        }
        return chunks;
    }

    private List<DocumentChunk> chunkBySentence(UUID documentId, String content, int maxTokens) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] sentences = content.split("(?<=[.!?])\\s+");

        StringBuilder current = new StringBuilder();
        int index = 0;

        for (String sentence : sentences) {
            if (estimateTokens(current.toString()) + estimateTokens(sentence) > maxTokens && !current.isEmpty()) {
                chunks.add(new DocumentChunk(UUID.randomUUID(), documentId, index,
                        current.toString().trim(), estimateTokens(current.toString()), null, Map.of()));
                current = new StringBuilder();
                index++;
            }
            current.append(sentence).append(" ");
        }

        if (!current.isEmpty()) {
            chunks.add(new DocumentChunk(UUID.randomUUID(), documentId, index,
                    current.toString().trim(), estimateTokens(current.toString()), null, Map.of()));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkByMarkdownHeader(UUID documentId, String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] sections = content.split("(?=^#{1,3}\\s)", -1);

        int index = 0;
        for (String section : sections) {
            if (!section.isBlank()) {
                String title = section.lines().findFirst().orElse("").replaceAll("^#+\\s*", "");
                chunks.add(new DocumentChunk(UUID.randomUUID(), documentId, index,
                        section.trim(), estimateTokens(section),
                        null, Map.of("section_title", title)));
                index++;
            }
        }
        return chunks;
    }

    private int estimateTokens(String text) {
        // Rough estimate: ~4 chars per token
        return text.length() / 4;
    }
}
