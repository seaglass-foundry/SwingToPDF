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
 * Verifies that JButton, JCheckBox, and JRadioButton labels are selectable
 * in the exported PDF.
 */
class AbstractButtonHandlerTest {

    // -----------------------------------------------------------------------
    // JButton
    // -----------------------------------------------------------------------

    @Test
    void jButton_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JButton btn = new JButton("Click Me");
        JPanel panel = panel(200, 50);
        panel.add(btn, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("button.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Click Me");
    }

    @Test
    void jButton_disabled_textStillSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JButton btn = new JButton("Save");
        btn.setEnabled(false);
        JPanel panel = panel(200, 50);
        panel.add(btn, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("button_disabled.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Save");
    }

    // -----------------------------------------------------------------------
    // JCheckBox
    // -----------------------------------------------------------------------

    @Test
    void jCheckBox_unchecked_labelIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("Enable feature");
        cb.setSelected(false);
        JPanel panel = panel(200, 30);
        panel.add(cb, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("checkbox_off.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Enable feature");
    }

    @Test
    void jCheckBox_checked_labelIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("Active", true);
        JPanel panel = panel(200, 30);
        panel.add(cb, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("checkbox_on.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Active");
    }

    // -----------------------------------------------------------------------
    // JRadioButton
    // -----------------------------------------------------------------------

    @Test
    void jRadioButton_unselected_labelIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton rb = new JRadioButton("Option A");
        rb.setSelected(false);
        JPanel panel = panel(200, 30);
        panel.add(rb, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("radio_off.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Option A");
    }

    @Test
    void jRadioButton_selected_labelIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton rb = new JRadioButton("Option B", true);
        JPanel panel = panel(200, 30);
        panel.add(rb, BorderLayout.CENTER);
        layout(panel);

        Path pdf = tmp.resolve("radio_on.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        assertPdfContains(pdf, "Option B");
    }

    // -----------------------------------------------------------------------
    // Mixed form panel
    // -----------------------------------------------------------------------

    @Test
    void mixedFormPanel_allLabelsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
        panel.setBackground(Color.WHITE);
        panel.add(new JButton("Submit"));
        panel.add(new JCheckBox("I agree", true));
        panel.add(new JRadioButton("Yes"));
        panel.add(new JRadioButton("No", true));

        panel.setPreferredSize(new Dimension(250, 160));
        panel.setSize(250, 160);
        panel.doLayout();
        for (Component c : panel.getComponents()) {
            c.setSize(250, 40);
        }

        Path pdf = tmp.resolve("form.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Submit");
            assertThat(text).contains("I agree");
            assertThat(text).contains("Yes");
            assertThat(text).contains("No");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JPanel panel(int w, int h) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(w, h));
        p.setSize(w, h);
        return p;
    }

    private static void layout(JPanel panel) {
        panel.doLayout();
        for (Component c : panel.getComponents()) {
            if (c.getWidth() == 0) c.setSize(panel.getWidth(), panel.getHeight());
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
