package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

/**
 * Handles generic Swing containers ({@code JPanel}, {@code JScrollPane},
 * {@code JSplitPane}, etc.):
 * <ol>
 *   <li>Fill the background if the component is opaque.</li>
 *   <li>Draw the border (LineBorder, TitledBorder, CompoundBorder).</li>
 *   <li>Recurse into child components via {@link HandlerContext#traverseChildren}.</li>
 * </ol>
 */
final class ContainerHandler implements ComponentHandler {

    static final ContainerHandler INSTANCE = new ContainerHandler();

    private ContainerHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JComponent jc)) {
            // Plain AWT container  -- just recurse
            if (comp instanceof Container c) ctx.traverseChildren(c, absX, absY);
            return;
        }

        // If this is a JPanel subclass that overrides paintComponent, its visual content
        // comes from that override (not from child components), so rasterize it instead
        // of treating it as a generic container whose children we would recurse into.
        if (hasPaintComponentOverride(jc)) {
            RasterFallback.render(comp, absX, absY, ctx);
            return;
        }

        // 1. Background fill
        if (jc.isOpaque() && jc.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, jc.getWidth(), jc.getHeight(), jc.getBackground());
        }

        // 2. Border
        renderBorderOnly(jc, absX, absY, ctx);

        // 3. Recurse into children
        ctx.traverseChildren(jc, absX, absY);
    }

    /**
     * Returns true if {@code comp} is a JPanel subclass whose own class body
     * declares {@code paintComponent}  -- meaning it has custom painting that
     * is not captured by the standard background + border + children approach.
     */
    private static boolean hasPaintComponentOverride(JComponent comp) {
        Class<?> cls = comp.getClass();
        if (!javax.swing.JPanel.class.isAssignableFrom(cls) || (cls == javax.swing.JPanel.class))                  return false;
        try {
            cls.getDeclaredMethod("paintComponent", java.awt.Graphics.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Draws only the border of {@code comp}  -- no background fill, no child recursion.
     * Package-private so other handlers (e.g. {@link JTextComponentHandler}) can reuse it.
     */
    static void renderBorderOnly(JComponent comp, int absX, int absY, HandlerContext ctx)
            throws IOException {
        Border border = comp.getBorder();
        if (border != null) renderBorder(border, comp, absX, absY, ctx);
    }

    static void renderBorder(Border border, JComponent comp,
                              int absX, int absY, HandlerContext ctx) throws IOException {
        renderBorderInBounds(border, comp, absX, absY, comp.getWidth(), comp.getHeight(), ctx);
    }

    private static void renderBorderInBounds(Border border, JComponent comp,
            float bx, float by, float bw, float bh, HandlerContext ctx) throws IOException {
        if (border instanceof LineBorder lb) {
            ctx.writer().strokeRect(bx, by, bw, bh, lb.getLineColor(), lb.getThickness());

        } else if (border instanceof EtchedBorder eb) {
            Color hi = eb.getHighlightColor(comp);
            Color sh = eb.getShadowColor(comp);
            if (hi == null) hi = Color.WHITE;
            if (sh == null) sh = Color.GRAY;
            boolean raised = eb.getEtchType() == EtchedBorder.RAISED;
            // LOWERED: outer top/left = shadow, outer bottom/right = highlight
            // RAISED:  outer top/left = highlight, outer bottom/right = shadow
            Color tlOuter = raised ? hi : sh;
            Color brOuter = raised ? sh : hi;
            Color tlInner = raised ? sh : hi;
            Color brInner = raised ? hi : sh;
            // Outer rect
            ctx.writer().drawLine(bx, by, bx + bw - 1, by, tlOuter, 1f);
            ctx.writer().drawLine(bx, by, bx, by + bh - 1, tlOuter, 1f);
            ctx.writer().drawLine(bx + bw - 1, by, bx + bw - 1, by + bh - 1, brOuter, 1f);
            ctx.writer().drawLine(bx, by + bh - 1, bx + bw - 1, by + bh - 1, brOuter, 1f);
            // Inner rect (1px inset)
            ctx.writer().drawLine(bx + 1, by + 1, bx + bw - 2, by + 1, tlInner, 1f);
            ctx.writer().drawLine(bx + 1, by + 1, bx + 1, by + bh - 2, tlInner, 1f);
            ctx.writer().drawLine(bx + bw - 2, by + 1, bx + bw - 2, by + bh - 2, brInner, 1f);
            ctx.writer().drawLine(bx + 1, by + bh - 2, bx + bw - 2, by + bh - 2, brInner, 1f);

        } else if (border instanceof MatteBorder mb) {
            Color c = mb.getMatteColor();
            if (c != null) {
                Insets ins = mb.getBorderInsets(comp);
                if (ins.top    > 0) ctx.writer().fillRect(bx,              by,                   bw,        ins.top,    c);
                if (ins.bottom > 0) ctx.writer().fillRect(bx,              by + bh - ins.bottom, bw,        ins.bottom, c);
                if (ins.left   > 0) ctx.writer().fillRect(bx,              by + ins.top,          ins.left,  bh - ins.top - ins.bottom, c);
                if (ins.right  > 0) ctx.writer().fillRect(bx + bw - ins.right, by + ins.top,     ins.right, bh - ins.top - ins.bottom, c);
            }

        } else if (border instanceof TitledBorder tb) {
            String title = tb.getTitle();
            Font titleFont = tb.getTitleFont() != null ? tb.getTitleFont() : comp.getFont();
            FontMetrics fm = comp.getFontMetrics(titleFont);
            int textHeight = fm.getHeight();
            int titlePos = tb.getTitlePosition();

            // Compute the border frame offset  -- Swing offsets the border so the title
            // text straddles the border line (for TOP/BOTTOM positions)
            float frameBx = bx;
            float frameBy = by;
            float frameBw = bw;
            float frameBh = bh;
            int EDGE = 2; // matches Swing's EDGE_SPACING
            int TEXT_SPACING = 2; // matches Swing's TEXT_SPACING
            switch (titlePos) {
                case TitledBorder.ABOVE_TOP:
                    frameBy = by + textHeight + TEXT_SPACING;
                    frameBh = bh - textHeight - TEXT_SPACING;
                    break;
                case TitledBorder.BELOW_TOP:
                    // border at component edge, title below border line  -- no offset needed
                    break;
                case TitledBorder.ABOVE_BOTTOM:
                    // border at component edge  -- no offset needed
                    break;
                case TitledBorder.BELOW_BOTTOM:
                    frameBh = bh - textHeight - TEXT_SPACING;
                    break;
                case TitledBorder.BOTTOM:
                    frameBh = bh - textHeight / 2f;
                    break;
                default: // TOP (0) and DEFAULT_POSITION
                    frameBy = by + textHeight / 2f;
                    frameBh = bh - textHeight / 2f;
                    break;
            }

            // Draw the inner border at the computed frame bounds
            Border inner = tb.getBorder();
            if (inner == null) inner = javax.swing.UIManager.getBorder("TitledBorder.border");
            if (inner != null) {
                renderBorderInBounds(inner, comp, frameBx, frameBy, frameBw, frameBh, ctx);
            } else {
                ctx.writer().strokeRect(frameBx, frameBy, frameBw, frameBh, Color.GRAY, 1f);
            }

            // Draw title text if present
            if (title != null && !title.isBlank()) {
                Color titleColor = tb.getTitleColor() != null ? tb.getTitleColor() : comp.getForeground();
                if (titleColor == null) titleColor = Color.BLACK;
                int textWidth = fm.stringWidth(title);

                int justification = tb.getTitleJustification();
                float textX;
                if (justification == TitledBorder.CENTER) {
                    textX = bx + (bw - textWidth) / 2f;
                } else if (justification == TitledBorder.RIGHT || justification == TitledBorder.TRAILING) {
                    textX = bx + bw - EDGE - TEXT_SPACING - textWidth;
                } else {
                    textX = bx + EDGE + TEXT_SPACING + 3; // small inset like Swing
                }

                // Vertical position for the text baseline
                float baselineY;
                switch (titlePos) {
                    case TitledBorder.ABOVE_TOP:
                        baselineY = by + fm.getAscent();
                        break;
                    case TitledBorder.BELOW_TOP:
                        baselineY = frameBy + fm.getAscent() + EDGE + TEXT_SPACING;
                        break;
                    case TitledBorder.ABOVE_BOTTOM:
                        baselineY = by + bh - EDGE - TEXT_SPACING - fm.getDescent();
                        break;
                    case TitledBorder.BELOW_BOTTOM:
                        baselineY = by + bh - fm.getDescent();
                        break;
                    case TitledBorder.BOTTOM:
                        // text straddles the bottom border line
                        baselineY = by + bh - textHeight / 2f + fm.getAscent();
                        break;
                    default: // TOP / DEFAULT_POSITION
                        // text straddles the top border line
                        baselineY = by + fm.getAscent();
                        break;
                }

                // Erase behind title text with background color to create the "break" in the border
                Color bg = comp.getBackground() != null ? comp.getBackground() : Color.WHITE;
                float eraseY = baselineY - fm.getAscent();
                ctx.writer().fillRect(textX - 2, eraseY, textWidth + 4, textHeight, bg);
                ctx.writer().drawText(title,
                        ctx.fontMapper().resolve(titleFont),
                        titleFont.getSize2D(),
                        titleColor,
                        textX, baselineY);
            }

        } else if (border instanceof CompoundBorder cb) {
            Border outer = cb.getOutsideBorder();
            Border inner = cb.getInsideBorder();
            if (outer != null) renderBorderInBounds(outer, comp, bx, by, bw, bh, ctx);
            if (inner != null) {
                Insets outerIns = (outer != null) ? outer.getBorderInsets(comp) : new Insets(0, 0, 0, 0);
                renderBorderInBounds(inner, comp,
                        bx + outerIns.left, by + outerIns.top,
                        bw - outerIns.left - outerIns.right,
                        bh - outerIns.top - outerIns.bottom, ctx);
            }

        } else if (border instanceof BevelBorder bb) {
            int w = (int) bw;
            int h = (int) bh;
            Color hiOuter = bb.getHighlightOuterColor(comp);
            Color hiInner = bb.getHighlightInnerColor(comp);
            Color shOuter = bb.getShadowOuterColor(comp);
            Color shInner = bb.getShadowInnerColor(comp);
            if (hiOuter == null) hiOuter = Color.WHITE;
            if (hiInner == null) hiInner = Color.WHITE;
            if (shOuter == null) shOuter = Color.GRAY;
            if (shInner == null) shInner = Color.DARK_GRAY;
            boolean raised = bb.getBevelType() == BevelBorder.RAISED;
            Color tlOuter = raised ? hiOuter : shOuter;
            Color tlInner = raised ? hiInner : shInner;
            Color brOuter = raised ? shOuter : hiOuter;
            Color brInner = raised ? shInner : hiInner;
            ctx.writer().drawLine(bx, by, bx + w - 1, by, tlOuter, 1f);
            ctx.writer().drawLine(bx, by, bx, by + h - 1, tlOuter, 1f);
            ctx.writer().drawLine(bx + w - 1, by, bx + w - 1, by + h - 1, brOuter, 1f);
            ctx.writer().drawLine(bx, by + h - 1, bx + w - 1, by + h - 1, brOuter, 1f);
            ctx.writer().drawLine(bx + 1, by + 1, bx + w - 2, by + 1, tlInner, 1f);
            ctx.writer().drawLine(bx + 1, by + 1, bx + 1, by + h - 2, tlInner, 1f);
            ctx.writer().drawLine(bx + w - 2, by + 1, bx + w - 2, by + h - 2, brInner, 1f);
            ctx.writer().drawLine(bx + 1, by + h - 2, bx + w - 2, by + h - 2, brInner, 1f);

        } else if (!(border instanceof EmptyBorder)) {
            // Unrecognized border type (L&F-specific borders like MetalBorders.*,
            // BasicBorders.MarginBorder, etc.). Rasterize by painting into an image
            // so that visible L&F borders render correctly and invisible padding
            // borders produce no output (avoiding spurious gray rectangles).
            rasterizeBorder(border, comp, bx, by, bw, bh, ctx);
        }
    }

    /**
     * Rasterize an unrecognized border by painting it to a {@link BufferedImage}
     * and embedding the result as a PDF image. Borders that paint nothing (e.g.
     * {@code BasicBorders.MarginBorder}) produce a fully transparent image which
     * is silently skipped.
     */
    private static void rasterizeBorder(Border border, JComponent comp,
                                         float bx, float by, float bw, float bh,
                                         HandlerContext ctx) throws IOException {
        int w = Math.max(1, (int) bw);
        int h = Math.max(1, (int) bh);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setClip(0, 0, w, h);
        try {
            border.paintBorder(comp, g, 0, 0, w, h);
        } finally {
            g.dispose();
        }
        // Skip fully transparent images (border paints nothing)
        if (!hasVisiblePixels(img)) return;
        var pdImg = ctx.imageEncoder().encode(img, ctx.document());
        ctx.writer().drawImage(pdImg, bx, by, w, h);
    }

    /** Returns true if any pixel in the image has non-zero alpha. */
    private static boolean hasVisiblePixels(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
