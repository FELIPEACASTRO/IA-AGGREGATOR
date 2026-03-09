package com.ia.aggregator.application.knowledge.port.in;

import com.ia.aggregator.domain.knowledge.RetrievalResult;

import java.util.List;
import java.util.UUID;

/**
 * Use case for RAG retrieval — searching the knowledge base.
 */
public interface RetrievalUseCase {

    /**
     * Retrieve relevant chunks for a query.
     *
     * @param collectionId  Knowledge base collection to search
     * @param query         The search query
     * @param topK          Number of results to return
     * @param scoreThreshold Minimum similarity score (0-1)
     * @return ranked list of relevant chunks
     */
    List<RetrievalResult> retrieve(UUID collectionId, String query, int topK, double scoreThreshold);

    /**
     * RAG-augmented chat: retrieve context and generate response.
     *
     * @param collectionId  Knowledge base collection
     * @param query         User query
     * @param systemPrompt  System instructions
     * @param topK          Number of context chunks
     * @return generated response with citations
     */
    RagResponse ragChat(UUID collectionId, String query, String systemPrompt, int topK);

    record RagResponse(String answer, List<RetrievalResult> sources, String model) {}
}
