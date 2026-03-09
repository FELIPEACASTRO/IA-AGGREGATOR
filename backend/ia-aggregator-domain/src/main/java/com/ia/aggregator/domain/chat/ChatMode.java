package com.ia.aggregator.domain.chat;

/**
 * Interaction hub chat modes.
 */
public enum ChatMode {
    /** Platform auto-selects model and tools based on context */
    AUTO,
    /** User explicitly chooses model */
    MANUAL,
    /** Same prompt sent to multiple models for side-by-side comparison */
    COMPARE,
    /** Synthesis from web, documents, and knowledge bases with citations */
    RESEARCH,
    /** Platform executes steps in connected tools or browser */
    ACTION
}
