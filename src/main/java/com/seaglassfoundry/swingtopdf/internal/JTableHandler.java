package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seaglassfoundry.swingtopdf.api.ExportMode;

/**
 * Renders a {@link JTable} as PDF vector graphics.
 *
 * <h3>DATA_REPORT mode</h3>
 * All model rows are rendered regardless of the viewport's current scroll
 * position. This allows the full table content to flow across multiple PDF
 * pages when the root component (and therefore the paginator) is sized to
 * accommodate the complete table height.
 *
 * <h3>UI_SNAPSHOT mode</h3>
 * Only rows that fit within the component's current height are rendered,
 * matching what is visible on screen.
 *
 * <h3>Column header</h3>
 * The {@link javax.swing.table.JTableHeader} is handled separately by
 * {@link JTableHeaderHandler}, which is visited by the traverser from the
 * scroll pane's column-header viewport at the correct absolute position.
 *
 * <h3>Cell values</h3>
 * The table's registered {@link TableCellRenderer} is called for each cell.
 * When it returns a {@link JLabel} (the default), text, font, foreground
 * colour, and horizontal alignment are extracted directly. Other renderers
 * fall back to {@code value.toString()}.
 */
final class JTableHandler implements ComponentHandler {

    static final JTableHandler INSTANCE = new JTableHandler();

    private static final Logger log = LoggerFactory.getLogger(JTableHandler.class);

    private JTableHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JTable table = (JTable) comp;

        boolean fullData = ctx.config().exportMode() == ExportMode.DATA_REPORT;
        int rowCount = fullData
                ? table.getModel().getRowCount()
                : visibleRowCount(table);

        // Detect whether the table header needs to repeat on this page.
        // This happens in DATA_REPORT mode when the table started on a previous
        // page (absY < sliceTopPx). The header is pushed to the slice top and
        // all rows on this page are shifted down by the header height so they
        // don't overlap with the repeated header.
        int headerRepeatH = 0;
        if (fullData) {
            javax.swing.table.JTableHeader header = table.getTableHeader();
            if (header != null && absY < ctx.sliceTopPx()) {
                headerRepeatH = header.getHeight();
                JTableHeaderHandler.renderAt(header, absX, (int) ctx.sliceTopPx(), ctx);
            }
        }

        renderRows(table, absX, absY, rowCount, ctx.sliceTopPx(), headerRepeatH, ctx);
    }

    // -----------------------------------------------------------------------

    private void renderRows(JTable table, int absX, int absY, int rowCount,
                             float sliceTopPx, int headerRepeatH,
                             HandlerContext ctx) throws IOException {
        int       colCount  = table.getColumnCount();

        int totalColW = 0;
        for (int col = 0; col < colCount; col++)
            totalColW += table.getColumnModel().getColumn(col).getWidth();
        float availW   = Math.max(1, ctx.pageWidthPx() - absX);
        float colScale = (totalColW > availW) ? availW / totalColW : 1f;
        if (colScale < 1f)
            log.warn("JTable ({} cols, {}px) scaled to {}% to fit {}px",
                     colCount, totalColW, Math.round(colScale * 100), (int) availW);

        Color     gridColor = table.getGridColor();
        boolean   showH     = table.getShowHorizontalLines();
        boolean   showV     = table.getShowVerticalLines();

        int curY = absY;      // natural (unshifted) cursor
        int maxRenderY = absY;

        float sliceBottom = ctx.sliceBottomPx();

        // Collect horizontal grid line Y-positions so they can be drawn after
        // all cell backgrounds, preventing the next row's fillRect from
        // overwriting a previously drawn grid line.
        int[] hLineYs = new int[Math.max(rowCount - 1, 0)];
        int   hLineCount = 0;

        for (int row = 0; row < rowCount; row++) {
            int rowH    = table.getRowHeight(row);
            // Shift rows that fall on a continuation page down by headerRepeatH
            int renderY = (headerRepeatH > 0 && curY >= sliceTopPx)
                          ? curY + headerRepeatH : curY;

            // Skip rows entirely above the current page slice
            if (renderY + rowH <= sliceTopPx) { curY += rowH; continue; }
            // Stop once rows are entirely below the current page slice
            if (renderY >= sliceBottom) break;

            int curX    = absX;

            for (int col = 0; col < colCount; col++) {
                int colW = (int)(table.getColumnModel().getColumn(col).getWidth() * colScale);

                // Ask the renderer for the cell component via prepareRenderer() so
                // that any JTable subclass override (e.g. custom foreground colours)
                // is applied before we read the component's attributes.
                TableCellRenderer renderer = table.getCellRenderer(row, col);
                Component         cellComp = table.prepareRenderer(renderer, row, col);

                // Size the renderer component so handlers that call getWidth()/getHeight()
                // (e.g. AbstractButtonHandler) position their content correctly.
                cellComp.setSize(colW, rowH);

                // Background
                Color bg = cellComp.getBackground();
                if (bg == null) bg = table.getBackground();
                if (bg != null) ctx.writer().fillRect(curX, renderY, colW, rowH, bg);

                // Render cell content: vector for known types, rasterize for others.
                if (cellComp instanceof JLabel label) {
                    // Delegate to JLabelHandler  -- handles background, icon, HTML, and text
                    JLabelHandler.INSTANCE.render(label, curX, renderY, ctx);
                } else if (cellComp instanceof AbstractButton btn) {
                    // Boolean columns use JCheckBox renderers; render as vector.
                    AbstractButtonHandler.INSTANCE.render(btn, curX, renderY, ctx);
                } else {
                    // Custom renderer  -- rasterize at cell dimensions (no WARN,
                    // this is expected behaviour for unknown renderers).
                    RasterFallback.renderSized(cellComp, colW, rowH, curX, renderY, ctx);
                }

                curX += colW;
            }

            // Record horizontal grid line position (skip last row)
            if (showH && row < rowCount - 1) {
                hLineYs[hLineCount++] = renderY + rowH;
            }

            maxRenderY = renderY + rowH;
            curY += rowH;
        }

        // Horizontal grid lines (deferred so they paint on top of cell backgrounds)
        if (showH) {
            int tableRenderW = (int)(totalColW * colScale);
            for (int i = 0; i < hLineCount; i++) {
                ctx.writer().drawLine(absX, hLineYs[i],
                                      absX + tableRenderW, hLineYs[i],
                                      gridColor, 1f);
            }
        }

        // Vertical grid lines (drawn after all rows to avoid paint-order artefacts)
        if (showV) {
            int vTop = (headerRepeatH > 0) ? (int) sliceTopPx + headerRepeatH : absY;
            int colX = absX;
            for (int col = 0; col < colCount - 1; col++) {
                colX += (int)(table.getColumnModel().getColumn(col).getWidth() * colScale);
                ctx.writer().drawLine(colX, vTop, colX, maxRenderY, gridColor, 1f);
            }
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Count of rows that fit within the component's current height (UI_SNAPSHOT).
     */
    private static int visibleRowCount(JTable table) {
        int limit  = table.getHeight();
        int cumH   = 0;
        int total  = table.getModel().getRowCount();
        for (int r = 0; r < total; r++) {
            cumH += table.getRowHeight(r);
            if (cumH > limit) return r;
        }
        return total;
    }

}
