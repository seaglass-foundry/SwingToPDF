package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Renders {@link AbstractButton} subclasses as PDF vector graphics.
 *
 * <ul>
 *   <li><b>{@code JButton} / {@code JToggleButton}</b>  -- background fill, border, centred text.</li>
 *   <li><b>{@code JCheckBox}</b>  -- square indicator (stroked rect + checkmark when selected),
 *       followed by label text.</li>
 *   <li><b>{@code JRadioButton}</b>  -- circular indicator (stroked ellipse + filled inner circle
 *       when selected), followed by label text.</li>
 * </ul>
 *
 * <p>HTML text is stripped to plain text. Disabled state uses a brighter foreground.
 * Icons other than the built-in indicator are not rendered (future enhancement).
 */
final class AbstractButtonHandler implements ComponentHandler {

    static final AbstractButtonHandler INSTANCE = new AbstractButtonHandler();

    private AbstractButtonHandler() {}

    private static final int FALLBACK_IND_SIZE  = 13;

    /** Inset padding (px) for the filled circle inside a selected radio button. */
    private static final int RADIO_FILL_PAD     = 3;

    /** Returns the checkbox/radio indicator size, querying the current L&F icon if available. */
    static int indicatorSize() {
        Icon icon = UIManager.getIcon("CheckBox.icon");
        if (icon != null && icon.getIconWidth() > 0) return icon.getIconWidth();
        return FALLBACK_IND_SIZE;
    }

    private static final int IND_SIZE = indicatorSize();

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof AbstractButton btn)) return;

        // 1. Background
        Color bg = btn.getBackground();
        if (btn instanceof JToggleButton && !(btn instanceof JCheckBox) && !(btn instanceof JRadioButton) && btn.isSelected()) {
            Color sel = UIManager.getColor("ToggleButton.select");
            if (sel != null) bg = sel;
            else if (bg != null) bg = bg.darker();
        }
        if (btn.isOpaque() && bg != null) {
            ctx.writer().fillRect(absX, absY, btn.getWidth(), btn.getHeight(), bg);
        }

        // 2. Border (skip for JCheckBox / JRadioButton — their L&F borders are
        //    paint artifacts that Swing's paintComponent overdraws; rendering them
        //    here produces a spurious bevel around the entire component)
        if (!(btn instanceof JCheckBox) && !(btn instanceof JRadioButton)) {
            ContainerHandler.renderBorderOnly(btn, absX, absY, ctx);
        }

        // 3. Content
        if (btn instanceof JCheckBox cb) {
            if (ctx.acroFormEmitter() != null) {
                // AcroForm widget appearance owns the indicator; skip vector indicator
                // drawing so the interactive appearance and the ink don't conflict.
                // Still draw the label text to the right of the indicator.
                ctx.acroFormEmitter().addCheckBox(cb, absX, absY, ctx.currentPage(), ctx.writer());
                int textStartX = absX + btn.getInsets().left + IND_SIZE + btn.getIconTextGap();
                drawIndicatorLabel(btn, absX, absY, textStartX, ctx);
            } else {
                renderCheckBox(btn, absX, absY, ctx);
            }
        } else if (btn instanceof JRadioButton rb) {
            if (ctx.acroFormEmitter() != null) {
                ctx.acroFormEmitter().addRadioButton(rb, absX, absY, ctx.currentPage(), ctx.writer());
                int textStartX = absX + btn.getInsets().left + IND_SIZE + btn.getIconTextGap();
                drawIndicatorLabel(btn, absX, absY, textStartX, ctx);
            } else {
                renderRadioButton(btn, absX, absY, ctx);
            }
        } else {
            renderButtonText(btn, absX, absY, ctx);
        }
    }

    // -----------------------------------------------------------------------
    // JButton / JToggleButton
    // -----------------------------------------------------------------------

    private void renderButtonText(AbstractButton btn, int absX, int absY,
                                   HandlerContext ctx) throws IOException {
        String text = stripHtml(btn.getText());
        Icon   icon = btn.getIcon();

        if (icon == null && (text == null || text.isBlank())) return;

        Font  font = resolvedFont(btn);
        Color fg   = resolvedFg(btn);

        FontMetrics fm  = btn.getFontMetrics(font);
        Insets      ins = btn.getInsets();
        Rectangle   viewR = new Rectangle(
                ins.left, ins.top,
                btn.getWidth()  - ins.left - ins.right,
                btn.getHeight() - ins.top  - ins.bottom);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();

        String layoutText = (text != null && !text.isBlank()) ? text : null;
        SwingUtilities.layoutCompoundLabel(
                btn, fm,
                layoutText,
                icon,
                btn.getVerticalAlignment(),
                btn.getHorizontalAlignment(),
                btn.getVerticalTextPosition(),
                btn.getHorizontalTextPosition(),
                viewR, iconR, textR,
                icon == null ? 0 : btn.getIconTextGap());

        if (icon != null) {
            IconPainter.render(icon, btn, absX + iconR.x, absY + iconR.y, ctx);
        }
        if (layoutText != null) {
            int baselineY = absY + textR.y + fm.getAscent();
            ctx.writer().drawText(text, ctx.fontMapper().resolve(font), font.getSize2D(),
                                  fg, absX + textR.x, baselineY);
        }
    }

    // -----------------------------------------------------------------------
    // JCheckBox
    // -----------------------------------------------------------------------

    private void renderCheckBox(AbstractButton btn, int absX, int absY,
                                 HandlerContext ctx) throws IOException {
        Color fg = resolvedFg(btn);
        int indX = absX + btn.getInsets().left;
        int indY = absY + (btn.getHeight() - IND_SIZE) / 2;

        // Outer box
        ctx.writer().strokeRect(indX, indY, IND_SIZE, IND_SIZE, fg, 1.5f);

        // Checkmark (two lines) when selected
        if (btn.isSelected()) {
            float x1 = indX + 2,               y1 = indY + IND_SIZE / 2f;
            float x2 = indX + IND_SIZE / 3f + 1, y2 = indY + IND_SIZE - 3f;
            float x3 = indX + IND_SIZE - 2f,   y3 = indY + 2f;
            ctx.writer().drawLine(x1, y1, x2, y2, fg, 1.5f);
            ctx.writer().drawLine(x2, y2, x3, y3, fg, 1.5f);
        }

        drawIndicatorLabel(btn, absX, absY, indX + IND_SIZE + btn.getIconTextGap(), ctx);
    }

    // -----------------------------------------------------------------------
    // JRadioButton
    // -----------------------------------------------------------------------

    private void renderRadioButton(AbstractButton btn, int absX, int absY,
                                    HandlerContext ctx) throws IOException {
        Color fg = resolvedFg(btn);
        int indX = absX + btn.getInsets().left;
        int indY = absY + (btn.getHeight() - IND_SIZE) / 2;

        // Outer circle
        ctx.writer().strokeEllipse(indX, indY, IND_SIZE, IND_SIZE, fg, 1.5f);

        // Filled inner circle when selected
        if (btn.isSelected()) {
            ctx.writer().fillEllipse(indX + RADIO_FILL_PAD, indY + RADIO_FILL_PAD,
                                     IND_SIZE - 2 * RADIO_FILL_PAD,
                                     IND_SIZE - 2 * RADIO_FILL_PAD, fg);
        }

        drawIndicatorLabel(btn, absX, absY, indX + IND_SIZE + btn.getIconTextGap(), ctx);
    }

    // -----------------------------------------------------------------------
    // Shared label helper
    // -----------------------------------------------------------------------

    private void drawIndicatorLabel(AbstractButton btn, int absX, int absY,
                                     int textStartX, HandlerContext ctx) throws IOException {
        String text = stripHtml(btn.getText());
        if (text == null || text.isBlank()) return;

        Font  font = resolvedFont(btn);
        Color fg   = resolvedFg(btn);

        FontMetrics fm  = btn.getFontMetrics(font);
        Insets      ins = btn.getInsets();
        int cY = absY + ins.top;
        int cH = btn.getHeight() - ins.top - ins.bottom;

        int ascent    = fm.getAscent();
        int textH     = ascent + fm.getDescent();
        int baselineY = cY + (cH - textH) / 2 + ascent;

        ctx.writer().drawText(text, ctx.fontMapper().resolve(font), font.getSize2D(),
                              fg, textStartX, baselineY);
    }

    // -----------------------------------------------------------------------
    // Shared icon helper for JButton / JToggleButton
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private static Font resolvedFont(AbstractButton btn) {
        Font f = btn.getFont();
        return f != null ? f : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    private static Color resolvedFg(AbstractButton btn) {
        Color c = btn.getForeground();
        if (c == null) c = Color.BLACK;
        if (btn.isEnabled()) return c;
        Color disabledFg = UIManager.getColor("Button.disabledText");
        return disabledFg != null ? disabledFg : Color.GRAY;
    }

    private static String stripHtml(String text) {
        return HtmlTextExtractor.extractText(text);
    }
}
