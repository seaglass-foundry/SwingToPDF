package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;

import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * Renders a {@link JSeparator} (and its subclasses {@code JToolBar.Separator}
 * and {@code JPopupMenu.Separator}) as a single PDF vector line.
 *
 * <p>A horizontal separator draws a hairline across the full width at mid-height;
 * a vertical separator draws a hairline down the full height at mid-width.
 * The separator's own foreground colour is used; falls back to {@link Color#GRAY}.
 */
final class JSeparatorHandler implements ComponentHandler {

    static final JSeparatorHandler INSTANCE = new JSeparatorHandler();

    private JSeparatorHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JSeparator sep)) return;
        int w = sep.getWidth();
        int h = sep.getHeight();
        if (w <= 0 || h <= 0) return;

        Color color = sep.getForeground();
        if (color == null) color = Color.GRAY;

        if (sep.getOrientation() == SwingConstants.HORIZONTAL) {
            int midY = absY + h / 2;
            ctx.writer().drawLine(absX, midY, absX + w, midY, color, 1f);
        } else {
            int midX = absX + w / 2;
            ctx.writer().drawLine(midX, absY, midX, absY + h, color, 1f);
        }
    }
}
