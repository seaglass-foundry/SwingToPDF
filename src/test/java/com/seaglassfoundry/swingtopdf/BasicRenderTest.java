package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Basic smoke tests for the walker-based rendering pipeline.
 * Verifies that text from standard components is selectable in the output PDF.
 */
class BasicRenderTest {

    @Test
    void jLabel_textIsSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Hello, PDF World!");
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 80));
        panel.add(label, BorderLayout.CENTER);
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();

        Path pdf = tmp.resolve("label.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.UI_SNAPSHOT)
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Hello, PDF World!");
        }
    }

    @Test
    void jPanel_backgroundFills(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel();
        panel.setBackground(Color.BLUE);
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(200, 100));
        panel.setSize(panel.getPreferredSize());

        Path pdf = tmp.resolve("panel.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .export(pdf);

        // Just verify a valid PDF is produced with one page
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void nestedPanels_multipleLabels_allTextSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        panel.add(new JLabel("Line One"));
        panel.add(new JLabel("Line Two"));
        panel.add(new JLabel("Line Three"));
        panel.setPreferredSize(new Dimension(300, 120));
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();
        for (Component c : panel.getComponents()) {
            c.setSize(300, 40);
        }

        Path pdf = tmp.resolve("nested.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Line One");
            assertThat(text).contains("Line Two");
            assertThat(text).contains("Line Three");
        }
    }

    @Test
    void boldLabel_embeddedAndSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel label = new JLabel("Bold Title");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(300, 60));
        panel.add(label, BorderLayout.CENTER);
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();

        Path pdf = tmp.resolve("bold.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Bold Title");
        }
    }
}
