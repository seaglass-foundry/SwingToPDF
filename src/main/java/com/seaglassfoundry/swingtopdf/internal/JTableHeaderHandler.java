package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Renders a {@link JTableHeader} as PDF vector graphics.
 *
 * <p>For each column: fills the cell background, draws the header label text
 * with the renderer's font and foreground, and adds column separators.
 * A horizontal rule is drawn at the bottom of the header row.
 *
 * <p>This handler is visited naturally by the {@link ComponentTraverser} when
 * the {@code JTableHeader} sits inside the scroll pane's column-header viewport.
 */
final class JTableHeaderHandler implements ComponentHandler {

    static final JTableHeaderHandler INSTANCE = new JTableHeaderHandler();

    private JTableHeaderHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JTableHeader header)) return;
        JTable table = header.getTable();
        if (table == null) return;
        renderAt(header, absX, absY, ctx);
    }

    /**
     * Render the header at an explicit absolute position.
     * Called by {@link JTableHandler} to repeat the header on continuation pages.
     */
    static void renderAt(JTableHeader header, int absX, int absY,
                          HandlerContext ctx) throws IOException {
        JTable table = header.getTable();
        if (table == null) return;

        int headerH  = header.getHeight();
        int colCount = table.getColumnCount();

        int totalW = 0;
        for (int col = 0; col < colCount; col++)
            totalW += table.getColumnModel().getColumn(col).getWidth();
        float availW   = Math.max(1, ctx.pageWidthPx() - absX);
        float colScale = (totalW > availW) ? availW / totalW : 1f;

        // Background
        Color headerBg = header.getBackground();
        if (headerBg == null) headerBg = Color.LIGHT_GRAY;
        ctx.writer().fillRect(absX, absY, (int)(totalW * colScale), headerH, headerBg);

        // Per-column cells
        int curX = absX;
        for (int col = 0; col < colCount; col++) {
            TableColumn tc = table.getColumnModel().getColumn(col);
            int colW = (int)(tc.getWidth() * colScale);

            // Renderer component
            TableCellRenderer renderer = tc.getHeaderRenderer();
            if (renderer == null) renderer = header.getDefaultRenderer();
            Component cellComp = renderer.getTableCellRendererComponent(
                    table, tc.getHeaderValue(), false, false, -1, col);

            if (cellComp instanceof JLabel label) {
                // If the renderer label has no text, fall back to the column's header value
                if (label.getText() == null || label.getText().isBlank()) {
                    Object hv = tc.getHeaderValue();
                    if (hv != null) label.setText(hv.toString());
                }
                // Delegate fully to JLabelHandler  -- handles background, icon, and text
                label.setSize(colW, headerH);
                JLabelHandler.INSTANCE.render(label, curX, absY, ctx);
            } else {
                // Non-JLabel header renderer: fill background and draw fallback text
                Color cellBg = cellComp.getBackground();
                if (cellBg != null && !cellBg.equals(headerBg)) {
                    ctx.writer().fillRect(curX, absY, colW, headerH, cellBg);
                }
                Object hv   = tc.getHeaderValue();
                String text = hv != null ? hv.toString() : null;
                if (text != null && !text.isBlank()) {
                    Font        font      = header.getFont() != null ? header.getFont()
                                                    : new Font(Font.SANS_SERIF, Font.BOLD, 11);
                    Color       fg        = header.getForeground() != null ? header.getForeground() : Color.BLACK;
                    FontMetrics fm        = header.getFontMetrics(font);
                    int         ascent    = fm.getAscent();
                    int         textH     = ascent + fm.getDescent();
                    int         textW     = fm.stringWidth(text);
                    int         textX     = curX + (colW - textW) / 2;
                    int         baselineY = absY + (headerH - textH) / 2 + ascent;
                    ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                                          font.getSize2D(), fg, textX, baselineY);
                }
            }

            // Column separator (skip last)
            if (col < colCount - 1) {
                Color sep = table.getGridColor() != null ? table.getGridColor() : Color.GRAY;
                ctx.writer().drawLine(curX + colW, absY, curX + colW, absY + headerH, sep, 1f);
            }

            curX += colW;
        }

        // Bottom rule separating header from data
        Color borderColor = table.getGridColor() != null ? table.getGridColor() : Color.GRAY;
        ctx.writer().drawLine(absX, absY + headerH,
                              absX + (int)(totalW * colScale), absY + headerH, borderColor, 1f);
    }
}
