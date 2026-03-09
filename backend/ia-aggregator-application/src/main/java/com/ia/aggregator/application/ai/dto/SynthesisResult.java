package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for text-to-speech synthesis.
 *
 * @param audioData synthesized audio as raw bytes
 * @param audioFormat format of the audio data (e.g., "mp3", "pcm", "opus")
 * @param modelUsed actual model used
 * @param providerUsed provider that synthesized the audio
 * @param durationSeconds estimated audio duration in seconds (optional)
 */
public record SynthesisResult(
        byte[] audioData,
        String audioFormat,
        String modelUsed,
        String providerUsed,
        Double durationSeconds
) {
}
