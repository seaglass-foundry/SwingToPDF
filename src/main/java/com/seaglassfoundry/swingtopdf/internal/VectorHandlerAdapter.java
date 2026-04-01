package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import com.seaglassfoundry.swingtopdf.api.VectorComponentHandler;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawerDefaultFonts;

/**
 * Bridges a user-supplied {@link VectorComponentHandler} to the internal
 * {@link ComponentHandler} interface.
 *
 * <p>Creates a {@link PdfBoxGraphics2D} backed by the current PDF document,
 * delegates drawing to the user handler, then embeds the resulting vector
 * form XObject at the component's position on the page.
 */
final class VectorHandlerAdapter implements ComponentHandler {

    private final VectorComponentHandler userHandler;

    VectorHandlerAdapter(VectorComponentHandler userHandler) {
        this.userHandler = userHandler;
    }

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        int w = comp.getWidth();
        int h = comp.getHeight();
        if (w <= 0 || h <= 0) return;

        PdfBoxGraphics2D g2 = new PdfBoxGraphics2D(ctx.document(), w, h);
        // Use the default-fonts text drawer so that text drawn via drawString()
        // is emitted as real PDF text operators (selectable/extractable), not
        // vector outlines.
        g2.setFontTextDrawer(new PdfBoxGraphics2DFontTextDrawerDefaultFonts());
        try {
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, w, h);
            userHandler.render(comp, g2, bounds);
        } finally {
            g2.dispose();
        }

        PDFormXObject xform = g2.getXFormObject();
        ctx.writer().drawFormXObject(xform, absX, absY, w, h);
    }
}
