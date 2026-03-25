package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.seaglassfoundry.swingtopdf.api.ExportMode;

/**
 * Renders a {@link JList} as PDF vector graphics.
 *
 * <h3>DATA_REPORT mode</h3>
 * All items from the list model are rendered, regardless of scroll position.
 *
 * <h3>UI_SNAPSHOT mode</h3>
 * Only items that fit within the component's current height are rendered.
 *
 * <p>Cell height is taken from {@link JList#getFixedCellHeight()} if set;
 * otherwise the renderer's preferred height is used, with a fallback default.
 * The registered {@link ListCellRenderer} drives background colour and text.
 */
final class JListHandler implements ComponentHandler {

    static final JListHandler INSTANCE = new JListHandler();

    private JListHandler() {}

    /** Fallback cell height (px) when the renderer has no preferred size. */
    private static final int DEFAULT_CELL_HEIGHT = 16;

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        @SuppressWarnings("unchecked")
        JList<Object> list = (JList<Object>) comp;

        // Background
        if (list.isOpaque() && list.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, list.getWidth(), list.getHeight(),
                                  list.getBackground());
        }

        // Border (list itself rarely has one but handle it anyway)
        ContainerHandler.renderBorderOnly(list, absX, absY, ctx);

        int total    = list.getModel().getSize();
        boolean full = ctx.config().exportMode() == ExportMode.DATA_REPORT;
        int limit    = full ? total : visibleCount(list);

        float sliceTop    = ctx.sliceTopPx();
        float sliceBottom = ctx.sliceBottomPx();

        int curY = absY;
        for (int i = 0; i < limit && i < total; i++) {
            Object value = list.getModel().getElementAt(i);

            ListCellRenderer<Object> renderer =
                    list.getCellRenderer();
            Component cell = renderer.getListCellRendererComponent(
                    list, value, i, false, false);

            int cellH = cellHeight(list, cell);
            int cellW = list.getWidth();

            // Skip items entirely above the current page slice
            if (curY + cellH <= sliceTop) { curY += cellH; continue; }
            // Stop once items are entirely below the current page slice
            if (curY >= sliceBottom) break;

            if (cell instanceof JLabel label) {
                // Delegate fully to JLabelHandler  -- handles background, icon, and text
                label.setSize(cellW, cellH);
                JLabelHandler.INSTANCE.render(label, absX, curY, ctx);
            } else {
                // Non-JLabel renderer: fill background and draw text from value
                Color bg = cell.getBackground();
                if (bg == null) bg = list.getBackground();
                if (bg != null) ctx.writer().fillRect(absX, curY, cellW, cellH, bg);
                renderFallbackText(cell, value, absX, curY, cellW, cellH, list, ctx);
            }

            curY += cellH;
        }
    }

    // -----------------------------------------------------------------------

    private void renderFallbackText(Component cell, Object value,
                                     int cellX, int cellY, int cellW, int cellH,
                                     JList<?> list, HandlerContext ctx) throws IOException {
        String text = value != null ? value.toString() : null;
        if (text == null || text.isBlank()) return;

        Font  font = cell.getFont();
        Color fg   = cell.getForeground();
        if (font == null) font = list.getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        if (fg   == null) fg   = list.getForeground();
        if (fg   == null) fg   = Color.BLACK;

        FontMetrics fm        = cell.getFontMetrics(font);
        int         ascent    = fm.getAscent();
        int         textH     = ascent + fm.getDescent();
        int         baselineY = cellY + (cellH - textH) / 2 + ascent;

        ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                              font.getSize2D(), fg, cellX + 2, baselineY);
    }

    private static int cellHeight(JList<?> list, Component cell) {
        int fixed = list.getFixedCellHeight();
        if (fixed > 0) return fixed;
        Dimension pref = cell.getPreferredSize();
        return pref != null && pref.height > 0 ? pref.height : DEFAULT_CELL_HEIGHT;
    }

    private static int visibleCount(JList<?> list) {
        int total = list.getModel().getSize();
        int limit = list.getHeight();
        int cumH  = 0;
        for (int i = 0; i < total; i++) {
            cumH += list.getFixedCellHeight() > 0
                    ? list.getFixedCellHeight()
                    : DEFAULT_CELL_HEIGHT;
            if (cumH > limit) return i;
        }
        return total;
    }
}
