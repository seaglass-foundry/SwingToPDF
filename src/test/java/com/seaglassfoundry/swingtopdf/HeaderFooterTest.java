package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that header and footer bands appear as selectable text in the PDF.
 */
class HeaderFooterTest {

    @Test
    void header_appearsOnEveryPage(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf.pdf");

        // Tall panel forces two pages
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header(HeaderFooter.of("Quarterly Report").align(HeaderFooter.Alignment.LEFT))
                .footer(HeaderFooter.of("Page {page} of {pages}"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();

            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                assertThat(text).contains("Quarterly Report");
                assertThat(text).contains("Page " + p + " of " + doc.getNumberOfPages());
            }
        }
    }

    @Test
    void tokenSubstitution_pageAndPages() {
        HeaderFooter hf = HeaderFooter.of("Page {page} of {pages}");
        assertThat(hf.resolve(3, 10)).isEqualTo("Page 3 of 10");
    }

    @Test
    void fluent_returnsNewInstance() {
        HeaderFooter a = HeaderFooter.of("x");
        HeaderFooter b = a.fontSize(12f).color(Color.BLACK).align(HeaderFooter.Alignment.RIGHT);
        assertThat(b).isNotSameAs(a);
        assertThat(b.fontSize()).isEqualTo(12f);
        assertThat(b.alignment()).isEqualTo(HeaderFooter.Alignment.RIGHT);
    }
}
