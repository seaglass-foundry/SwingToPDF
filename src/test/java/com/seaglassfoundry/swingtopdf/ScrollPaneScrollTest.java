package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests that JScrollPane content is fully exported in DATA_REPORT mode
 * regardless of the current scroll position.
 */
class ScrollPaneScrollTest {

    /** Scrolled JTextArea — all text must appear (not just the visible window). */
    @Test
    void scrolledTextArea_allTextAppearsInDataReport(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextArea ta = new JTextArea("Line1\nLine2\nLine3\nLine4\nLine5\nLine6\nLine7\nLine8");
        ta.setLineWrap(false);
        JScrollPane sp = new JScrollPane(ta);
        // Show only ~3 lines; scroll down so Line1/Line2 are out of the viewport
        sp.setPreferredSize(new Dimension(200, 50));
        sp.setSize(200, 50);
        sp.validate();
        sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());

        Path pdf = exportDataReport(sp, tmp, "scrolled_textarea.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Line1");   // top content must appear
            assertThat(text).contains("Line8");   // bottom content must appear
        }
    }

    /** Scrolled JPanel with labels — top labels must appear. */
    @Test
    void scrolledPanel_topLabelAppearsInDataReport(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new JLabel("TOP_LABEL"));
        for (int i = 0; i < 10; i++) content.add(new JLabel("filler " + i));
        content.add(new JLabel("BOTTOM_LABEL"));

        JScrollPane sp = new JScrollPane(content);
        sp.setPreferredSize(new Dimension(200, 80));
        sp.setSize(200, 80);
        sp.validate();
        // Scroll to bottom so TOP_LABEL is out of the visible viewport
        sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());

        Path pdf = exportDataReport(sp, tmp, "scrolled_panel.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("TOP_LABEL");
            assertThat(text).contains("BOTTOM_LABEL");
        }
    }

    /**
     * Mirrors ComprehensiveDemoFrame.generateAction(): the content JPanel is exported
     * directly (not the JScrollPane), while the enclosing JScrollPane is scrolled down.
     * All content must appear — not just what was visible in the viewport.
     */
    @Test
    void scrolledViewport_contentPanelExportedDirectly(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new JLabel("TOP_LABEL"));
        for (int i = 0; i < 10; i++) content.add(new JLabel("filler " + i));
        content.add(new JLabel("BOTTOM_LABEL"));

        JScrollPane sp = new JScrollPane(content);
        sp.setSize(200, 80);
        sp.validate();
        // Scroll to bottom — content.getY() is now negative
        sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());

        // Export the content panel directly, exactly like ComprehensiveDemoFrame does
        Dimension pref = content.getPreferredSize();
        content.setSize(pref.width > 0 ? pref.width : 800, pref.height > 0 ? pref.height : 600);
        content.validate();

        Path pdf = tmp.resolve("content_panel_direct.pdf");
        SwingPdfExporter.from(content)
                .exportMode(com.seaglassfoundry.swingtopdf.api.ExportMode.DATA_REPORT)
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("TOP_LABEL");
            assertThat(text).contains("BOTTOM_LABEL");
        }
    }

    private static Path exportDataReport(JComponent comp, Path tmp, String name) throws Exception {
        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(comp)
                .exportMode(com.seaglassfoundry.swingtopdf.api.ExportMode.DATA_REPORT)
                .export(pdf);
        return pdf;
    }
}
