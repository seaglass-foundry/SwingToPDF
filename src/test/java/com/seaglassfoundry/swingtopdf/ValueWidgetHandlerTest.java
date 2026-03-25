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
 * Verifies JComboBox, JProgressBar, and JSlider rendering.
 */
class ValueWidgetHandlerTest {

    // =========================================================================
    // JComboBox
    // =========================================================================

    @Test
    void comboBox_selectedItem_isSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<String> cb = new JComboBox<>(new String[]{"Red", "Green", "Blue"});
        cb.setSelectedItem("Green");

        Path pdf = exportWrapped(cb, 200, 30, tmp.resolve("combo.pdf"));

        assertPdfContains(pdf, "Green");
    }

    @Test
    void comboBox_firstItem_isSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<String> cb = new JComboBox<>(new String[]{"Alpha", "Beta"});
        cb.setSelectedIndex(0);

        Path pdf = exportWrapped(cb, 200, 30, tmp.resolve("combo_first.pdf"));

        assertPdfContains(pdf, "Alpha");
    }

    @Test
    void comboBox_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<Integer> cb = new JComboBox<>(new Integer[]{1, 2, 3});
        cb.setSelectedIndex(2);

        Path pdf = exportWrapped(cb, 150, 28, tmp.resolve("combo_int.pdf"));

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // JProgressBar
    // =========================================================================

    @Test
    void progressBar_withString_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(75);
        bar.setStringPainted(true);
        bar.setString("75% complete");

        Path pdf = exportWrapped(bar, 300, 24, tmp.resolve("progress_string.pdf"));

        assertPdfContains(pdf, "75% complete");
    }

    @Test
    void progressBar_noString_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(40);
        bar.setStringPainted(false);

        Path pdf = exportWrapped(bar, 300, 24, tmp.resolve("progress_no_string.pdf"));

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void progressBar_defaultString_containsPercent(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(50);
        bar.setStringPainted(true); // string not set — library should render "50%"

        Path pdf = exportWrapped(bar, 300, 24, tmp.resolve("progress_default.pdf"));

        assertPdfContains(pdf, "50%");
    }

    @Test
    void progressBar_vertical_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JProgressBar bar = new JProgressBar(SwingConstants.VERTICAL, 0, 100);
        bar.setValue(60);

        Path pdf = exportWrapped(bar, 30, 120, tmp.resolve("progress_vertical.pdf"));

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // JSlider
    // =========================================================================

    @Test
    void slider_horizontal_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JSlider slider = new JSlider(0, 100, 42);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);

        Path pdf = exportWrapped(slider, 300, 50, tmp.resolve("slider_h.pdf"));

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void slider_withLabels_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JSlider slider = new JSlider(0, 100, 50);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(25);
        slider.setPaintLabels(true);

        Path pdf = exportWrapped(slider, 300, 60, tmp.resolve("slider_labels.pdf"));

        // Label table uses integers 0, 25, 50, 75, 100
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).containsAnyOf("0", "25", "50", "75", "100");
        }
    }

    @Test
    void slider_vertical_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 100, 70);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(20);

        Path pdf = exportWrapped(slider, 40, 200, tmp.resolve("slider_v.pdf"));

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // Mixed panel
    // =========================================================================

    @Test
    void mixedPanel_allWidgets_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
        panel.setBackground(Color.WHITE);

        JComboBox<String> cb = new JComboBox<>(new String[]{"Option X", "Option Y"});
        cb.setSelectedItem("Option X");

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(33);
        bar.setStringPainted(true);
        bar.setString("Loading...");

        JSlider slider = new JSlider(0, 10, 7);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(5);

        panel.add(new JLabel("Select mode:"));
        panel.add(cb);
        panel.add(bar);
        panel.add(slider);

        panel.setPreferredSize(new Dimension(350, 160));
        panel.setSize(350, 160);
        panel.validate();

        Path pdf = tmp.resolve("mixed.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Select mode:");
            assertThat(text).contains("Option X");
            assertThat(text).contains("Loading...");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Path exportWrapped(JComponent comp, int w, int h, Path out) throws Exception {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        panel.add(comp, BorderLayout.CENTER);
        panel.validate();

        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(out);
        return out;
    }

    private static void assertPdfContains(Path pdf, String expected) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains(expected);
        }
    }
}
