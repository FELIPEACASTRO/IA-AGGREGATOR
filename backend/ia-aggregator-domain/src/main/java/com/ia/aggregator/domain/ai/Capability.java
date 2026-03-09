package com.ia.aggregator.domain.ai;

/**
 * Defines the capabilities that AI providers can support.
 * Each capability represents a distinct type of AI operation.
 *
 * <p>Design: Strategy Pattern — routing decisions are based on capability type.
 * Time complexity: O(1) for capability lookup via EnumSet.
 */
public enum Capability {

    /** Text-to-text chat completion (OpenAI /chat/completions compatible) */
    CHAT,

    /** Structured responses with tool use and multi-turn context */
    RESPONSES,

    /** Text-to-vector embedding generation */
    EMBEDDINGS,

    /** Document relevance re-ranking against a query */
    RERANK,

    /** Text-to-image generation */
    IMAGE_GENERATION,

    /** Image editing with text instructions */
    IMAGE_EDITING,

    /** Text/image-to-video generation */
    VIDEO_GENERATION,

    /** Audio-to-text transcription */
    SPEECH_TO_TEXT,

    /** Text-to-audio speech synthesis */
    TEXT_TO_SPEECH,

    /** Image-to-text optical character recognition */
    OCR,

    /** Web search with structured results */
    WEB_SEARCH,

    /** Chat grounded with real-time web search results */
    WEB_GROUNDED_CHAT,

    /** Dark web and threat intelligence search (requires compliance gate) */
    THREAT_INTEL_SEARCH,

    /** Text-to-text translation between languages */
    TRANSLATION,

    /** Structured document parsing and extraction */
    DOCUMENT_PARSING,

    /** Text/image-to-3D model generation */
    THREE_D_GENERATION,

    /** Script-driven avatar video generation */
    AVATAR_VIDEO
}
