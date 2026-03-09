package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.TextToSpeechRequest;
import com.ia.aggregator.application.ai.dto.SynthesisResult;

/**
 * Capability interface for text-to-audio speech synthesis (TTS).
 * Providers implementing this convert text into audio bytes.
 */
public interface TextToSpeechCapable {

    /**
     * Synthesizes speech audio from text.
     *
     * @param request the synthesis request with text, voice, and audio format
     * @return the synthesis result with audio bytes and metadata
     */
    SynthesisResult synthesize(TextToSpeechRequest request);
}
