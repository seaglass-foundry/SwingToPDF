package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Verifies that Swing HTML text (strings starting with {@code <html>}) is rendered
 * as selectable plain text in the exported PDF, with tags stripped and HTML
 * entities decoded correctly.
 */
class HtmlRenderingTest {

    // -----------------------------------------------------------------------
    // JLabel — basic HTML stripping
    // -----------------------------------------------------------------------

    @Test
    void jLabel_htmlTagsStripped_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("<html><b>Hello</b> <i>World</i></html>");
        Path pdf = exportLabel(label, tmp, "html_label.pdf");

        assertPdfContains(pdf, "Hello");
        assertPdfContains(pdf, "World");
    }

    @Test
    void jLabel_htmlEntitiesDecoded(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // &amp; → &,  &lt; → <,  &gt; → >
        JLabel label = new JLabel("<html>A &amp; B &lt; C &gt; D</html>");
        Path pdf = exportLabel(label, tmp, "html_entities.pdf");

        assertPdfContains(pdf, "A & B < C > D");
    }

    @Test
    void jLabel_brTagSeparatesLines(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("<html>First<br>Second</html>");
        label.setPreferredSize(new Dimension(400, 60));
        label.setSize(400, 60);
        Path pdf = exportLabel(label, tmp, "html_br.pdf");

        // Both words must appear in the PDF text
        assertPdfContains(pdf, "First");
        assertPdfContains(pdf, "Second");
    }

    @Test
    void jLabel_multiLineBr_allLinesAppearInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("<html>Line One<br>Line Two<br>Line Three</html>");
        label.setPreferredSize(new Dimension(400, 80));
        label.setSize(400, 80);
        Path pdf = exportLabel(label, tmp, "html_multiline.pdf");

        assertPdfContains(pdf, "Line One");
        assertPdfContains(pdf, "Line Two");
        assertPdfContains(pdf, "Line Three");
    }

    @Test
    void jLabel_bodyStyleWrapper_textExtracted(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // Common pattern used in demos: wrap text with body style for word-wrap hints
        JLabel label = new JLabel("<html><body style='width:300px'>Important notice</body></html>");
        Path pdf = exportLabel(label, tmp, "html_body_style.pdf");

        assertPdfContains(pdf, "Important notice");
    }

    @Test
    void jLabel_caseInsensitiveHtmlTag(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("<HTML><B>UPPER</B></HTML>");
        Path pdf = exportLabel(label, tmp, "html_case.pdf");

        assertPdfContains(pdf, "UPPER");
    }

    @Test
    void jLabel_plainTextUnchanged(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Plain text, no HTML");
        Path pdf = exportLabel(label, tmp, "plain_text.pdf");

        assertPdfContains(pdf, "Plain text, no HTML");
    }

    // -----------------------------------------------------------------------
    // JButton — HTML label stripping
    // -----------------------------------------------------------------------

    @Test
    void jButton_htmlLabel_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JButton btn = new JButton("<html><b>Save</b> &amp; Close</html>");
        JPanel panel = wrapInPanel(btn, 220, 50);

        Path pdf = tmp.resolve("html_button.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Save");
        assertPdfContains(pdf, "Close");
    }

    @Test
    void jCheckBox_htmlLabel_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("<html>I &lt;agree&gt;</html>", true);
        JPanel panel = wrapInPanel(cb, 220, 30);

        Path pdf = tmp.resolve("html_checkbox.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "I <agree>");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Path exportLabel(JLabel label, Path tmp, String name) throws Exception {
        JPanel panel = wrapInPanel(label, 400, 60);
        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);
        return pdf;
    }

    private static JPanel wrapInPanel(Component comp, int w, int h) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        panel.add(comp, BorderLayout.CENTER);
        panel.doLayout();
        for (Component c : panel.getComponents()) {
            if (c.getWidth() == 0) c.setSize(w, h);
        }
        panel.validate();
        return panel;
    }

    private static void assertPdfContains(Path pdf, String expected) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(expected);
        }
    }
}
