package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Icon;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Renders a Swing {@link Icon} into the current PDF page.
 *
 * <p>The icon is painted into a {@link BufferedImage} using the icon's own
 * {@link Icon#paintIcon} method (so all L&amp;F-provided icons, {@code ImageIcon},
 * and custom icons are supported), then routed through
 * {@link DeduplicatingImageEncoder} so repeated references to the same icon
 * instance are only encoded once per export.
 */
final class IconPainter {

    private IconPainter() {}

    /**
     * Paint {@code icon} at ({@code absX}, {@code absY}) in root pixel space.
     *
     * @param icon  the icon to render; nothing is drawn if width or height is &lt;= 0
     * @param host  the component that hosts the icon (used by
     *              {@link Icon#paintIcon} for L&amp;F colour lookups)
     * @param absX  absolute X in root pixel space
     * @param absY  absolute Y in root pixel space
     * @param ctx   current handler context
     */
    static void render(Icon icon, Component host, int absX, int absY,
                       HandlerContext ctx) throws IOException {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) return;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            icon.paintIcon(host, g, 0, 0);
        } finally {
            g.dispose();
        }

        PDImageXObject pdImg = ctx.imageEncoder().encode(img, ctx.document());
        ctx.writer().drawImage(pdImg, absX, absY, w, h);
    }
}
