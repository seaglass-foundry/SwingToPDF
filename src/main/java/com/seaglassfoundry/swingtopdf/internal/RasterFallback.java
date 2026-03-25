package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a component to a {@link BufferedImage} using the component's own
 * {@code paint()} method (real screen Graphics2D, so all L&F operations work),
 * then embeds the result as a lossless PDF image stream.
 *
 * <p>This is the deliberate, documented fallback for components that do not
 * have a specialised {@link ComponentHandler}. Text inside rasterized components
 * will not be selectable in the PDF.
 *
 * <p>Every rasterized component is logged at {@code WARN} level so callers can
 * identify which types need a handler.
 */
final class RasterFallback {

    private static final Logger log = LoggerFactory.getLogger(RasterFallback.class);

    private RasterFallback() {}

    static void render(Component comp, int absX, int absY, HandlerContext ctx)
            throws IOException {
        int w = comp.getWidth();
        int h = comp.getHeight();
        if (w <= 0 || h <= 0) return;

        String typeName = comp.getClass().getSimpleName();
        if (typeName.isEmpty()) typeName = comp.getClass().getName();
        log.debug("No handler for {}  -- rasterizing {}x{} px (text will not be selectable)",
                  typeName, w, h);
        rasterize(comp, w, h, absX, absY, ctx);
    }

    /**
     * Rasterize {@code comp} at explicit dimensions without emitting a WARN.
     * Used intentionally for non-{@link javax.swing.JLabel} table-cell renderers
     * where rasterization is the correct fallback, not a missing-handler gap.
     */
    static void renderSized(Component comp, int w, int h, int absX, int absY,
                             HandlerContext ctx) throws IOException {
        if (w <= 0 || h <= 0) return;
        log.debug("Rasterizing cell renderer {} at {}x{} px", comp.getClass().getSimpleName(), w, h);
        comp.setSize(w, h);
        rasterize(comp, w, h, absX, absY, ctx);
    }

    // -----------------------------------------------------------------------

    private static void rasterize(Component comp, int w, int h,
                                   int absX, int absY, HandlerContext ctx)
            throws IOException {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Set a clip rect so that getClip() is never null  -- some L&F painters
        // (Metal, Nimbus) call getClip().intersects(...) without a null check.
        g.setClip(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,        RenderingHints.VALUE_RENDER_QUALITY);
        try {
            comp.paint(g);
        } finally {
            g.dispose();
        }

        PDImageXObject pdImg = ctx.imageEncoder().encode(img, ctx.document());
        ctx.writer().drawImage(pdImg, absX, absY, w, h);
    }
}
