package com.ia.aggregator.domain.ai;

/**
 * Classifies AI providers into architectural groups that share common base classes,
 * auth strategies, and routing characteristics.
 *
 * <p>Big O: O(1) for all enum operations.
 */
public enum ProviderType {

    /** Providers following OpenAI /chat/completions API shape (Groq, Cerebras, DeepSeek, etc.) */
    OPENAI_COMPATIBLE,

    /** Providers with proprietary APIs: Gemini, Anthropic, Cohere, Mistral, HuggingFace, AI21 */
    NATIVE_LLM,

    /** Enterprise AI gateways: Azure OpenAI, AWS Bedrock, Cloudflare Workers AI */
    ENTERPRISE_GATEWAY,

    /** Image/video generation and editing: Stability AI, fal.ai, Replicate, BFL, Runway, Ideogram */
    MEDIA,

    /** Speech-to-text and text-to-speech: Deepgram, AssemblyAI, ElevenLabs, Gladia */
    AUDIO,

    /** Web and news search: Exa, NewsCatcher, Tavily, Jina AI */
    SEARCH,

    /** Dark web and threat intelligence: OnionSearch, DarkOwl, Twingly, FullHunt, Flare */
    THREAT_INTEL
}
