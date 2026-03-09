package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for speech-to-text transcription.
 *
 * @param audioData raw audio bytes (required)
 * @param audioFormat format of the audio (e.g., "wav", "mp3", "flac", "ogg", "webm")
 * @param model preferred STT model (optional)
 * @param language BCP-47 language tag (e.g., "en-US", "pt-BR") (optional — auto-detect if null)
 * @param enableTimestamps whether to include word-level timestamps (optional)
 */
public record SpeechToTextRequest(
        @NotNull(message = "audio data is required")
        byte[] audioData,
        String audioFormat,
        String model,
        String language,
        Boolean enableTimestamps
) {
    /** Convenience constructor for simple transcription. */
    public SpeechToTextRequest(byte[] audioData, String audioFormat) {
        this(audioData, audioFormat, null, null, null);
    }
}
