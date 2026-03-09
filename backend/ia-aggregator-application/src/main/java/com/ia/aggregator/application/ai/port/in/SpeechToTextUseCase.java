package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.SpeechToTextRequest;
import com.ia.aggregator.application.ai.dto.TranscriptionResult;

/**
 * Input port for speech-to-text transcription.
 * Converts audio data into text.
 */
public interface SpeechToTextUseCase {
    TranscriptionResult execute(SpeechToTextRequest request);
}
