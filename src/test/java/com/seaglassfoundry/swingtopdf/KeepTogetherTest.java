package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that components marked with {@link SwingPdfExporter#KEEP_TOGETHER}
 * are not split across page boundaries.
 *
 * <h3>Why PDFTextStripperByArea?</h3>
 * {@code PDFTextStripper} extracts text from the content stream regardless of
 * whether the coordinates fall outside the page MediaBox.  When a keep-together
 * snap does NOT happen, the mid-label lands at {@code pdfBaseline ≈ 852 pt}
 * on a 842 pt page — above the top, Java 2D y ≈ −10.  That coordinate lies
 * outside any region we define, so {@code PDFTextStripperByArea} correctly
 * excludes it.  When the snap DOES happen (break moves to y=900), the same
 * label lands at {@code pdfBaseline ≈ 758 pt} (Java 2D y ≈ 84), inside the
 * page, and is extracted.
 *
 * <h3>Geometry (96 dpi, A4 portrait, default 36 pt margins)</h3>
 * <pre>
 *   stepPx  ≈ 1026 px/page     ideal break = 1026
 *   KT panel  y = 900–1100     straddles ideal break
 *   mid label y = 950 (abs)    inside KT panel, above ideal break
 *
 *   WITHOUT keep-together:  sliceTopPx page 2 = 1026
 *     pdfBaseline(965) = 841 – 36 – (965–1026)×0.75 = 852  > pageH → outside page
 *
 *   WITH keep-together:  sliceTopPx page 2 = 900
 *     pdfBaseline(965) = 841 – 36 – (965–900)×0.75 = 757  → within printable area
 * </pre>
 */
class KeepTogetherTest {

    private static final String LABEL_TEXT = "MID-LABEL";

    // -----------------------------------------------------------------------

    @Test
    void withKeepTogether_midLabelAppearsInPage2(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("kt-on.pdf");
        buildAndExport(true, out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(pageText(doc, 1)).contains(LABEL_TEXT);
        }
    }

    @Test
    void withoutKeepTogether_midLabelAbovePageTop_notInPage2(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("kt-off.pdf");
        buildAndExport(false, out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(pageText(doc, 1)).doesNotContain(LABEL_TEXT);
        }
    }

    @Test
    void keepTogether_componentTallerThanPage_doesNotHang(@TempDir Path tmp) throws Exception {
        // A component taller than one page cannot be kept together — must not hang
        JPanel kt = new JPanel(null);
        kt.setBounds(0, 100, 400, 2000);   // taller than stepPx ≈ 1026
        kt.setOpaque(false);
        kt.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);

        JPanel root = new JPanel(null);
        root.setBackground(Color.WHITE);
        root.setSize(400, 2200);
        root.add(kt);
        root.validate();

        Path out = tmp.resolve("tall-kt.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Build a 2200 px root with a panel spanning y=900–1100 (straddles ideal
     * break ≈ 1026) that contains a label at absolute y=950.
     * Optionally marks the panel KEEP_TOGETHER.
     */
    private static void buildAndExport(boolean keepTogether, Path out) throws Exception {
        // Label at relative y=50 inside panel → absolute y = 900 + 50 = 950
        JLabel label = new JLabel(LABEL_TEXT);
        label.setBounds(0, 50, 400, 20);

        JPanel panel = new JPanel(null);
        panel.setBounds(0, 900, 400, 200);
        panel.setOpaque(false);
        panel.add(label);
        if (keepTogether)
            panel.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);

        JPanel root = new JPanel(null);
        root.setBackground(Color.WHITE);
        root.setSize(400, 2200);
        root.add(panel);
        root.validate();

        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(out);
    }

    /**
     * Extract all text from the given 0-based page index using
     * {@link PDFTextStripperByArea} bounded to the full page rectangle.
     * Text whose coordinates fall outside the MediaBox (e.g. above the top)
     * will not be returned.
     */
    private static String pageText(PDDocument doc, int pageIndex) throws Exception {
        PDPage page  = doc.getPage(pageIndex);
        float  pageW = page.getMediaBox().getWidth();
        float  pageH = page.getMediaBox().getHeight();

        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.addRegion("page", new Rectangle2D.Float(0, 0, pageW, pageH));
        stripper.extractRegions(page);
        return stripper.getTextForRegion("page");
    }
}
