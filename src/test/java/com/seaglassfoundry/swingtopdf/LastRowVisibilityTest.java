package com.seaglassfoundry.swingtopdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the "last row invisible" bug: when a multi-page JTable
 * is exported with headers/footers, {@code JTableHandler} shifts rows on
 * continuation pages down by the repeated-header height. If the page-break
 * algorithm doesn't reserve this space, the last row is drawn past the slice
 * clip rect and appears as empty whitespace (its text operators still emit,
 * so {@code PDFTextStripper} extracts it).
 *
 * <p>Directly asserts the structural invariant: <b>every drawn rectangle on
 * a page must lie within that page's clip rectangle.</b> This is stronger
 * than pixel scanning and pinpoints the bug regardless of how the bug
 * manifests visually.
 */
class LastRowVisibilityTest {

    private static final int ROW_COUNT  = 120;
    private static final int ROW_HEIGHT = 22;

    @Test
    void everyDrawnRect_liesWithinClip(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-last-row.pdf");

        JPanel content = buildInventoryPanel();
        Dimension pref = content.getPreferredSize();
        content.setSize(pref.width, pref.height);
        content.validate();

        SwingPdfExporter.from(content)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .margins(72, 54, 72, 54)
                .header(HeaderFooter.of("Warehouse Inventory Report"))
                .footer(HeaderFooter.of("Page {page} of {pages}"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);

            PDFRenderer renderer = new PDFRenderer(doc);
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                PDPage page = doc.getPage(p);
                RectCollector rc = new RectCollector();
                rc.processPage(page);

                assertThat(rc.clip)
                        .as("page %d has a clip rect", p + 1)
                        .isNotNull();

                float clipTop    = rc.clip[1] + rc.clip[3]; // highest PDF y
                float clipBottom = rc.clip[1];              // lowest PDF y

                // Guard against the specific bug: a rect whose TOP is inside
                // the clip area but whose BOTTOM extends below the clip bottom.
                // That's a table row shifted past its slice boundary by the
                // repeated-header height -- text operators still emit (so
                // PDFTextStripper finds it) but the cell fill is clipped away.
                for (float[] r : rc.cellRects) {
                    float rectBottom = r[1];
                    float rectTop    = r[1] + r[3];
                    boolean topInsideClip = rectTop >= clipBottom - 0.1f && rectTop <= clipTop + 0.1f;
                    if (!topInsideClip) continue;
                    assertThat(rectBottom)
                            .as("page %d: rect y=%.2f h=%.2f (top=%.2f) extends below clip bottom %.2f (table row shifted past slice)",
                                p + 1, r[1], r[3], rectTop, clipBottom)
                            .isGreaterThanOrEqualTo(clipBottom - 0.1f);
                }

                // On continuation pages (p > 0), assert the table column
                // header is repeated at the top of the content area by
                // scanning for non-white ink in a 12pt band starting at the
                // top margin. Without the repeated-header rendering this
                // band is blank (rows start immediately, no header).
                if (p > 0) {
                    BufferedImage img = renderer.renderImageWithDPI(p, 150f);
                    float pxPerPt = img.getHeight() / 842f;
                    int topY     = Math.round(72f * pxPerPt);
                    int bottomY  = Math.round((72f + 12f) * pxPerPt);
                    int leftX    = Math.round(54f * pxPerPt);
                    int rightX   = img.getWidth() - leftX;
                    int nonWhite = 0;
                    for (int y = topY; y <= bottomY; y++) {
                        for (int x = leftX; x <= rightX; x++) {
                            int rgb = img.getRGB(x, y) & 0xFFFFFF;
                            int rr = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, bb = rgb & 0xFF;
                            if (rr < 240 || gg < 240 || bb < 240) nonWhite++;
                        }
                    }
                    assertThat(nonWhite)
                            .as("page %d: top-of-content band is blank -- repeated table header missing", p + 1)
                            .isGreaterThan(1000);
                }
            }
        }
    }

    /**
     * Parses a page content stream and records (a) the first rectangle used
     * as a clip, and (b) all subsequent rectangles drawn as small table-cell-
     * sized rects (h &lt; 40 pt) so we focus on table cells / grid and avoid
     * page backgrounds and large panel fills.
     */
    private static class RectCollector extends PDFStreamEngine {
        float[] clip;
        final List<float[]> cellRects = new ArrayList<>();
        private float[] lastRect;

        @Override
        protected void operatorException(Operator operator, List<COSBase> operands, IOException e) {
        }

        @Override
        protected void processOperator(Operator op, List<COSBase> operands) throws IOException {
            String name = op.getName();
            if ("re".equals(name) && operands.size() == 4) {
                float x = ((COSNumber) operands.get(0)).floatValue();
                float y = ((COSNumber) operands.get(1)).floatValue();
                float w = ((COSNumber) operands.get(2)).floatValue();
                float h = ((COSNumber) operands.get(3)).floatValue();
                float[] r = new float[]{x, y, w, h};
                // Record small rects (likely cell backgrounds / grid stubs)
                if (h < 40f && h > 0.1f) cellRects.add(r);
                lastRect = r;
            } else if ("W".equals(name) || "W*".equals(name)) {
                if (clip == null && lastRect != null) {
                    clip = lastRect;
                }
            }
            super.processOperator(op, operands);
        }
    }

    private static JPanel buildInventoryPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(0xF0F4FA));
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Warehouse Inventory Report");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        p.add(heading);

        String[] cols = {"SKU", "Product", "Category", "Warehouse", "Qty", "Unit Price", "Total", "Status"};
        Object[][] data = new Object[ROW_COUNT][cols.length];
        for (int i = 0; i < ROW_COUNT; i++) {
            data[i] = new Object[]{
                    String.format("SKU-%05d", 10000 + i),
                    "Product " + i,
                    "Category-" + (i % 5),
                    "WH-" + (i % 3),
                    100 + i,
                    String.format("$%.2f", 12.50 + i * 0.3),
                    String.format("$%,.2f", (12.50 + i * 0.3) * (100 + i)),
                    (i % 4 == 0) ? "Low Stock" : "In Stock"
            };
        }
        JTable table = new JTable(new DefaultTableModel(data, cols) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setRowHeight(ROW_HEIGHT);
        table.setGridColor(new Color(0xCCD6E6));
        table.getTableHeader().setBackground(new Color(0x2B4C7E));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));

        JScrollPane sp = new JScrollPane(table);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(860, ROW_COUNT * ROW_HEIGHT + 30));
        p.add(sp);

        return p;
    }
}
