package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;

/**
 * Renders a {@link JTextPane} as PDF vector text, preserving per-run font styles,
 * colours, underline, strikethrough, and background highlights.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Walk the {@link StyledDocument} element tree: root -&gt; paragraph -&gt; leaf runs.</li>
 *   <li>For each leaf, call {@link JTextPane#modelToView2D(int)} to obtain the exact
 *       screen position as computed by the Swing text layout engine.  This handles
 *       word-wrap, tab stops, and variable-height lines automatically.</li>
 *   <li>For runs that span multiple visual lines (soft word-wrap), split at row
 *       boundaries using {@link Utilities#getRowEnd(javax.swing.text.JTextComponent, int)}.</li>
 *   <li>Embedded {@link Icon} objects are rendered via {@link IconPainter}.</li>
 *   <li>Embedded {@link Component} objects are recursed into via
 *       {@link HandlerContext#traverseAt}.</li>
 * </ol>
 *
 * <p>Text produced by this handler is fully selectable in the PDF viewer.
 * {@code JEditorPane} (HTML/RTF content) continues to use {@link RasterFallback}
 * because its layout depends on the L&amp;F renderer.
 */
final class JTextPaneHandler implements ComponentHandler {

    static final JTextPaneHandler INSTANCE = new JTextPaneHandler();

    private JTextPaneHandler() {}

    /** Default font size (px) when the pane has no base font. */
    private static final int DEFAULT_FONT_SIZE = 12;

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JTextPane pane)) return;
        int w = pane.getWidth();
        int h = pane.getHeight();
        if (w <= 0 || h <= 0) return;

        // JViewport scrolls by setting view Y to -scrollOffset.
        // Compensate so absY is anchored to the viewport's top, not the scrolled view origin.
        if (pane.getParent() instanceof JViewport) {
            absY = absY - pane.getY();
            absX = absX - pane.getX();
        }

        // Background
        if (pane.isOpaque() && pane.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, w, h, pane.getBackground());
        }

        // Border
        ContainerHandler.renderBorderOnly(pane, absX, absY, ctx);

        // Document content
        StyledDocument doc = pane.getStyledDocument();
        Element root = doc.getDefaultRootElement();
        for (int pi = 0; pi < root.getElementCount(); pi++) {
            renderParagraph(pane, doc, root.getElement(pi), absX, absY, ctx);
        }
    }

    // -----------------------------------------------------------------------

    private void renderParagraph(JTextPane pane, StyledDocument doc, Element para,
                                  int absX, int absY, HandlerContext ctx) throws IOException {
        for (int i = 0; i < para.getElementCount(); i++) {
            renderLeaf(pane, doc, para.getElement(i), absX, absY, ctx);
        }
    }

    private void renderLeaf(JTextPane pane, StyledDocument doc, Element leaf,
                              int absX, int absY, HandlerContext ctx) throws IOException {
        int         start = leaf.getStartOffset();
        int         end   = leaf.getEndOffset();
        AttributeSet attrs = leaf.getAttributes();

        // Embedded icon  -- render at the run's position
        Icon icon = StyleConstants.getIcon(attrs);
        if (icon != null) {
            try {
                Rectangle2D b = pane.modelToView2D(start);
                if (b != null) {
                    IconPainter.render(icon, pane,
                                       absX + (int) b.getX(),
                                       absY + (int) b.getY(), ctx);
                }
            } catch (BadLocationException ignored) {}
            return;
        }

        // Embedded component  -- recurse at its laid-out position
        Component embedded = StyleConstants.getComponent(attrs);
        if (embedded != null) {
            try {
                Rectangle2D b = pane.modelToView2D(start);
                if (b != null) {
                    ctx.traverseAt(embedded,
                                   absX + (int) b.getX(),
                                   absY + (int) b.getY());
                }
            } catch (BadLocationException ignored) {}
            return;
        }

        // Resolve text style
        Font  font      = resolveFont(attrs, pane);
        Color fg        = StyleConstants.getForeground(attrs);
        // StyleConstants.getBackground() returns Color.BLACK when the attribute is absent,
        // not null  -- so we must check isDefined() before calling it.
        Color highlight = attrs.isDefined(StyleConstants.Background)
                          ? StyleConstants.getBackground(attrs) : null;
        boolean underline = StyleConstants.isUnderline(attrs);
        boolean strike    = StyleConstants.isStrikeThrough(attrs);

        if (fg == null) fg = pane.getForeground();
        if (fg == null) fg = Color.BLACK;

        // Transparent highlight is treated as "no highlight"
        if (highlight != null && highlight.getAlpha() == 0) highlight = null;

        FontMetrics fm = pane.getFontMetrics(font);

        // Render line-by-line to handle soft word-wrap
        int pos = start;
        while (pos < end) {
            int rowEnd;
            try {
                rowEnd = Utilities.getRowEnd(pane, pos);
            } catch (BadLocationException e) {
                break;
            }

            // Segment for this visual line: pos .. min(rowEnd+1, end)  [exclusive end]
            int segEnd = Math.min(rowEnd + 1, end);

            try {
                String seg = doc.getText(pos, segEnd - pos)
                               .replace("\n", "").replace("\r", "");
                if (!seg.isEmpty()) {
                    Rectangle2D b = pane.modelToView2D(pos);
                    if (b != null) {
                        int rx       = (int) b.getX();
                        int ry       = (int) b.getY();
                        int baseline = ry + fm.getAscent();
                        int segW     = fm.stringWidth(seg);

                        // Per-run background highlight
                        if (highlight != null) {
                            ctx.writer().fillRect(absX + rx, absY + ry,
                                                  segW, fm.getHeight(), highlight);
                        }

                        // Text
                        ctx.writer().drawText(seg, ctx.fontMapper().resolve(font),
                                              font.getSize2D(), fg,
                                              absX + rx, absY + baseline);

                        // Underline (1 px below baseline)
                        if (underline) {
                            int ulY = absY + baseline + 1;
                            ctx.writer().drawLine(absX + rx, ulY,
                                                  absX + rx + segW, ulY, fg, 1f);
                        }

                        // Strikethrough (at ~x-height midpoint)
                        if (strike) {
                            int stY = absY + baseline - fm.getAscent() / 3;
                            ctx.writer().drawLine(absX + rx, stY,
                                                  absX + rx + segW, stY, fg, 1f);
                        }
                    }
                }
            } catch (BadLocationException ignored) {}

            pos = segEnd;
        }
    }

    // -----------------------------------------------------------------------

    private static Font resolveFont(AttributeSet attrs, JTextPane pane) {
        String  family = StyleConstants.getFontFamily(attrs);
        int     size   = StyleConstants.getFontSize(attrs);
        boolean bold   = StyleConstants.isBold(attrs);
        boolean italic = StyleConstants.isItalic(attrs);

        Font base = pane.getFont();
        if (family == null || family.isEmpty()) {
            family = base != null ? base.getFamily() : Font.SANS_SERIF;
        }
        if (size <= 0) {
            size = base != null ? base.getSize() : DEFAULT_FONT_SIZE;
        }
        int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
        return new Font(family, style, size);
    }
}
