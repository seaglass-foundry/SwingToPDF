package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.seaglassfoundry.swingtopdf.demo.ComprehensiveDemoFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visual fidelity tests that compare a Swing panel rendered to a BufferedImage
 * against the same panel exported to PDF and rendered back via PDFRenderer.
 *
 * <p>These tests self-skip unless the system property is set:
 * <pre>
 *   mvn test -Dswing2pdf.visualFidelity=true -Dtest=VisualFidelityTest
 * </pre>
 *
 * <p>Artefacts (swing.png, pdf.png, diff.png) are saved to
 * {@code target/visual-comparison/<name>/} for human inspection.
 */
class VisualFidelityTest {

    // ---- Tunable constants ----
    static final double RMSE_THRESHOLD  = 8.0;   // 0–255 scale; tune after first run
    static final int    PIXEL_HOT_DELTA = 30;    // per-channel delta → red in diff image
    static final float  A4_WIDTH_PT     = 595f;
    static final Path   OUTPUT_ROOT     = Paths.get("target", "visual-comparison");

    // ---- Panel descriptor ----
    record PanelCase(String name, String builderMethod, boolean acroForm) {
        @Override public String toString() { return name; }
    }

    // ---- 14 test cases matching the tabs in ComprehensiveDemoFrame ----
    static Stream<PanelCase> panelCases() {
        return Stream.of(
            new PanelCase("basic-form",      "buildBasicForm",      false),
            new PanelCase("long-form",       "buildLongForm",       false),
            new PanelCase("data-table",      "buildDataTable",      false),
            new PanelCase("long-list",       "buildLongList",       false),
            new PanelCase("tree-view",       "buildTreeView",       false),
            new PanelCase("rich-text",       "buildRichText",       false),
            new PanelCase("html-labels",     "buildHtmlLabels",     false),
            new PanelCase("value-widgets",   "buildValueWidgets",   false),
            new PanelCase("borders",         "buildBordersGallery", false),
            new PanelCase("buttons",         "buildButtonsPanel",   false),
            new PanelCase("split-pane",      "buildSplitPane",      false),
            new PanelCase("internal-frames", "buildInternalFrames", false),
            new PanelCase("mixed-ui",        "buildMixedUi",        false),
            new PanelCase("acro-form",       "buildAcroForm",       true)
        );
    }

    // ---- Main parameterised test ----
    @ParameterizedTest(name = "{0}")
    @MethodSource("panelCases")
    void visualFidelity(PanelCase pc, @TempDir Path tmp) throws Exception {
        assumeTrue(Boolean.getBoolean("swing2pdf.visualFidelity"),
                "Visual fidelity tests disabled — use -Dswing2pdf.visualFidelity=true");
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipped in headless environment");

        JPanel panel = buildPanel(pc.builderMethod());

        // Size the panel
        Dimension pref = panel.getPreferredSize();
        int panelW = pref.width  > 0 ? pref.width  : 800;
        int panelH = pref.height > 0 ? pref.height : 600;
        panel.setSize(panelW, panelH);
        panel.validate();

        // Swing render
        BufferedImage swingImg = renderSwing(panel, panelW, panelH);

        // Export PDF with zero margins so content origin matches panel origin
        Path pdfPath = tmp.resolve(pc.name() + ".pdf");
        SwingPdfExporter exporter = SwingPdfExporter.from(panel).margins(0, 0, 0, 0);
        if (pc.acroForm()) exporter = exporter.enableAcroForm();
        exporter.export(pdfPath);

        // PDF render — DPI chosen so PDF image width == panel width
        float renderDpi = 72f * panelW / A4_WIDTH_PT;
        BufferedImage pdfImg = renderPdf(pdfPath, renderDpi);

        // Align: take the top slice of the Swing image matching the PDF page height
        BufferedImage swingAligned = alignSwingToPage(swingImg, pdfImg);

        // Save artefacts
        Path outDir = OUTPUT_ROOT.resolve(pc.name());
        Files.createDirectories(outDir);

        // Compare and save diff
        double rmse = compare(swingAligned, pdfImg, outDir);

        // Save swing and pdf images
        ImageIO.write(swingAligned, "png", outDir.resolve("swing.png").toFile());
        ImageIO.write(pdfImg,       "png", outDir.resolve("pdf.png").toFile());

        System.out.printf("[visual-fidelity] %-20s RMSE=%.2f  (threshold=%.1f)  %s%n",
                pc.name(), rmse, RMSE_THRESHOLD, rmse <= RMSE_THRESHOLD ? "✓" : "✗");

        assertThat(rmse)
                .as("RMSE for panel '%s' exceeds threshold %.1f (actual=%.2f); "
                        + "see target/visual-comparison/%s/", pc.name(), RMSE_THRESHOLD, rmse, pc.name())
                .isLessThanOrEqualTo(RMSE_THRESHOLD);
    }

    // ---- Helpers ----

    static JPanel buildPanel(String methodName) throws Exception {
        Method m = ComprehensiveDemoFrame.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        return (JPanel) m.invoke(null);
    }

    static BufferedImage renderSwing(JPanel panel, int w, int h) {
        if (!panel.isDisplayable()) panel.addNotify();
        panel.validate();
        BufferedImage img = new BufferedImage(w, h, TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING,        VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING,   VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(KEY_RENDERING,           VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        panel.paint(g);
        g.dispose();
        return img;
    }

    static BufferedImage renderPdf(Path pdfPath, float dpi) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            return new PDFRenderer(doc).renderImageWithDPI(0, dpi);
        }
    }

    /**
     * Returns a slice (or scaled version) of {@code swing} that matches the
     * dimensions of {@code pdfPage}. Handles multi-page exports by comparing
     * the first page region only.
     */
    static BufferedImage alignSwingToPage(BufferedImage swing, BufferedImage pdfPage) {
        int w = pdfPage.getWidth();
        int h = pdfPage.getHeight();

        // Happy path: widths match, swing is at least as tall as the PDF page
        if (swing.getWidth() == w && swing.getHeight() >= h) {
            return swing.getSubimage(0, 0, w, h);
        }

        // Fallback: bilinear scale to match PDF page dimensions
        BufferedImage scaled = new BufferedImage(w, h, TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(swing, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    /**
     * Computes per-pixel RMSE (0–255 scale) between two same-size images,
     * writes a diff image highlighting large per-channel deltas in red,
     * and returns the RMSE value.
     */
    static double compare(BufferedImage a, BufferedImage b, Path outDir) throws IOException {
        int w = Math.min(a.getWidth(),  b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());

        BufferedImage diff = new BufferedImage(w, h, TYPE_INT_RGB);
        double sumSq = 0.0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int ca = a.getRGB(x, y);
                int cb = b.getRGB(x, y);

                int dR = ((ca >> 16) & 0xFF) - ((cb >> 16) & 0xFF);
                int dG = ((ca >>  8) & 0xFF) - ((cb >>  8) & 0xFF);
                int dB = ( ca        & 0xFF) - ( cb        & 0xFF);

                sumSq += (double) dR * dR + (double) dG * dG + (double) dB * dB;

                boolean hot = Math.abs(dR) > PIXEL_HOT_DELTA
                           || Math.abs(dG) > PIXEL_HOT_DELTA
                           || Math.abs(dB) > PIXEL_HOT_DELTA;
                diff.setRGB(x, y, hot ? 0xFF0000 : cb);
            }
        }

        double rmse = Math.sqrt(sumSq / (3.0 * w * h));
        ImageIO.write(diff, "png", outDir.resolve("diff.png").toFile());
        return rmse;
    }
}
