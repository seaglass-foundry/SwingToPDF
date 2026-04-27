package com.seaglassfoundry.swingtopdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Regression test: when a panel containing several stacked JTables spans
 * multiple pages, only the column header of a table that is still continuing
 * onto a continuation page should be repeated. A table that already finished
 * on an earlier page must not have its header stamped onto later pages.
 *
 * <p>The two tables use distinctive header background colours so that the
 * rendered top-of-content band on each continuation page can be sampled to
 * tell which (if any) header was drawn there. Text-stream extraction would
 * be unreliable here because clipped natural-traversal text is still emitted
 * to the content stream.
 */
class MultiTableRepeatedHeaderTest {

    private static final Color ALPHA_HDR = new Color(220, 30, 30);   // distinctive red
    private static final Color BETA_HDR  = new Color(30, 140, 220);  // distinctive blue

    @Test
    void finishedTableHeader_doesNotRepeatOnLaterPages(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTable tableA = makeTable(new String[]{"AlphaSku", "AlphaName"}, 6, "A", ALPHA_HDR);
        JTable tableB = makeTable(new String[]{"BetaSku", "BetaName"}, 120, "B", BETA_HDR);

        JScrollPane spA = wrap(tableA);
        JScrollPane spB = wrap(tableB);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.add(spA);
        root.add(spB);
        Dimension pref = root.getPreferredSize();
        root.setSize(pref.width, pref.height);
        root.validate();

        Path out = tmp.resolve("multi-table.pdf");
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages())
                    .as("test setup must produce multiple pages")
                    .isGreaterThan(1);

            PDFRenderer renderer = new PDFRenderer(doc);

            // Sample the top-of-content band on each continuation page. The
            // band sits just below the top margin (default 36pt). Look at a
            // strip ~10pt tall starting 4pt below the top margin.
            for (int p = 1; p < doc.getNumberOfPages(); p++) {
                BufferedImage img = renderer.renderImageWithDPI(p, 150f);
                float pxPerPt = img.getHeight() / 842f;
                int topY    = Math.round((36f + 4f)  * pxPerPt);
                int bottomY = Math.round((36f + 14f) * pxPerPt);
                int leftX   = Math.round(36f * pxPerPt);
                int rightX  = img.getWidth() - leftX;

                long alphaPx = countMatching(img, leftX, rightX, topY, bottomY, ALPHA_HDR);
                long betaPx  = countMatching(img, leftX, rightX, topY, bottomY, BETA_HDR);

                assertThat(alphaPx)
                        .as("page %d: top band must not contain AlphaSku header (red) ink -- "
                          + "table A finished on page 1 and should not repeat", p + 1)
                        .isZero();
                assertThat(betaPx)
                        .as("page %d: top band must contain BetaSku header (blue) ink -- "
                          + "table B is still continuing", p + 1)
                        .isGreaterThan(100);
            }
        }
    }

    private static JTable makeTable(String[] cols, int rows, String prefix, Color headerBg) {
        Object[][] data = new Object[rows][cols.length];
        for (int i = 0; i < rows; i++) {
            for (int c = 0; c < cols.length; c++) data[i][c] = prefix + "-" + i + "-" + c;
        }
        JTable t = new JTable(new DefaultTableModel(data, cols));
        t.setRowHeight(22);
        t.getTableHeader().setBackground(headerBg);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setOpaque(true);
        return t;
    }

    private static JScrollPane wrap(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(500,
                30 + t.getModel().getRowCount() * t.getRowHeight()));
        return sp;
    }

    /** Count pixels in the strip whose RGB is close to the target colour. */
    private static long countMatching(BufferedImage img, int x0, int x1, int y0, int y1, Color target) {
        long count = 0;
        int tr = target.getRed(), tg = target.getGreen(), tb = target.getBlue();
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int rgb = img.getRGB(x, y);
                int rr = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, bb = rgb & 0xFF;
                if (Math.abs(rr - tr) < 35 && Math.abs(gg - tg) < 35 && Math.abs(bb - tb) < 35) count++;
            }
        }
        return count;
    }
}
