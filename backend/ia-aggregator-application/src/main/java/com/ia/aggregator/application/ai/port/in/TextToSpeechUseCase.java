package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.TextToSpeechRequest;
import com.ia.aggregator.application.ai.dto.SynthesisResult;

/**
 * Input port for text-to-speech synthesis.
 * Converts text into audio data.
 */
public interface TextToSpeechUseCase {
    SynthesisResult execute(TextToSpeechRequest request);
}
