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
 * Verifies that JTextField, JTextArea, and JPasswordField content is
 * selectable in the exported PDF.
 */
class JTextComponentHandlerTest {

    // -----------------------------------------------------------------------
    // JTextField
    // -----------------------------------------------------------------------

    @Test
    void jTextField_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField field = new JTextField("Enter your name");
        JPanel panel = sized(new JPanel(new BorderLayout()), 300, 40);
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("textfield.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Enter your name");
    }

    @Test
    void jTextField_centreAligned_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField field = new JTextField("Centred Text");
        field.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel panel = sized(new JPanel(new BorderLayout()), 300, 40);
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("textfield_center.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Centred Text");
    }

    @Test
    void jTextField_rightAligned_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField field = new JTextField("Right Value");
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel panel = sized(new JPanel(new BorderLayout()), 300, 40);
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("textfield_right.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Right Value");
    }

    // -----------------------------------------------------------------------
    // JTextArea
    // -----------------------------------------------------------------------

    @Test
    void jTextArea_multiLineTextIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextArea area = new JTextArea("First line\nSecond line\nThird line");
        area.setLineWrap(false);
        JPanel panel = sized(new JPanel(new BorderLayout()), 300, 120);
        panel.setBackground(Color.WHITE);
        panel.add(area, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("textarea.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("First line");
            assertThat(text).contains("Second line");
            assertThat(text).contains("Third line");
        }
    }

    @Test
    void jTextArea_wordWrap_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextArea area = new JTextArea(
                "The quick brown fox jumps over the lazy dog near the riverbank");
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel panel = sized(new JPanel(new BorderLayout()), 150, 200);
        panel.setBackground(Color.WHITE);
        panel.add(area, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("textarea_wrap.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        // All words must be present, though potentially on different lines
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc).replace("\n", " ");
            assertThat(text).contains("quick");
            assertThat(text).contains("fox");
            assertThat(text).contains("riverbank");
        }
    }

    // -----------------------------------------------------------------------
    // JPasswordField
    // -----------------------------------------------------------------------

    @Test
    void jPasswordField_showsEchoCharsNotPassword(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPasswordField field = new JPasswordField("secret123");
        field.setEchoChar('*');
        JPanel panel = sized(new JPanel(new BorderLayout()), 200, 40);
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("password.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).doesNotContain("secret123");
            assertThat(text).contains("*");
        }
    }

    // -----------------------------------------------------------------------
    // JFormattedTextField
    // -----------------------------------------------------------------------

    @Test
    void jFormattedTextField_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JFormattedTextField field = new JFormattedTextField("2025-12-31");
        JPanel panel = sized(new JPanel(new BorderLayout()), 300, 40);
        panel.setBackground(Color.WHITE);
        panel.add(field, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("formatted_field.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "2025-12-31");
    }

    // -----------------------------------------------------------------------
    // Multiple fields in a form-like panel
    // -----------------------------------------------------------------------

    @Test
    void formPanel_labelAndFieldPairs_allTextSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new GridLayout(3, 2, 4, 4));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Name:"));
        panel.add(new JTextField("Alice"));
        panel.add(new JLabel("City:"));
        panel.add(new JTextField("Springfield"));
        panel.add(new JLabel("Notes:"));
        panel.add(new JTextArea("Some notes here"));

        sized(panel, 400, 120);
        layout(panel);

        Path pdf = tmp.resolve("form.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Name:");
            assertThat(text).contains("Alice");
            assertThat(text).contains("City:");
            assertThat(text).contains("Springfield");
            assertThat(text).contains("Notes:");
            assertThat(text).contains("Some notes here");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JPanel sized(JPanel panel, int w, int h) {
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        return panel;
    }

    private static void layout(JPanel panel) {
        panel.doLayout();
        for (Component c : panel.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setSize(jc.getPreferredSize().width == 0 ? panel.getWidth() : jc.getPreferredSize().width,
                           jc.getPreferredSize().height == 0 ? 30 : jc.getPreferredSize().height);
            }
        }
        panel.validate();
    }

    private static void assertPdfContains(Path pdf, String expected) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(expected);
        }
    }
}
