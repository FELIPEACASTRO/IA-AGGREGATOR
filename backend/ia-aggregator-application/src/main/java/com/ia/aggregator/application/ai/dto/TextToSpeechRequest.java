package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for text-to-speech synthesis.
 *
 * @param text the text to synthesize into speech (required)
 * @param model preferred TTS model (optional)
 * @param voice voice identifier (e.g., "alloy", "echo", "shimmer") (optional)
 * @param outputFormat audio output format (e.g., "mp3", "pcm", "opus") (optional, default "mp3")
 * @param speed speech speed multiplier [0.25, 4.0] (optional, default 1.0)
 */
public record TextToSpeechRequest(
        @NotBlank(message = "text is required")
        String text,
        String model,
        String voice,
        String outputFormat,
        Double speed
) {
    /** Convenience constructor for simple synthesis. */
    public TextToSpeechRequest(String text, String voice) {
        this(text, null, voice, null, null);
    }
}
