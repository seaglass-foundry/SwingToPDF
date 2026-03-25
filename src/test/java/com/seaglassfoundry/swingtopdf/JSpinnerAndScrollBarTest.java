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
 * Tests for JSpinner vector rendering and JScrollBar vector rendering.
 */
class JSpinnerAndScrollBarTest {

    // -----------------------------------------------------------------------
    // JSpinner
    // -----------------------------------------------------------------------

    @Test
    void spinner_integer_valueAppearsAsSelectableText(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(42, 0, 100, 1));
        Path pdf = exportWrapped(spinner, 120, 30, tmp, "spinner_int.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("42");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void spinner_string_valueAppearsAsSelectableText(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JSpinner spinner = new JSpinner(
                new SpinnerListModel(new String[]{"Alpha", "Beta", "Gamma"}));
        Path pdf = exportWrapped(spinner, 160, 30, tmp, "spinner_str.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // Selected item (first by default) must be present as text
            assertThat(text).containsAnyOf("Alpha", "Beta", "Gamma");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void spinner_producesValidSinglePagePdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new GridLayout(3, 2, 4, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 120));
        panel.setSize(300, 120);

        panel.add(new JLabel("Count:"));
        panel.add(new JSpinner(new SpinnerNumberModel(5, 0, 99, 1)));
        panel.add(new JLabel("Price:"));
        panel.add(new JSpinner(new SpinnerNumberModel(9.99, 0.0, 999.0, 0.5)));
        panel.add(new JLabel("Option:"));
        panel.add(new JSpinner(new SpinnerListModel(new String[]{"Low", "Med", "High"})));

        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("spinner_form.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Count");
            assertThat(text).contains("Price");
        }
    }

    // -----------------------------------------------------------------------
    // JScrollBar
    // -----------------------------------------------------------------------

    @Test
    void verticalScrollBar_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JScrollBar sb = new JScrollBar(JScrollBar.VERTICAL, 30, 20, 0, 100);
        Path pdf = exportWrapped(sb, 20, 150, tmp, "scrollbar_v.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void horizontalScrollBar_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JScrollBar sb = new JScrollBar(JScrollBar.HORIZONTAL, 10, 30, 0, 100);
        Path pdf = exportWrapped(sb, 200, 20, tmp, "scrollbar_h.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void scrollPane_withContent_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextArea area = new JTextArea("Scrollable content here");
        area.setPreferredSize(new Dimension(400, 300));

        JScrollPane sp = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(200, 120));
        sp.setSize(200, 120);
        sp.doLayout();
        sp.validate();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 120));
        panel.setSize(200, 120);
        panel.add(sp, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("scrollpane_with_scrollbars.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Scrollable content");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Path exportWrapped(Component comp, int compW, int compH,
                                       Path tmp, String name) throws Exception {
        comp.setPreferredSize(new Dimension(compW, compH));
        comp.setSize(compW, compH);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        panel.setBackground(Color.WHITE);
        int panelW = compW + 16;
        int panelH = compH + 16;
        panel.setPreferredSize(new Dimension(panelW, panelH));
        panel.setSize(panelW, panelH);
        panel.add(comp);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);
        return pdf;
    }
}
