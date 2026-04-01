package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.VectorComponentHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for the {@link VectorComponentHandler} feature, which allows
 * custom-painted components to emit vector PDF output instead of rasterised bitmaps.
 */
class VectorComponentHandlerTest {

    // -----------------------------------------------------------------------
    // Basic: handler is invoked and produces valid vector PDF
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_isInvoked_andProducesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        AtomicInteger callCount = new AtomicInteger(0);

        CustomDrawingPanel widget = new CustomDrawingPanel();
        JPanel panel = wrapInPanel(widget, 300, 200);

        Path pdf = tmp.resolve("basic_vector.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(CustomDrawingPanel.class, (comp, g2, bounds) -> {
                    callCount.incrementAndGet();
                    g2.setColor(Color.BLUE);
                    g2.fill(new Rectangle2D.Double(
                            bounds.getX(), bounds.getY(),
                            bounds.getWidth(), bounds.getHeight()));
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    g2.drawString("Vector Output", 10, 30);
                })
                .export(pdf);

        assertThat(callCount.get()).isGreaterThan(0);
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Vector text is selectable/extractable from the PDF
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_textIsExtractable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        CustomDrawingPanel widget = new CustomDrawingPanel();
        JPanel panel = wrapInPanel(widget, 400, 200);

        Path pdf = tmp.resolve("text_extractable.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(CustomDrawingPanel.class, (comp, g2, bounds) -> {
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    g2.drawString("Hello Vector World", 20, 50);
                    g2.drawString("Second line of text", 20, 80);
                })
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Hello Vector World");
            assertThat(text).contains("Second line of text");
        }
    }

    // -----------------------------------------------------------------------
    // Complex: shapes, gradients, strokes, multiple draw operations
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_complexDrawing_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        CustomDrawingPanel widget = new CustomDrawingPanel();
        JPanel panel = wrapInPanel(widget, 500, 300);

        Path pdf = tmp.resolve("complex_vector.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(CustomDrawingPanel.class, (comp, g2, bounds) -> {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

                    // Filled rectangle with gradient
                    g2.setPaint(new GradientPaint(0, 0, Color.BLUE, (float) bounds.getWidth(),
                            (float) bounds.getHeight(), Color.CYAN));
                    g2.fill(bounds);

                    // Stroked shapes
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(new Ellipse2D.Double(20, 20, 100, 100));
                    g2.draw(new Line2D.Double(20, 150, 480, 150));

                    // Text
                    g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                    g2.drawString("Chart Title", 150, 60);

                    // Multiple small rectangles (bar chart simulation)
                    double[] values = {40, 70, 55, 90, 60};
                    Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN};
                    for (int i = 0; i < values.length; i++) {
                        g2.setColor(colors[i]);
                        double bx = 60 + i * 80;
                        double bh = values[i] * 2;
                        g2.fill(new Rectangle2D.Double(bx, 280 - bh, 60, bh));
                    }

                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.setColor(Color.WHITE);
                    g2.drawString("Complex Vector Content", 150, 280);
                })
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Chart Title");
            assertThat(text).contains("Complex Vector Content");
        }
    }

    // -----------------------------------------------------------------------
    // Subclass matching: handler registered for parent matches subclass
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_matchesSubclass(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        AtomicInteger callCount = new AtomicInteger(0);

        // SubDrawingPanel extends CustomDrawingPanel — handler for CustomDrawingPanel should match
        SubDrawingPanel widget = new SubDrawingPanel();
        JPanel panel = wrapInPanel(widget, 200, 100);

        Path pdf = tmp.resolve("subclass_match.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(CustomDrawingPanel.class, (comp, g2, bounds) -> {
                    callCount.incrementAndGet();
                    g2.setColor(Color.GREEN);
                    g2.fill(bounds);
                })
                .export(pdf);

        assertThat(callCount.get()).isGreaterThan(0);
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // User handler overrides built-in handler for the same type
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_overridesBuiltIn(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        AtomicInteger callCount = new AtomicInteger(0);

        // JLabel has a built-in handler — registering a vector handler should override it
        JLabel label = new JLabel("Original Label");
        JPanel panel = wrapInPanel(label, 200, 50);

        Path pdf = tmp.resolve("override_builtin.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(JLabel.class, (comp, g2, bounds) -> {
                    callCount.incrementAndGet();
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                    g2.drawString("Custom Vector Label", 10, 30);
                })
                .export(pdf);

        assertThat(callCount.get()).isGreaterThan(0);
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Custom Vector Label");
            // The original label text "Original Label" should NOT appear because
            // the built-in handler was overridden
            assertThat(text).doesNotContain("Original Label");
        }
    }

    // -----------------------------------------------------------------------
    // Multiple handlers for different types
    // -----------------------------------------------------------------------

    @Test
    void vectorHandler_multipleHandlers_eachCalled(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        AtomicInteger customCount = new AtomicInteger(0);
        AtomicInteger subCount = new AtomicInteger(0);

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(400, 200));
        panel.setSize(400, 200);

        CustomDrawingPanel custom = new CustomDrawingPanel();
        AnotherCustomPanel another = new AnotherCustomPanel();
        panel.add(custom);
        panel.add(another);
        panel.doLayout();
        panel.validate();

        Path pdf = tmp.resolve("multi_handler.pdf");
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .registerHandler(CustomDrawingPanel.class, (comp, g2, bounds) -> {
                    customCount.incrementAndGet();
                    g2.setColor(Color.RED);
                    g2.fill(bounds);
                })
                .registerHandler(AnotherCustomPanel.class, (comp, g2, bounds) -> {
                    subCount.incrementAndGet();
                    g2.setColor(Color.BLUE);
                    g2.fill(bounds);
                })
                .export(pdf);

        assertThat(customCount.get()).isGreaterThan(0);
        assertThat(subCount.get()).isGreaterThan(0);
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Null arguments rejected
    // -----------------------------------------------------------------------

    @Test
    void registerHandler_nullType_throws() {
        SwingPdfExporter exporter = SwingPdfExporter.from(new JPanel());
        assertThatThrownBy(() -> exporter.registerHandler(null, (c, g, b) -> {}))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void registerHandler_nullHandler_throws() {
        SwingPdfExporter exporter = SwingPdfExporter.from(new JPanel());
        assertThatThrownBy(() -> exporter.registerHandler(JPanel.class, null))
                .isInstanceOf(NullPointerException.class);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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

    /** Custom-painted component with no built-in handler. */
    static class CustomDrawingPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        CustomDrawingPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Subclass of CustomDrawingPanel for subclass-matching test. */
    static class SubDrawingPanel extends CustomDrawingPanel {
        private static final long serialVersionUID = 1L;
    }

    /** A second custom-painted component for multi-handler test. */
    static class AnotherCustomPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.DARK_GRAY);
            g.fillOval(0, 0, getWidth(), getHeight());
        }
    }
}
