package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.UIManager;

/**
 * Renders a {@link JSpinner} as PDF vector graphics.
 *
 * <p>Draws the spinner's editor text value in the left area and a column of
 * two small chevron arrows (increment on top, decrement on bottom) on the
 * right  -- matching the standard visual appearance without relying on the
 * L&amp;F's {@code BasicArrowButton} rendering.
 *
 * <p>The editor text field is resolved via {@link JSpinner.DefaultEditor#getTextField()};
 * rendering is delegated to {@link JTextComponentHandler} so that the font,
 * foreground, border, and insets are respected exactly.  If the editor is a
 * custom non-default type, the spinner's current value is rendered as plain text.
 */
final class JSpinnerHandler implements ComponentHandler {

    static final JSpinnerHandler INSTANCE = new JSpinnerHandler();

    private JSpinnerHandler() {}

    /** Default arrow column width when the actual button cannot be measured. */
    private static final int DEFAULT_ARROW_W = 16;

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JSpinner spinner)) return;
        int w = spinner.getWidth();
        int h = spinner.getHeight();
        if (w <= 0 || h <= 0) return;

        int arrowW = measureArrowWidth(spinner);

        // 1. Background
        Color bg = spinner.getBackground() != null ? spinner.getBackground() : Color.WHITE;
        ctx.writer().fillRect(absX, absY, w, h, bg);

        // 2. Border (use component border; fall back to a simple 1px outline)
        ContainerHandler.renderBorderOnly(spinner, absX, absY, ctx);
        if (spinner.getBorder() == null) {
            ctx.writer().strokeRect(absX, absY, w, h, Color.GRAY, 1f);
        }

        // 3. Arrow column  -- two halves separated by a horizontal midline
        int arrowColX = absX + w - arrowW;
        int midH      = h / 2;
        Color arrowBg = uiColor("Spinner.arrowButtonBackground",
                uiColor("Button.background", Color.LIGHT_GRAY));
        Color border  = uiColor("Spinner.border", Color.GRAY);
        ctx.writer().fillRect(arrowColX, absY,        arrowW, midH,     arrowBg);
        ctx.writer().fillRect(arrowColX, absY + midH, arrowW, h - midH, arrowBg);
        ctx.writer().drawLine(arrowColX, absY,        arrowColX, absY + h,             border, 0.5f);
        ctx.writer().drawLine(arrowColX, absY + midH, arrowColX + arrowW, absY + midH, border, 0.5f);
        drawChevron(arrowColX, absY,        arrowW, midH,     true,  ctx);
        drawChevron(arrowColX, absY + midH, arrowW, h - midH, false, ctx);

        // 4. Editor  -- prefer the JFormattedTextField inside DefaultEditor
        int editorW = w - arrowW;
        JFormattedTextField field = resolveTextField(spinner.getEditor());
        if (field != null) {
            field.setSize(editorW, h);
            JTextComponentHandler.INSTANCE.render(field, absX, absY, ctx);
        } else {
            // Custom editor: render the value as plain text
            String text = spinner.getValue() != null ? spinner.getValue().toString() : "";
            renderFallbackText(text, spinner, absX, absY, editorW, h, ctx);
        }
    }

    // -----------------------------------------------------------------------

    /** Extracts the {@link JFormattedTextField} from a spinner editor. */
    private static JFormattedTextField resolveTextField(JComponent editor) {
        if (editor instanceof JSpinner.DefaultEditor de) {
            return de.getTextField();
        }
        // Walk direct children for custom editors that embed a text field
        for (int i = 0; i < editor.getComponentCount(); i++) {
            if (editor.getComponent(i) instanceof JFormattedTextField tf) return tf;
        }
        return null;
    }

    private static void renderFallbackText(String text, JSpinner spinner,
                                            int absX, int absY, int w, int h,
                                            HandlerContext ctx) throws IOException {
        if (text == null || text.isBlank()) return;
        Font  font = spinner.getFont();
        Color fg   = spinner.getForeground();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        if (fg   == null) fg   = Color.BLACK;
        FontMetrics fm        = spinner.getFontMetrics(font);
        int         ascent    = fm.getAscent();
        int         textH     = ascent + fm.getDescent();
        int         baselineY = absY + (h - textH) / 2 + ascent;
        ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                              font.getSize2D(), fg, absX + 3, baselineY);
    }

    /** Two-line "^" or "v" chevron centred in the arrow half-cell. */
    private static void drawChevron(int x, int y, int w, int h, boolean up,
                                     HandlerContext ctx) throws IOException {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int hw = 3, hh = 2;
        Color c = Color.DARK_GRAY;
        if (up) {
            ctx.writer().drawLine(cx - hw, cy + hh, cx,      cy - hh, c, 1.5f);
            ctx.writer().drawLine(cx,      cy - hh, cx + hw, cy + hh, c, 1.5f);
        } else {
            ctx.writer().drawLine(cx - hw, cy - hh, cx,      cy + hh, c, 1.5f);
            ctx.writer().drawLine(cx,      cy + hh, cx + hw, cy - hh, c, 1.5f);
        }
    }

    private static Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    /**
     * Measure the arrow button width from the spinner's children.
     * Most L&amp;Fs add two arrow buttons after the editor component.
     */
    private static int measureArrowWidth(JSpinner spinner) {
        // Arrow buttons are typically the last children (after the editor)
        for (int i = spinner.getComponentCount() - 1; i >= 0; i--) {
            Component child = spinner.getComponent(i);
            if (child.getWidth() > 0 && child.getHeight() > 0
                    && child != spinner.getEditor()) {
                return child.getWidth();
            }
        }
        return DEFAULT_ARROW_W;
    }
}
