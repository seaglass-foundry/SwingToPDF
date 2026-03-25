package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;

import javax.swing.JSplitPane;

/**
 * Renders a {@link JSplitPane} as PDF vector graphics.
 *
 * <p>The left/right (or top/bottom) content panels are rendered by recursing
 * into them via {@link HandlerContext#traverseAt}.  The divider component
 * (a L&amp;F-internal {@code BasicSplitPaneDivider}) is intentionally skipped  --
 * instead, a clean vector divider bar with optional grab-dot indicators is drawn
 * using the split pane's own geometry.
 */
final class JSplitPaneHandler implements ComponentHandler {

    static final JSplitPaneHandler INSTANCE = new JSplitPaneHandler();

    private JSplitPaneHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JSplitPane sp = (JSplitPane) comp;
        int w = sp.getWidth();
        int h = sp.getHeight();
        if (w <= 0 || h <= 0) return;

        // Background + border
        if (sp.isOpaque() && sp.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, w, h, sp.getBackground());
        }
        ContainerHandler.renderBorderOnly(sp, absX, absY, ctx);

        // Render the two content panels at their laid-out positions.
        // We call traverseAt (bypasses isVisible check) so hidden panels still render.
        Component left  = sp.getLeftComponent();   // also getTopComponent()
        Component right = sp.getRightComponent();  // also getBottomComponent()
        if (left  != null && left.getWidth()  > 0 && left.getHeight()  > 0) {
            ctx.traverseAt(left,  absX + left.getX(),  absY + left.getY());
        }
        if (right != null && right.getWidth() > 0 && right.getHeight() > 0) {
            ctx.traverseAt(right, absX + right.getX(), absY + right.getY());
        }

        // Vector divider bar  -- position derived from the component layout
        int divSize = sp.getDividerSize();
        Color divBg = sp.getBackground() != null ? sp.getBackground().darker()
                                                 : Color.LIGHT_GRAY;

        if (sp.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            int divX = dividerStart(left, right, true, sp.getDividerLocation(), divSize);
            ctx.writer().fillRect(absX + divX, absY, divSize, h, divBg);
            if (divSize >= 4) {
                // Vertical column of grab dots centred in the divider
                int cx = absX + divX + divSize / 2;
                int cy = absY + h / 2;
                for (int i = -2; i <= 2; i++) {
                    ctx.writer().fillRect(cx - 1, cy + i * 4, 2, 2, Color.GRAY);
                }
            }
        } else {
            int divY = dividerStart(left, right, false, sp.getDividerLocation(), divSize);
            ctx.writer().fillRect(absX, absY + divY, w, divSize, divBg);
            if (divSize >= 4) {
                // Horizontal row of grab dots centred in the divider
                int cx = absX + w / 2;
                int cy = absY + divY + divSize / 2;
                for (int i = -2; i <= 2; i++) {
                    ctx.writer().fillRect(cx + i * 4, cy - 1, 2, 2, Color.GRAY);
                }
            }
        }
    }

    /**
     * Returns the pixel start of the divider bar along the split axis.
     *
     * <p>Derives the position from the laid-out component bounds where possible,
     * falling back to {@code getDividerLocation()} if both panels are zero-sized.
     *
     * @param left      left/top content panel (may be null)
     * @param right     right/bottom content panel (may be null)
     * @param horizontal true for HORIZONTAL_SPLIT (X axis), false for VERTICAL (Y axis)
     * @param fallback  value from {@link JSplitPane#getDividerLocation()}
     * @param divSize   divider thickness
     */
    private static int dividerStart(Component left, Component right,
                                     boolean horizontal, int fallback, int divSize) {
        if (right != null && right.getWidth() > 0 && right.getHeight() > 0) {
            return (horizontal ? right.getX() : right.getY()) - divSize;
        }
        if (left != null && left.getWidth() > 0 && left.getHeight() > 0) {
            return (horizontal ? left.getX() + left.getWidth() : left.getY() + left.getHeight());
        }
        return fallback;
    }
}
