package com.ia.aggregator.infrastructure.artifact;

import com.ia.aggregator.application.artifact.port.out.PdfExportPort;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * PDF export adapter using OpenPDF (librepdf).
 */
@Component
public class OpenPdfExportAdapter implements PdfExportPort {

    private static final Logger log = LoggerFactory.getLogger(OpenPdfExportAdapter.class);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

    @Override
    public byte[] generate(String title, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            HeaderFooter footer = new HeaderFooter(new Phrase("IA Aggregator - ", FOOTER_FONT), true);
            footer.setAlignment(HeaderFooter.ALIGN_CENTER);
            footer.setBorderWidthTop(0.5f);
            document.setFooter(footer);

            document.open();

            Paragraph titleParagraph = new Paragraph(title, TITLE_FONT);
            titleParagraph.setSpacingAfter(20f);
            document.add(titleParagraph);

            String[] paragraphs = content.split("\n\n");
            for (String para : paragraphs) {
                String trimmed = para.trim();
                if (!trimmed.isEmpty()) {
                    Paragraph bodyParagraph = new Paragraph(trimmed, BODY_FONT);
                    bodyParagraph.setSpacingAfter(10f);
                    bodyParagraph.setLeading(16f);
                    document.add(bodyParagraph);
                }
            }

            document.close();
            log.debug("PDF generated for artifact: title={}, size={} bytes", title, baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for artifact: title={}", title, e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
