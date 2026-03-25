package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Renders a {@link JLabel} as PDF vector text, with its icon (if any) embedded
 * as a raster image.
 *
 * <p>Text and icon are positioned using {@link SwingUtilities#layoutCompoundLabel}
 * so that alignment, text position, and icon-text gap exactly match the on-screen
 * layout.  HTML text is decoded via {@link HtmlTextExtractor}.
 *
 * <p>Borders are drawn before text/icon so that labels with
 * {@code LineBorder} or {@code TitledBorder} still render their content.
 *
 * <p>HTML text containing {@code <br>} or block-level tags is rendered as
 * multiple lines, centred vertically in the label's content area.
 *
 * <p>When {@code label.isEnabled()} is {@code false} the foreground colour is
 * taken from {@code UIManager("Label.disabledForeground")}, falling back to
 * {@code fg.brighter()}.
 */
final class JLabelHandler implements ComponentHandler {

    static final JLabelHandler INSTANCE = new JLabelHandler();

    private JLabelHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JLabel label = (JLabel) comp;

        // Background
        if (label.isOpaque() && label.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, label.getWidth(), label.getHeight(),
                                  label.getBackground());
        }

        // Border  -- draw it, but do NOT return; text/icon rendering continues below
        if (label.getBorder() != null) {
            ContainerHandler.renderBorderOnly(label, absX, absY, ctx);
        }

        Icon   icon = label.getIcon();
        String text = label.getText();

        // Nothing to render
        if (icon == null && (text == null || text.isBlank())) return;

        // HTML text: parse and render with full inline styling (colour, size, bold/italic/u/s)
        if (text != null && text.regionMatches(true, 0, "<html>", 0, 6)) {
            Font baseFont = label.getFont();
            if (baseFont == null) baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            Color fg = label.getForeground();
            if (fg == null) fg = Color.BLACK;
            if (!label.isEnabled()) {
                Color disabledFg = UIManager.getColor("Label.disabledForeground");
                fg = disabledFg != null ? disabledFg : fg.brighter();
            }
            List<List<HtmlStyledTextRenderer.StyledRun>> lines =
                    HtmlStyledTextRenderer.parse(text, baseFont, fg);
            HtmlStyledTextRenderer.renderLines(label, icon, lines, absX, absY, ctx);
            return;
        }

        text = HtmlTextExtractor.extractText(text);

        Font font = label.getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        Color fg = label.getForeground();
        if (fg == null) fg = Color.BLACK;
        if (!label.isEnabled()) {
            Color disabledFg = UIManager.getColor("Label.disabledForeground");
            fg = disabledFg != null ? disabledFg : fg.brighter();
        }

        FontMetrics fm     = label.getFontMetrics(font);
        Insets      insets = label.getInsets();

        // If text contains newlines (from <br> in HTML), use multi-line rendering
        if (text != null && text.contains("\n")) {
            renderMultiLine(label, icon, text, font, fg, fm, insets, absX, absY, ctx);
            return;
        }

        Rectangle viewR  = new Rectangle(
                insets.left,
                insets.top,
                label.getWidth()  - insets.left - insets.right,
                label.getHeight() - insets.top  - insets.bottom);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();

        // Let Swing compute exact icon and text positions
        String layoutText = (text != null && !text.isBlank()) ? text : null;
        SwingUtilities.layoutCompoundLabel(
                label, fm,
                layoutText,
                icon,
                label.getVerticalAlignment(),
                label.getHorizontalAlignment(),
                label.getVerticalTextPosition(),
                label.getHorizontalTextPosition(),
                viewR, iconR, textR,
                icon == null ? 0 : label.getIconTextGap());

        // Render icon
        if (icon != null) {
            IconPainter.render(icon, label, absX + iconR.x, absY + iconR.y, ctx);
        }

        // Render text
        if (layoutText != null) {
            int baselineY = absY + textR.y + fm.getAscent();
            var pdFont = ctx.fontMapper().resolve(font);
            int textX = absX + textR.x;
            int ha = label.getHorizontalAlignment();
            if (ha == SwingConstants.RIGHT || ha == SwingConstants.TRAILING) {
                // Use PDF font metrics for right-edge placement: Swing's FM and the
                // PDF font may disagree on string width, causing visible overflow.
                float pdfW = ctx.writer().textWidthPx(text, pdFont, font.getSize2D());
                textX = absX + label.getWidth() - insets.right - Math.round(pdfW);
                textX = Math.max(textX, absX + insets.left);
            }
            ctx.writer().drawText(text, pdFont, font.getSize2D(), fg, textX, baselineY);
        }
    }

    /**
     * Renders multi-line text (split on {@code \n}).  The icon, if present, is
     * placed at the leading edge and vertically centred.  Text lines are laid out
     * in a column and the whole block is positioned according to the label's
     * vertical alignment.
     */
    private static void renderMultiLine(JLabel label, Icon icon, String text,
                                         Font font, Color fg, FontMetrics fm,
                                         Insets insets,
                                         int absX, int absY,
                                         HandlerContext ctx) throws IOException {
        String[] lines  = text.split("\n", -1);
        int      lineH  = fm.getHeight();
        int      totalH = lines.length * lineH;

        int viewX = absX + insets.left;
        int viewY = absY + insets.top;
        int viewW = label.getWidth()  - insets.left - insets.right;
        int viewH = label.getHeight() - insets.top  - insets.bottom;

        // Icon at leading edge, vertically centred
        if (icon != null) {
            int iconY = viewY + (viewH - icon.getIconHeight()) / 2;
            IconPainter.render(icon, label, viewX, iconY, ctx);
            viewX += icon.getIconWidth() + label.getIconTextGap();
            viewW -= icon.getIconWidth() + label.getIconTextGap();
        }

        // Vertical alignment of the text block
        int startY;
        int va = label.getVerticalAlignment();
        if (va == SwingConstants.BOTTOM) {
            startY = viewY + viewH - totalH;
        } else if (va == SwingConstants.TOP) {
            startY = viewY;
        } else {  // CENTER (default)
            startY = viewY + (viewH - totalH) / 2;
        }

        int ha = label.getHorizontalAlignment();
        var pdFont = ctx.fontMapper().resolve(font);
        for (String line : lines) {
            int x;
            if (ha == SwingConstants.RIGHT || ha == SwingConstants.TRAILING) {
                float pdfW = ctx.writer().textWidthPx(line, pdFont, font.getSize2D());
                x = viewX + viewW - Math.round(pdfW);
            } else if (ha == SwingConstants.CENTER) {
                float pdfW = ctx.writer().textWidthPx(line, pdFont, font.getSize2D());
                x = viewX + (int)((viewW - pdfW) / 2);
            } else {
                x = viewX;
            }
            ctx.writer().drawText(line, pdFont, font.getSize2D(), fg, x, startY + fm.getAscent());
            startY += lineH;
        }
    }
}
