package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for EtchedBorder/MatteBorder rendering, JSplitPane divider, and
 * JInternalFrame title bar.
 */
class NicheContainerTest {

    // -----------------------------------------------------------------------
    // EtchedBorder
    // -----------------------------------------------------------------------

    @Test
    void etchedBorder_lowered_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = labelledPanel("Etched Lowered",
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        Path pdf = export(panel, tmp, "etched_lowered.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(doc)).contains("Etched Lowered");
        }
    }

    @Test
    void etchedBorder_raised_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = labelledPanel("Etched Raised",
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        Path pdf = export(panel, tmp, "etched_raised.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void matteBorder_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = labelledPanel("Matte Border",
                BorderFactory.createMatteBorder(4, 4, 4, 4, Color.BLUE));
        Path pdf = export(panel, tmp, "matte.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(doc)).contains("Matte Border");
        }
    }

    @Test
    void titledBorder_withEtchedInner_textSelectableInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // TitledBorder defaults to EtchedBorder as its inner border — previously broken
        JPanel panel = labelledPanel("Section Content",
                BorderFactory.createTitledBorder("Section Title"));
        Path pdf = export(panel, tmp, "titled_etched.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Section Title");
            assertThat(text).contains("Section Content");
        }
    }

    // -----------------------------------------------------------------------
    // JSplitPane
    // -----------------------------------------------------------------------

    @Test
    void horizontalSplitPane_bothPanelsTextSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel left  = new JLabel("Left Panel");
        JLabel right = new JLabel("Right Panel");

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setDividerLocation(120);
        sp.setPreferredSize(new Dimension(300, 80));
        sp.setSize(300, 80);
        sp.doLayout();
        sp.validate();

        JPanel root = wrapWhite(sp, 300, 80);
        Path pdf = export(root, tmp, "split_h.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Left Panel");
            assertThat(text).contains("Right Panel");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void verticalSplitPane_bothPanelsTextSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JLabel top    = new JLabel("Top Panel");
        JLabel bottom = new JLabel("Bottom Panel");

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        sp.setDividerLocation(60);
        sp.setPreferredSize(new Dimension(200, 150));
        sp.setSize(200, 150);
        sp.doLayout();
        sp.validate();

        JPanel root = wrapWhite(sp, 200, 150);
        Path pdf = export(root, tmp, "split_v.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Top Panel");
            assertThat(text).contains("Bottom Panel");
        }
    }

    // -----------------------------------------------------------------------
    // JInternalFrame
    // -----------------------------------------------------------------------

    @Test
    void internalFrame_titleAndContentSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JInternalFrame frame = new JInternalFrame("My Document", true, true, true, true);
        frame.getContentPane().add(new JLabel("Frame Content"), BorderLayout.CENTER);
        frame.setSize(280, 160);
        frame.setVisible(true);

        JDesktopPane desktop = new JDesktopPane();
        desktop.setBackground(Color.LIGHT_GRAY);
        desktop.setPreferredSize(new Dimension(320, 200));
        desktop.setSize(320, 200);
        desktop.add(frame);
        desktop.doLayout();
        desktop.validate();
        frame.doLayout();
        frame.validate();

        JPanel root = wrapWhite(desktop, 320, 200);
        Path pdf = export(root, tmp, "internal_frame.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("My Document");
            assertThat(text).contains("Frame Content");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void internalFrame_noTitle_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JInternalFrame frame = new JInternalFrame("", false, false, false, false);
        frame.getContentPane().add(new JLabel("Untitled"), BorderLayout.CENTER);
        frame.setSize(200, 120);
        frame.setVisible(true);

        JDesktopPane desktop = new JDesktopPane();
        desktop.setPreferredSize(new Dimension(240, 160));
        desktop.setSize(240, 160);
        desktop.add(frame);
        desktop.doLayout();
        desktop.validate();
        frame.doLayout();
        frame.validate();

        JPanel root = wrapWhite(desktop, 240, 160);
        Path pdf = export(root, tmp, "internal_frame_notitle.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JPanel labelledPanel(String text, Border border) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(border);
        panel.setPreferredSize(new Dimension(200, 60));
        panel.setSize(200, 60);
        panel.add(new JLabel(text), BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();
        return panel;
    }

    private static JPanel wrapWhite(Component comp, int w, int h) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        panel.add(comp, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();
        return panel;
    }

    private static Path export(JPanel root, Path tmp, String name) throws Exception {
        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);
        return pdf;
    }
}
