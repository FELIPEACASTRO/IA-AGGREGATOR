package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;

/**
 * Capability interface for structured document parsing.
 * Providers implementing this can extract text, tables, and metadata from documents.
 */
public interface DocumentParsingCapable {

    /**
     * Parses a document and extracts structured content.
     *
     * @param request the document parsing request
     * @return the parsed result with content, tables, and metadata
     */
    DocumentParsingResult parseDocument(DocumentParsingRequest request);
}
