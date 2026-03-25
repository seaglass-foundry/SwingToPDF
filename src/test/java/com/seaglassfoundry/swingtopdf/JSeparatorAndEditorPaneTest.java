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
 * Tests for JSeparator vector rendering and JEditorPane HTML vector rendering.
 */
class JSeparatorAndEditorPaneTest {

    // -----------------------------------------------------------------------
    // JSeparator
    // -----------------------------------------------------------------------

    @Test
    void horizontalSeparator_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 60));
        panel.setSize(300, 60);

        JLabel above = new JLabel("Above");
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        JLabel below = new JLabel("Below");

        JPanel inner = new JPanel(new GridLayout(3, 1));
        inner.add(above);
        inner.add(sep);
        inner.add(below);
        panel.add(inner, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("separator_h.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        // Labels surrounding the separator must still be selectable
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Above");
            assertThat(text).contains("Below");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void verticalSeparator_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 50));
        panel.setSize(200, 50);

        panel.add(new JLabel("Left"));
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(4, 30));
        panel.add(sep);
        panel.add(new JLabel("Right"));
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("separator_v.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void toolbarSeparator_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JToolBar toolbar = new JToolBar();
        toolbar.add(new JButton("File"));
        toolbar.addSeparator();   // adds JToolBar.Separator (extends JSeparator)
        toolbar.add(new JButton("Edit"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 40));
        panel.setSize(300, 40);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("toolbar_separator.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("File");
            assertThat(text).contains("Edit");
        }
    }

    // -----------------------------------------------------------------------
    // JEditorPane — HTML content must be rendered as selectable vector text
    // -----------------------------------------------------------------------

    @Test
    void jEditorPane_htmlContent_isSelectableAndMarkupFree(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JEditorPane ep = new JEditorPane("text/html",
                "<html><body><b>Bold</b> and <i>italic</i> text</body></html>");
        ep.setPreferredSize(new Dimension(300, 80));
        ep.setSize(300, 80);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 80));
        panel.setSize(300, 80);
        panel.add(ep, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("editor_pane.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // HTML tags must not appear as literal text in the PDF
            assertThat(text).doesNotContain("<html>");
            assertThat(text).doesNotContain("<body>");
            assertThat(text).doesNotContain("<b>");
            // Text content must be selectable (vector, not raster)
            assertThat(text).contains("Bold");
            assertThat(text).contains("italic");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void jEditorPane_htmlTable_textIsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JEditorPane ep = new JEditorPane();
        ep.setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        ep.setEditable(false);
        ep.setText("<html><body>"
                + "<h2>Project Status Report</h2>"
                + "<table border='1' cellpadding='4'>"
                + "<tr><th>Project</th><th>Status</th></tr>"
                + "<tr><td>Alpha</td><td><font color='green'>On Track</font></td></tr>"
                + "<tr><td>Beta</td><td><font color='red'>Delayed</font></td></tr>"
                + "</table></body></html>");
        ep.setPreferredSize(new Dimension(500, 200));
        ep.setSize(500, 200);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(500, 200));
        panel.setSize(500, 200);
        panel.add(ep, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("editor_table.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Project Status Report");
            assertThat(text).contains("Alpha");
            assertThat(text).contains("On Track");
            assertThat(text).contains("Beta");
            assertThat(text).contains("Delayed");
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void jEditorPane_plainText_rendersAsValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JEditorPane ep = new JEditorPane("text/plain", "Plain editor content");
        ep.setPreferredSize(new Dimension(300, 60));
        ep.setSize(300, 60);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(300, 60));
        panel.setSize(300, 60);
        panel.add(ep, BorderLayout.CENTER);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("editor_plain.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }
}
