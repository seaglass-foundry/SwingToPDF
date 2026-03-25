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
 * Verifies tab title and content rendering for JTabbedPane in both modes.
 */
class JTabbedPaneHandlerTest {

    // -----------------------------------------------------------------------
    // UI_SNAPSHOT — only selected tab content rendered
    // -----------------------------------------------------------------------

    @Test
    void snapshot_selectedTabContent_isSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = buildTabs(
                new String[]{"Alpha", "Beta", "Gamma"},
                new String[]{"Content of Alpha", "Content of Beta", "Content of Gamma"});
        tp.setSelectedIndex(0);

        Path pdf = export(tp, tmp.resolve("snapshot.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // Selected tab content must be present
            assertThat(text).contains("Content of Alpha");
            // Tab bar titles must be drawn
            assertThat(text).contains("Alpha");
            assertThat(text).contains("Beta");
            assertThat(text).contains("Gamma");
        }
    }

    @Test
    void snapshot_differentSelectedTab_contentChanges(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = buildTabs(
                new String[]{"One", "Two"},
                new String[]{"First panel text", "Second panel text"});
        tp.setSelectedIndex(1);

        Path pdf = export(tp, tmp.resolve("snapshot2.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Second panel text");
        }
    }

    // -----------------------------------------------------------------------
    // DATA_REPORT — all tabs stacked vertically
    // -----------------------------------------------------------------------

    @Test
    void dataReport_allTabContents_areRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = buildTabs(
                new String[]{"Tab A", "Tab B", "Tab C"},
                new String[]{"Panel A content", "Panel B content", "Panel C content"});
        tp.setSelectedIndex(0);

        Path pdf = export(tp, tmp.resolve("datareport-all.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // All three panels must be present, not just the selected one
            assertThat(text).contains("Panel A content");
            assertThat(text).contains("Panel B content");
            assertThat(text).contains("Panel C content");
            // Tab bar and section headers include all titles
            assertThat(text).contains("Tab A");
            assertThat(text).contains("Tab B");
            assertThat(text).contains("Tab C");
        }
    }

    @Test
    void dataReport_nonSelectedTab_isAlsoRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = buildTabs(
                new String[]{"First", "Second"},
                new String[]{"Content of first tab", "Content of second tab"});
        tp.setSelectedIndex(0);   // only First is selected

        Path pdf = export(tp, tmp.resolve("datareport-nonselected.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // Both panels must appear even though Second is not selected
            assertThat(text).contains("Content of first tab");
            assertThat(text).contains("Content of second tab");
        }
    }

    @Test
    void dataReport_tabBarTitles_areRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = buildTabs(
                new String[]{"Overview", "Details"},
                new String[]{"Summary text", "Detailed breakdown"});

        Path pdf = export(tp, tmp.resolve("sections.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Overview");
            assertThat(text).contains("Details");
            assertThat(text).contains("Summary text");
            assertThat(text).contains("Detailed breakdown");
        }
    }

    @Test
    void emptyTabbedPane_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = new JTabbedPane();
        JPanel root = sized(new JPanel(new BorderLayout()), 400, 300);
        root.setBackground(Color.WHITE);
        root.add(tp, BorderLayout.CENTER);
        root.validate();

        Path pdf = tmp.resolve("empty.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Mixed content — tabs containing tables, fields, labels
    // -----------------------------------------------------------------------

    @Test
    void dataReport_selectedTabWithMixedContent_isSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTabbedPane tp = new JTabbedPane();

        // Tab 1 (selected): labels
        JPanel labels = new JPanel(new GridLayout(2, 1));
        labels.add(new JLabel("Label One"));
        labels.add(new JLabel("Label Two"));
        tp.addTab("Labels", labels);

        // Tab 2: text field
        JPanel fields = new JPanel(new BorderLayout());
        fields.add(new JTextField("Field value here"), BorderLayout.CENTER);
        tp.addTab("Fields", fields);

        tp.setSelectedIndex(0);
        tp.setPreferredSize(new Dimension(400, 200));
        tp.setSize(400, 200);
        tp.validate();

        Path pdf = export(tp, tmp.resolve("mixed.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Label One");
            assertThat(text).contains("Label Two");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JTabbedPane buildTabs(String[] titles, String[] contents) {
        JTabbedPane tp = new JTabbedPane();
        for (int i = 0; i < titles.length; i++) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.WHITE);
            panel.add(new JLabel(contents[i]), BorderLayout.CENTER);
            tp.addTab(titles[i], panel);
        }
        tp.setPreferredSize(new Dimension(400, 200));
        tp.setSize(400, 200);
        tp.validate();
        return tp;
    }

    private static JPanel sized(JPanel p, int w, int h) {
        p.setPreferredSize(new Dimension(w, h));
        p.setSize(w, h);
        return p;
    }

    private static Path export(JTabbedPane tp, Path out, ExportMode mode) throws Exception {
        SwingPdfExporter.from(tp).pageSize(PageSize.A4).exportMode(mode).export(out);
        return out;
    }
}
