package com.ia.aggregator.application.artifact.port.out;

/**
 * Port for generating PDF documents from content.
 */
public interface PdfExportPort {

    /**
     * Generates a PDF from the given title and body content.
     *
     * @param title   document title
     * @param content body content (plain text or markdown)
     * @return PDF file bytes
     */
    byte[] generate(String title, String content);
}
