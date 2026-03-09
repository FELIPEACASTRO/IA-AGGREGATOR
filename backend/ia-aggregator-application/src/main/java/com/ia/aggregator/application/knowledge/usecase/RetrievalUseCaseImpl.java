package com.ia.aggregator.application.knowledge.usecase;

import com.ia.aggregator.application.knowledge.port.in.RetrievalUseCase;
import com.ia.aggregator.application.knowledge.port.out.EmbeddingPort;
import com.ia.aggregator.application.knowledge.port.out.VectorStorePort;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.domain.knowledge.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG retrieval: embed query → vector search → optionally generate response.
 */
@Service
public class RetrievalUseCaseImpl implements RetrievalUseCase {

    private static final Logger log = LoggerFactory.getLogger(RetrievalUseCaseImpl.class);

    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStore;
    private final List<AiModelProvider> chatProviders;

    @Value("${app.knowledge.embedding-model:text-embedding-3-small}")
    private String defaultEmbeddingModel;
    @Value("${app.knowledge.rag-model:gpt-4o-mini}")
    private String defaultRagModel;

    public RetrievalUseCaseImpl(EmbeddingPort embeddingPort,
                                 VectorStorePort vectorStore,
                                 List<AiModelProvider> chatProviders) {
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
        this.chatProviders = chatProviders;
    }

    @Override
    public List<RetrievalResult> retrieve(UUID collectionId, String query, int topK, double scoreThreshold) {
        float[] queryEmbedding = embeddingPort.embedQuery(query, defaultEmbeddingModel);
        return vectorStore.search(collectionId, queryEmbedding, topK, scoreThreshold);
    }

    @Override
    public RagResponse ragChat(UUID collectionId, String query, String systemPrompt, int topK) {
        // Step 1: Retrieve relevant chunks
        List<RetrievalResult> results = retrieve(collectionId, query, topK, 0.3);

        // Step 2: Build augmented prompt
        String context = results.stream()
                .map(r -> "[Source: " + r.documentTitle() + "]\n" + r.content())
                .collect(Collectors.joining("\n\n---\n\n"));

        String augmentedPrompt = (systemPrompt != null ? systemPrompt + "\n\n" : "")
                + "Use the following context to answer the user's question. "
                + "Cite sources when possible.\n\n"
                + "Context:\n" + context
                + "\n\nQuestion: " + query;

        // Step 3: Generate response using chat provider
        String response = generateChat(augmentedPrompt);

        return new RagResponse(response, results, defaultRagModel);
    }

    private String generateChat(String prompt) {
        for (AiModelProvider provider : chatProviders) {
            if (provider.supports(defaultRagModel)) {
                try {
                    return provider.generate(prompt, defaultRagModel);
                } catch (Exception e) {
                    log.warn("RAG chat failed with {}: {}", provider.providerName(), e.getMessage());
                }
            }
        }
        return "Unable to generate response — no available chat provider.";
    }
}
