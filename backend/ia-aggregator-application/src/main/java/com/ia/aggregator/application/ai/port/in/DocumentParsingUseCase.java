package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;

/**
 * Use case port for structured document parsing.
 */
public interface DocumentParsingUseCase {

    DocumentParsingResult execute(DocumentParsingRequest request);
}
