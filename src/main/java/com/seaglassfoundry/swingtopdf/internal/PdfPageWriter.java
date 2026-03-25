package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper over {@link PDPageContentStream} that:
 * <ul>
 *   <li>Converts Swing pixel coordinates (top-left origin, Y-down) to PDF page
 *       coordinates (bottom-left origin, Y-up)</li>
 *   <li>Accounts for the page margins and scale factor</li>
 *   <li>Accounts for the vertical slice offset in multi-page exports</li>
 *   <li>Caches the current fill/stroke color to avoid redundant state changes</li>
 * </ul>
 *
 * <h3>Coordinate system</h3>
 * All {@code x}, {@code y}, {@code w}, {@code h} parameters are in Swing root-space
 * pixels (absolute, measured from the root component's top-left corner).
 * The writer applies the scale factor and Y-axis flip internally.
 */
final class PdfPageWriter {

    private static final Logger log = LoggerFactory.getLogger(PdfPageWriter.class);

    private final PDPageContentStream cs;
    private final float scale;          // Swing pixels -&gt; PDF points  (= 72/dpi x fitScale)
    private final float marginLeft;     // left margin in points
    private final float marginTop;      // top margin in points
    private final float pageH;          // total page height in points
    private final float sliceTopPx;     // first Swing pixel row visible on this page (0 for page 1)
    private final float pageHeightPx;   // page height in Swing pixels (printable area)
    private final float printableWidthPt;   // printable width in PDF points
    private final float printableHeightPt;  // printable height in PDF points

    // Cached graphics state  -- avoids redundant PDF colour/alpha operators
    private Color lastFill;
    private Color lastStroke;
    private float lastFillAlpha   = 1f;
    private float lastStrokeAlpha = 1f;

    PdfPageWriter(PDPageContentStream cs,
                  float scale,
                  float marginLeft, float marginTop,
                  float pageH,
                  float sliceTopPx,
                  float pageHeightPx,
                  float printableWidthPt,
                  float printableHeightPt) {
        this.cs                = cs;
        this.scale             = scale;
        this.marginLeft        = marginLeft;
        this.marginTop         = marginTop;
        this.pageH             = pageH;
        this.sliceTopPx        = sliceTopPx;
        this.pageHeightPx      = pageHeightPx;
        this.printableWidthPt  = printableWidthPt;
        this.printableHeightPt = printableHeightPt;
    }

    /**
     * Apply a clip rect covering the current page slice. When page breaks snap
     * to avoid splitting widgets, the slice may be shorter than the full
     * printable height. The clip rect is sized to the actual slice height
     * so that content beyond the slice boundary is not visible.
     *
     * @param sliceHeightPx actual height of this page slice in Swing pixels
     *                      ({@code sliceBottomPx - sliceTopPx})
     */
    void applyPageClip(float sliceHeightPx) throws IOException {
        float clipH = Math.min(sliceHeightPx * scale, printableHeightPt);
        cs.saveGraphicsState();
        cs.addRect(marginLeft,
                   pageH - marginTop - clipH,
                   printableWidthPt, clipH);
        cs.clip();
    }

    // -----------------------------------------------------------------------
    // Coordinate helpers (package-private so handlers can use them directly)
    // -----------------------------------------------------------------------

    /** Y coordinate of the top of the current page slice, in Swing root-space pixels. */
    float sliceTopPx()   { return sliceTopPx; }

    /** Printable page height in Swing pixels (used by AcroFormEmitter for page detection). */
    float pageHeightPx() { return pageHeightPx; }

    /** Swing X -&gt; PDF X. */
    float pdfX(float swingX) {
        return marginLeft + swingX * scale;
    }

    /**
     * Swing top-Y + height -&gt; PDF bottom-Y (PDFBox's lower-left convention).
     * Accounts for the vertical slice offset so page 2 sees different content
     * than page 1.
     */
    float pdfY(float swingY, float swingH) {
        return pageH - marginTop - (swingY - sliceTopPx + swingH) * scale;
    }

    /**
     * Swing text baseline Y -&gt; PDF baseline Y.
     * Text is positioned by its baseline in both Swing and PDF.
     */
    float pdfBaseline(float swingBaselineY) {
        return pageH - marginTop - (swingBaselineY - sliceTopPx) * scale;
    }

    /** Scale Swing pixels to PDF points. */
    float pt(float pixels) {
        return pixels * scale;
    }

    /**
     * Returns the advance width of {@code text} rendered at {@code fontSizePx} in
     * Swing pixel units, using the PDF font's own glyph metrics.
     * Used to position right- or center-aligned text precisely without relying on
     * Swing's {@link java.awt.FontMetrics} (which may differ from the PDF font).
     */
    float textWidthPx(String text, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSizePx) {
        if (text == null || text.isEmpty() || font == null) return 0f;
        try {
            return font.getStringWidth(text) / 1000f * fontSizePx;
        } catch (IOException e) {
            return 0f;
        }
    }

    // -----------------------------------------------------------------------
    // Drawing primitives
    // -----------------------------------------------------------------------

    /** Fill a rectangle with a solid colour. */
    void fillRect(float x, float y, float w, float h, Color color) throws IOException {
        if (color == null || w <= 0 || h <= 0) return;
        setFillColor(color);
        cs.addRect(pdfX(x), pdfY(y, h), pt(w), pt(h));
        cs.fill();
    }

    /** Stroke (outline) a rectangle. {@code lineWidthPx} is in Swing pixels. */
    void strokeRect(float x, float y, float w, float h, Color color, float lineWidthPx)
            throws IOException {
        if (color == null || w <= 0 || h <= 0) return;
        setStrokeColor(color);
        cs.setLineWidth(pt(lineWidthPx));
        cs.addRect(pdfX(x), pdfY(y, h), pt(w), pt(h));
        cs.stroke();
    }

    /** Draw a line from (x1,y1) to (x2,y2). Coordinates in Swing pixels. */
    void drawLine(float x1, float y1, float x2, float y2, Color color, float lineWidthPx)
            throws IOException {
        if (color == null) return;
        setStrokeColor(color);
        cs.setLineWidth(pt(lineWidthPx));
        cs.moveTo(pdfX(x1), pdfBaseline(y1));
        cs.lineTo(pdfX(x2), pdfBaseline(y2));
        cs.stroke();
    }

    /**
     * Stroke (outline) an ellipse inscribed in the given bounding box.
     * Coordinates and size are in Swing pixels.
     */
    void strokeEllipse(float x, float y, float w, float h, Color color, float lineWidthPx)
            throws IOException {
        if (color == null || w <= 0 || h <= 0) return;
        setStrokeColor(color);
        cs.setLineWidth(pt(lineWidthPx));
        appendEllipsePath(x, y, w, h);
        cs.stroke();
    }

    /**
     * Fill an ellipse inscribed in the given bounding box.
     * Coordinates and size are in Swing pixels.
     */
    void fillEllipse(float x, float y, float w, float h, Color color) throws IOException {
        if (color == null || w <= 0 || h <= 0) return;
        setFillColor(color);
        appendEllipsePath(x, y, w, h);
        cs.fill();
    }

    /**
     * Appends an ellipse path to the content stream, approximated by four cubic
     * Bezier curves (Spiro constant k ~= 0.5523).  All inputs are in Swing pixel
     * space; the helper converts to PDF page coordinates internally.
     */
    private void appendEllipsePath(float x, float y, float w, float h) throws IOException {
        final float K  = 0.5523f;
        float rx = w / 2f;
        float ry = h / 2f;
        float cx = x + rx;
        float cy = y + ry;

        // PDF page-space coordinates for center and radii
        float pCx = pdfX(cx);
        float pCy = pdfBaseline(cy);   // Y-axis is flipped in PDF vs Swing
        float pRx = pt(rx);
        float pRy = pt(ry);

        cs.moveTo(pCx, pCy + pRy);
        cs.curveTo(pCx + K * pRx, pCy + pRy,   pCx + pRx, pCy + K * pRy,   pCx + pRx, pCy);
        cs.curveTo(pCx + pRx, pCy - K * pRy,   pCx + K * pRx, pCy - pRy,   pCx,       pCy - pRy);
        cs.curveTo(pCx - K * pRx, pCy - pRy,   pCx - pRx, pCy - K * pRy,   pCx - pRx, pCy);
        cs.curveTo(pCx - pRx, pCy + K * pRy,   pCx - K * pRx, pCy + pRy,   pCx,       pCy + pRy);
        cs.closePath();
    }

    /**
     * Draw a string of text.
     *
     * @param text        the string to draw
     * @param font        the embedded PDFont
     * @param fontSizePx  font size in Swing pixels
     * @param color       text colour
     * @param x           left edge of text in Swing pixels (root-space)
     * @param baselineY   text baseline Y in Swing pixels (root-space)
     */
    void drawText(String text, PDFont font, float fontSizePx, Color color,
                  float x, float baselineY) throws IOException {
        if (text == null || text.isEmpty() || color == null) return;
        float fontSizePt = fontSizePx * scale;
        if (fontSizePt < 0.5f) return; // invisible

        // Strip control characters (newlines, tabs, etc.)  -- PDF text operators
        // do not accept them and throw IllegalArgumentException.
        text = sanitize(text);
        if (text.isEmpty()) return;

        setFillColor(color);
        cs.beginText();
        cs.setFont(font, fontSizePt);
        cs.newLineAtOffset(pdfX(x), pdfBaseline(baselineY));
        try {
            cs.showText(text);
        } catch (Exception e) {
            // Font encoding issue  -- try printable ASCII subset
            String safe = sanitizeToAscii(text);
            if (!safe.isEmpty()) {
                try { cs.showText(safe); } catch (Exception e2) {
                    log.warn("Text could not be rendered (even ASCII fallback failed) for '{}': {}", safe, e2.getMessage());
                }
            }
        } finally {
            cs.endText();
        }
    }

    /** Remove control characters that PDF text streams cannot contain. */
    private static String sanitize(String s) {
        return s.codePoints()
                .filter(cp -> cp >= 0x20 || cp == 0x09) // keep printable + tab
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        // NOTE: do NOT trim  -- leading/trailing spaces are valid in multi-run HTML rendering
        // (inter-run spaces live at the edges of adjacent styled runs)
    }

    /** Further restrict to printable ASCII (< 128) for Type-1 fallback fonts. */
    private static String sanitizeToAscii(String s) {
        return s.codePoints()
                .filter(cp -> cp >= 0x20 && cp < 128)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Draw a pre-created image XObject at the given Swing-pixel bounds.
     */
    void drawImage(PDImageXObject img, float x, float y, float w, float h) throws IOException {
        if (w <= 0 || h <= 0) return;
        cs.saveGraphicsState();
        cs.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(pdfX(x), pdfY(y, h)));
        cs.transform(org.apache.pdfbox.util.Matrix.getScaleInstance(pt(w), pt(h)));
        cs.drawImage(img, 0, 0, 1, 1);
        cs.restoreGraphicsState();
    }

    /** Save the current graphics state. */
    void save() throws IOException {
        cs.saveGraphicsState();
        lastFill       = null; // force re-emit after restore
        lastStroke     = null;
        lastFillAlpha  = 1f;
        lastStrokeAlpha = 1f;
    }

    /** Restore the previously saved graphics state. */
    void restore() throws IOException {
        cs.restoreGraphicsState();
        lastFill       = null;
        lastStroke     = null;
        lastFillAlpha  = 1f;
        lastStrokeAlpha = 1f;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void setFillColor(Color c) throws IOException {
        if (c.equals(lastFill)) return;
        cs.setNonStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
        float alpha = c.getAlpha() / 255f;
        if (alpha != lastFillAlpha) {
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant(alpha);
            cs.setGraphicsStateParameters(gs);
            lastFillAlpha = alpha;
        }
        lastFill = c;
    }

    private void setStrokeColor(Color c) throws IOException {
        if (c.equals(lastStroke)) return;
        cs.setStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
        float alpha = c.getAlpha() / 255f;
        if (alpha != lastStrokeAlpha) {
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setStrokingAlphaConstant(alpha);
            cs.setGraphicsStateParameters(gs);
            lastStrokeAlpha = alpha;
        }
        lastStroke = c;
    }
}
