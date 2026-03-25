package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for the JLabel border bug fix and icon rendering.
 */
class IconRenderingTest {

    // -----------------------------------------------------------------------
    // Border bug regression — text must survive when a label has a border
    // -----------------------------------------------------------------------

    @Test
    void jLabel_withLineBorder_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Bordered Label");
        label.setBorder(new LineBorder(Color.BLACK, 1));
        Path pdf = exportLabel(label, tmp, "border_line.pdf");

        assertPdfContains(pdf, "Bordered Label");
    }

    @Test
    void jLabel_withTitledBorder_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Inner Label Text");
        label.setBorder(new TitledBorder("Section"));
        Path pdf = exportLabel(label, tmp, "border_titled.pdf");

        assertPdfContains(pdf, "Inner Label Text");
    }

    @Test
    void jLabel_withTitledBorder_bothTitleAndTextSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Content");
        label.setBorder(new TitledBorder("Title"));
        Path pdf = exportLabel(label, tmp, "border_titled_both.pdf");

        assertPdfContains(pdf, "Content");
        assertPdfContains(pdf, "Title");
    }

    // -----------------------------------------------------------------------
    // Icon rendering — icon must be embedded as an image XObject
    // -----------------------------------------------------------------------

    @Test
    void jLabel_withIcon_iconEmbeddedInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Labelled", makeIcon(16, 16, Color.RED), SwingConstants.LEFT);
        Path pdf = exportLabel(label, tmp, "label_icon.pdf");

        assertPdfContains(pdf, "Labelled");
        assertPdfHasImageXObject(pdf);
    }

    @Test
    void jLabel_iconOnly_iconEmbeddedInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel(makeIcon(24, 24, Color.BLUE));
        Path pdf = exportLabel(label, tmp, "label_icon_only.pdf");

        assertPdfHasImageXObject(pdf);
    }

    @Test
    void jButton_withIcon_iconEmbeddedInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JButton btn = new JButton("Save", makeIcon(16, 16, Color.GREEN));
        JPanel panel = wrapInPanel(btn, 160, 50);

        Path pdf = tmp.resolve("button_icon.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Save");
        assertPdfHasImageXObject(pdf);
    }

    @Test
    void jButton_iconOnly_iconEmbeddedInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JButton btn = new JButton(makeIcon(20, 20, Color.ORANGE));
        JPanel panel = wrapInPanel(btn, 80, 50);

        Path pdf = tmp.resolve("button_icon_only.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfHasImageXObject(pdf);
    }

    @Test
    void jLabel_iconAndBorder_bothRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Status", makeIcon(12, 12, Color.GREEN), SwingConstants.LEFT);
        label.setBorder(new LineBorder(Color.DARK_GRAY, 1));
        Path pdf = exportLabel(label, tmp, "label_icon_border.pdf");

        assertPdfContains(pdf, "Status");
        assertPdfHasImageXObject(pdf);
    }

    // -----------------------------------------------------------------------
    // Disabled state — foreground must be dimmed
    // -----------------------------------------------------------------------

    @Test
    void jLabel_disabled_textAppearsInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Disabled Label");
        label.setEnabled(false);
        Path pdf = exportLabel(label, tmp, "label_disabled.pdf");

        // Text must still be present (dimmed colour, but selectable)
        assertPdfContains(pdf, "Disabled Label");
    }

    @Test
    void jLabel_disabledWithIcon_textAndIconPresent(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Off", makeIcon(12, 12, Color.GRAY), SwingConstants.LEFT);
        label.setEnabled(false);
        Path pdf = exportLabel(label, tmp, "label_disabled_icon.pdf");

        assertPdfContains(pdf, "Off");
        assertPdfHasImageXObject(pdf);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Icon makeIcon(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return new ImageIcon(img);
    }

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

    private static void assertPdfHasImageXObject(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            boolean found = false;
            for (PDPage page : doc.getPages()) {
                PDResources res = page.getResources();
                if (res != null && res.getXObjectNames().iterator().hasNext()) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as("PDF should contain at least one image XObject").isTrue();
        }
    }
}
