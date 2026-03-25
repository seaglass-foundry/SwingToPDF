package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for JTextPane vector text rendering via StyledDocument element walk.
 */
class JTextPaneHandlerTest {

    // -----------------------------------------------------------------------
    // Plain text
    // -----------------------------------------------------------------------

    @Test
    void plainText_isSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        pane.setText("Hello from JTextPane");
        Path pdf = exportPane(pane, 300, 60, tmp, "textpane_plain.pdf");

        assertPdfContains(pdf, "Hello from JTextPane");
    }

    // -----------------------------------------------------------------------
    // Bold and italic runs
    // -----------------------------------------------------------------------

    @Test
    void boldAndItalicRuns_allTextSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        StyledDocument doc = pane.getStyledDocument();

        Style bold = doc.addStyle("bold", null);
        StyleConstants.setBold(bold, true);

        Style italic = doc.addStyle("italic", null);
        StyleConstants.setItalic(italic, true);

        doc.insertString(0, "Normal ", null);
        doc.insertString(doc.getLength(), "Bold ", bold);
        doc.insertString(doc.getLength(), "Italic", italic);

        Path pdf = exportPane(pane, 300, 60, tmp, "textpane_styles.pdf");

        assertPdfContains(pdf, "Normal");
        assertPdfContains(pdf, "Bold");
        assertPdfContains(pdf, "Italic");
    }

    // -----------------------------------------------------------------------
    // Colour runs
    // -----------------------------------------------------------------------

    @Test
    void colouredRuns_textSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        StyledDocument doc = pane.getStyledDocument();

        Style red = doc.addStyle("red", null);
        StyleConstants.setForeground(red, Color.RED);

        Style blue = doc.addStyle("blue", null);
        StyleConstants.setForeground(blue, Color.BLUE);

        doc.insertString(0, "Red text ", red);
        doc.insertString(doc.getLength(), "Blue text", blue);

        Path pdf = exportPane(pane, 300, 60, tmp, "textpane_colors.pdf");

        assertPdfContains(pdf, "Red text");
        assertPdfContains(pdf, "Blue text");
    }

    // -----------------------------------------------------------------------
    // Multiple font sizes
    // -----------------------------------------------------------------------

    @Test
    void mixedFontSizes_allTextSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        StyledDocument doc = pane.getStyledDocument();

        Style big = doc.addStyle("big", null);
        StyleConstants.setFontSize(big, 20);

        Style small = doc.addStyle("small", null);
        StyleConstants.setFontSize(small, 9);

        doc.insertString(0, "BigText ", big);
        doc.insertString(doc.getLength(), "SmallText", small);

        Path pdf = exportPane(pane, 300, 80, tmp, "textpane_sizes.pdf");

        assertPdfContains(pdf, "BigText");
        assertPdfContains(pdf, "SmallText");
    }

    // -----------------------------------------------------------------------
    // Underline and strikethrough
    // -----------------------------------------------------------------------

    @Test
    void underlineAndStrikethrough_textSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        StyledDocument doc = pane.getStyledDocument();

        Style ul = doc.addStyle("ul", null);
        StyleConstants.setUnderline(ul, true);

        Style st = doc.addStyle("st", null);
        StyleConstants.setStrikeThrough(st, true);

        doc.insertString(0, "Underlined ", ul);
        doc.insertString(doc.getLength(), "Struck", st);

        Path pdf = exportPane(pane, 300, 60, tmp, "textpane_deco.pdf");

        assertPdfContains(pdf, "Underlined");
        assertPdfContains(pdf, "Struck");
    }

    // -----------------------------------------------------------------------
    // Multi-paragraph (newlines create new paragraphs in StyledDocument)
    // -----------------------------------------------------------------------

    @Test
    void multiParagraph_allLinesSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        pane.setText("First line\nSecond line\nThird line");

        Path pdf = exportPane(pane, 300, 80, tmp, "textpane_multiline.pdf");

        assertPdfContains(pdf, "First line");
        assertPdfContains(pdf, "Second line");
        assertPdfContains(pdf, "Third line");
    }

    // -----------------------------------------------------------------------
    // Word-wrap (long line in narrow pane)
    // -----------------------------------------------------------------------

    @Test
    void wordWrapped_textSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();
        pane.setText("The quick brown fox jumps over the lazy dog");

        // Narrow pane forces wrapping
        Path pdf = exportPane(pane, 120, 80, tmp, "textpane_wrap.pdf");

        assertPdfContains(pdf, "quick brown fox");
    }

    // -----------------------------------------------------------------------
    // Empty pane — must not throw
    // -----------------------------------------------------------------------

    @Test
    void emptyPane_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextPane pane = new JTextPane();

        Path pdf = exportPane(pane, 200, 60, tmp, "textpane_empty.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Path exportPane(JTextPane pane, int w, int h,
                                    Path tmp, String name) throws Exception {
        pane.setPreferredSize(new Dimension(w, h));
        pane.setSize(w, h);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w + 10, h + 10));
        panel.setSize(w + 10, h + 10);
        panel.add(pane, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);
        return pdf;
    }

    private static void assertPdfContains(Path pdf, String expected) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(expected);
        }
    }
}
