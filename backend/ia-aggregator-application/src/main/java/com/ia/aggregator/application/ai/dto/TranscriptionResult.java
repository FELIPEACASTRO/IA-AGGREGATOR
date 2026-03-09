package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for speech-to-text transcription.
 *
 * @param text the transcribed text
 * @param language detected or specified language
 * @param durationSeconds audio duration in seconds
 * @param modelUsed actual model used
 * @param providerUsed provider that transcribed the audio
 * @param words word-level timestamps (optional, null if not requested)
 * @param confidence overall transcription confidence [0.0, 1.0] (optional)
 */
public record TranscriptionResult(
        String text,
        String language,
        Double durationSeconds,
        String modelUsed,
        String providerUsed,
        List<WordTimestamp> words,
        Double confidence
) {
    /** Convenience constructor without timestamps. */
    public TranscriptionResult(String text, String language, String modelUsed, String providerUsed) {
        this(text, language, null, modelUsed, providerUsed, null, null);
    }

    /**
     * Word-level timestamp data.
     *
     * @param word the word text
     * @param startSeconds word start time in seconds
     * @param endSeconds word end time in seconds
     */
    public record WordTimestamp(String word, double startSeconds, double endSeconds) {
    }
}
