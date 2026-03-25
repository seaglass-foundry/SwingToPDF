package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Renders a {@link JComboBox} as PDF vector graphics.
 *
 * <p>Shows the currently selected item text in a framed box with a simple
 * downward-arrow indicator on the right  -- the same visual footprint as an
 * on-screen combo box.  The drop-down list is not expanded; only the selected
 * value is recorded in the PDF (appropriate for both modes since a combo is a
 * selection control, not a data container).
 */
final class JComboBoxHandler implements ComponentHandler {

    static final JComboBoxHandler INSTANCE = new JComboBoxHandler();

    private JComboBoxHandler() {}

    /** Default arrow column width when the actual arrow button cannot be measured. */
    private static final int DEFAULT_ARROW_W = 18;

    /**
     * Dummy JList used to satisfy BasicComboBoxRenderer, which requires a non-null
     * JList to inherit background/foreground colours from.
     */
    private static final JList<?> DUMMY_LIST = new JList<>();

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        @SuppressWarnings("unchecked")
        JComboBox<Object> cb = (JComboBox<Object>) comp;
        int w = cb.getWidth();
        int h = cb.getHeight();

        // When AcroForm is active the widget appearance (rendered by the PDF viewer)
        // draws the full combo box  -- background, border, text, and arrow  -- so skip
        // all vector drawing to avoid a double-arrow artefact.
        if (ctx.acroFormEmitter() != null) {
            ctx.acroFormEmitter().addComboBox(cb, absX, absY, ctx.currentPage(), ctx.writer());
            return;
        }

        int arrowW = measureArrowWidth(cb);

        // 1. Background
        Color bg = cb.getBackground() != null ? cb.getBackground() : Color.WHITE;
        ctx.writer().fillRect(absX, absY, w, h, bg);

        // 2. Arrow column indicator
        int arrowColX = absX + w - arrowW;
        drawArrow(arrowColX, absY, arrowW, h, ctx);

        // 4. Selected item text (via the renderer)
        Object   selected = cb.getSelectedItem();
        int      selIdx   = cb.getSelectedIndex();
        ListCellRenderer<Object> renderer = cb.getRenderer();
        Component cell = renderer.getListCellRendererComponent(DUMMY_LIST, selected, selIdx, false, false);

        // The renderer used DUMMY_LIST's fg/bg for the non-selected state.
        // Override with the combo box's own colours so the PDF matches the
        // on-screen display area (not the drop-down list).
        if (cb.getForeground() != null) cell.setForeground(cb.getForeground());
        if (cb.getBackground() != null) cell.setBackground(cb.getBackground());

        if (cell instanceof JLabel label) {
            // Size the label to the text area (excluding the arrow column) and delegate
            label.setSize(w - arrowW, h);
            JLabelHandler.INSTANCE.render(label, absX, absY, ctx);
        } else {
            // Non-JLabel renderer: draw text from selected item
            String text = selected != null ? selected.toString() : null;
            if (text == null || text.isBlank()) return;

            Font  font = cell.getFont();
            Color fg   = cell.getForeground();
            if (font == null) font = cb.getFont();
            if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            if (fg   == null) fg   = cb.getForeground();
            if (fg   == null) fg   = Color.BLACK;

            FontMetrics fm        = cb.getFontMetrics(font);
            int         ascent    = fm.getAscent();
            int         textH     = ascent + fm.getDescent();
            int         baselineY = absY + (h - textH) / 2 + ascent;
            ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                                  font.getSize2D(), fg, absX + 3, baselineY);
        }

        // 5. Outer border and arrow column separator — drawn after the label
        //    so they are not covered by the label renderer's background fill.
        ctx.writer().strokeRect(absX, absY, w, h, Color.GRAY, 1f);
        ctx.writer().drawLine(arrowColX, absY, arrowColX, absY + h, Color.GRAY, 0.5f);
    }

    // -----------------------------------------------------------------------

    /** Draw a simple downward-pointing "v" arrow centred in the arrow column. */
    private static void drawArrow(int colX, int colY, int colW, int colH,
                                   HandlerContext ctx) throws IOException {
        int cx  = colX + colW / 2;
        int mid = colY + colH / 2;
        int hw  = 4;   // half-width of the arrow base
        int hh  = 3;   // half-height of the arrow

        Color arrowColor = Color.DARK_GRAY;
        ctx.writer().drawLine(cx - hw, mid - hh, cx,      mid + hh, arrowColor, 1.5f);
        ctx.writer().drawLine(cx,      mid + hh, cx + hw, mid - hh, arrowColor, 1.5f);
    }

    /**
     * Measure the arrow button width from the combo's last child component
     * (the arrow button in most L&amp;Fs), falling back to a sensible default.
     */
    private static int measureArrowWidth(JComboBox<?> cb) {
        // Most L&Fs add the arrow button as the last child component.
        // In headless / forced layout the button can end up with an inflated
        // width (sometimes the full combo width), so cap it at the combo
        // height  -- the arrow button is visually square in every standard L&F.
        int maxW = Math.max(cb.getHeight(), DEFAULT_ARROW_W);
        int n = cb.getComponentCount();
        if (n > 0) {
            Component last = cb.getComponent(n - 1);
            int w = last.getWidth();
            if (w > 0) return Math.min(w, maxW);
        }
        return DEFAULT_ARROW_W;
    }
}
