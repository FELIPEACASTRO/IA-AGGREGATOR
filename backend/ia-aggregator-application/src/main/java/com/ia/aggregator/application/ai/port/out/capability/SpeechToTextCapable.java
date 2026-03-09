package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.SpeechToTextRequest;
import com.ia.aggregator.application.ai.dto.TranscriptionResult;

/**
 * Capability interface for audio-to-text transcription (STT).
 * Providers implementing this accept audio bytes and return transcribed text.
 */
public interface SpeechToTextCapable {

    /**
     * Transcribes audio data to text.
     *
     * @param request the transcription request with audio bytes, format, and language
     * @return the transcription result with text and confidence metadata
     */
    TranscriptionResult transcribe(SpeechToTextRequest request);
}
