package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter.Alignment;

/**
 * Draws a {@link HeaderFooter} band at the top or bottom of a PDF page. Routes
 * to a mode-specific path:
 * <ul>
 *   <li>TEXT      -- Helvetica, optional greedy word-wrap</li>
 *   <li>HTML      -- parsed via {@link HtmlStyledTextRenderer#parse}, rendered
 *                    with per-run styling and word-wrap</li>
 *   <li>COMPONENT -- full Swing component rendered through the handler pipeline
 *                    using a dedicated {@link PdfPageWriter} scoped to the band</li>
 * </ul>
 *
 * <p>Bands live outside the content clip, so this class manages its own graphics
 * state (background fill, component rendering) without touching the caller's
 * clip rectangle.
 */
final class HeaderFooterRenderer {

    // Inset between the band edge and the text/content (matches TEXT mode's
    // original 4pt inset for left/right alignment).
    private static final float EDGE_INSET_PT = 4f;

    private HeaderFooterRenderer() {}

    /**
     * Render {@code band} on the given page. Parameters mirror the structure
     * used by {@code ExportEngine.renderPages}.
     *
     * @param band       the band to render
     * @param doc        the PDF document (needed for COMPONENT mode)
     * @param pdPage     the current page (needed for COMPONENT mode)
     * @param cs         the page content stream
     * @param page       1-based page number
     * @param pages      total page count
     * @param pageW      full page width in points
     * @param pageH      full page height in points
     * @param marginLeft left margin in points
     * @param marginRight right margin in points
     * @param margin     top margin (header) or bottom margin (footer) in points
     * @param isHeader   {@code true} for the top band, {@code false} for the bottom
     * @param fontMapper font mapper (needed for HTML and COMPONENT modes)
     * @param registry   handler registry (needed for COMPONENT mode)
     * @param imageEncoder image encoder (needed for COMPONENT mode)
     * @param config     the export configuration (needed for COMPONENT mode)
     * @param baseScale  Swing-pixel-to-PDF-point scale (= 72 / dpi)
     */
    static void draw(HeaderFooter band,
                     PDDocument doc, PDPage pdPage, PDPageContentStream cs,
                     int page, int pages,
                     float pageW, float pageH,
                     float marginLeft, float marginRight, float margin,
                     boolean isHeader,
                     DefaultFontMapper fontMapper,
                     HandlerRegistry registry,
                     DeduplicatingImageEncoder imageEncoder,
                     ExportConfig config,
                     float baseScale) throws IOException {
        switch (band.mode()) {
            case TEXT      -> drawTextBand(band, cs, page, pages, pageW, pageH,
                                           marginLeft, marginRight, margin, isHeader);
            case HTML      -> drawHtmlBand(band, cs, page, pages, pageW, pageH,
                                           marginLeft, marginRight, margin, isHeader);
            case COMPONENT -> drawComponentBand(band, doc, pdPage, cs, page, pages,
                                                pageW, pageH, marginLeft, marginRight,
                                                margin, isHeader,
                                                fontMapper, registry, imageEncoder,
                                                config, baseScale);
        }
    }

    // =======================================================================
    // TEXT mode
    // =======================================================================

    private static void drawTextBand(HeaderFooter band, PDPageContentStream cs,
                                      int page, int pages,
                                      float pageW, float pageH,
                                      float marginLeft, float marginRight, float margin,
                                      boolean isHeader) throws IOException {
        String text = band.resolve(page, pages);
        if (text.isBlank()) return;

        PDType1Font font     = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float       fontSize = band.fontSize();
        float       printW   = pageW - marginLeft - marginRight;
        float       bandH    = band.height() > 0 ? Math.min(band.height(), margin) : margin;
        float       bandY    = isHeader ? pageH - bandH : 0;

        paintBackground(cs, band, pageW, bandY, bandH);

        Color c = band.color();
        cs.setNonStrokingColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);

        if (!band.wrap()) {
            // Single-line path (unchanged default behaviour)
            float textW = font.getStringWidth(text) / 1000f * fontSize;
            float x = alignX(band.alignment(), marginLeft, printW, textW);
            float y = bandY + bandH / 2f - fontSize / 3f;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            return;
        }

        // Wrapped path
        float maxLineW = printW - 2 * EDGE_INSET_PT;
        List<String> lines = wrapPlainText(text, font, fontSize, maxLineW);
        float lineH = fontSize * 1.2f;
        int maxLines = Math.max(1, (int) Math.floor(bandH / lineH));
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);

        // Anchor: top for header, bottom for footer
        float firstBaselineY;
        if (isHeader) {
            // Baseline of line 0 sits near top edge, leaving room for ascenders
            firstBaselineY = bandY + bandH - fontSize * 0.9f;
        } else {
            // Baseline of last line sits near bottom edge, leaving room for descenders
            float totalH = lines.size() * lineH;
            firstBaselineY = bandY + totalH - fontSize * 0.3f;
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineW = font.getStringWidth(line) / 1000f * fontSize;
            float x     = alignX(band.alignment(), marginLeft, printW, lineW);
            float y     = firstBaselineY - i * lineH;
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y);
            cs.showText(line);
            cs.endText();
        }
    }

    // =======================================================================
    // HTML mode
    // =======================================================================

    private static void drawHtmlBand(HeaderFooter band, PDPageContentStream cs,
                                      int page, int pages,
                                      float pageW, float pageH,
                                      float marginLeft, float marginRight, float margin,
                                      boolean isHeader) throws IOException {
        String html = band.resolve(page, pages);
        if (html.isBlank()) return;

        // Use fontSize directly (treat band as point-space: Swing Font sizes are
        // numerically equal to PDF point sizes in this isolated renderer).
        Font baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(1, Math.round(band.fontSize())));
        List<List<HtmlStyledTextRenderer.StyledRun>> parsed =
                HtmlStyledTextRenderer.parse(html, baseFont, band.color());
        if (parsed.isEmpty()) return;

        float printW   = pageW - marginLeft - marginRight;
        float bandH    = band.height() > 0 ? Math.min(band.height(), margin) : margin;
        float bandY    = isHeader ? pageH - bandH : 0;
        float maxLineW = printW - 2 * EDGE_INSET_PT;

        paintBackground(cs, band, pageW, bandY, bandH);

        // Word-wrap each parsed logical line to fit printable width
        List<List<HtmlStyledTextRenderer.StyledRun>> lines = new ArrayList<>();
        for (List<HtmlStyledTextRenderer.StyledRun> logicalLine : parsed) {
            lines.addAll(wrapStyledLine(logicalLine, maxLineW));
        }
        if (lines.isEmpty()) return;

        // Per-line metrics (max run font size sets line height)
        float[] lineHeights = new float[lines.size()];
        float[] lineAscents = new float[lines.size()];
        float totalH = 0f;
        for (int i = 0; i < lines.size(); i++) {
            float maxSize = band.fontSize();
            for (HtmlStyledTextRenderer.StyledRun run : lines.get(i)) {
                maxSize = Math.max(maxSize, run.font().getSize2D());
            }
            lineHeights[i] = maxSize * 1.2f;
            lineAscents[i] = maxSize * 0.9f;
            totalH += lineHeights[i];
        }

        // Clip lines that don't fit in the band
        int keep = lines.size();
        float acc = 0f;
        for (int i = 0; i < lines.size(); i++) {
            acc += lineHeights[i];
            if (acc > bandH) { keep = i; break; }
        }
        if (keep == 0) keep = 1; // always emit at least one line
        if (keep < lines.size()) {
            lines.subList(keep, lines.size()).clear();
            totalH = 0f;
            for (int i = 0; i < keep; i++) totalH += lineHeights[i];
        }

        // Starting Y of the first line's TOP edge (in PDF coords, higher = top)
        float blockTopY;
        if (isHeader) {
            blockTopY = bandY + bandH;                 // block pinned to top of band
        } else {
            blockTopY = bandY + totalH;                // block pinned to bottom of band
        }

        float cursorTopY = blockTopY;
        for (int i = 0; i < lines.size(); i++) {
            List<HtmlStyledTextRenderer.StyledRun> line = lines.get(i);

            // Measure line width (sum of run widths at each run's own font size)
            float lineW = 0f;
            for (HtmlStyledTextRenderer.StyledRun run : line) {
                lineW += measureRunPt(run);
            }

            float startX  = alignX(band.alignment(), marginLeft, printW, lineW);
            float baseY   = cursorTopY - lineAscents[i];

            float runX = startX;
            for (HtmlStyledTextRenderer.StyledRun run : line) {
                float runPt = run.font().getSize2D();
                PDType1Font pdFont = resolveStandardFont(run.font());
                float runW = measureRunPt(run);

                // Colour
                Color col = run.color() != null ? run.color() : band.color();
                cs.setNonStrokingColor(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);

                // Text
                cs.beginText();
                cs.setFont(pdFont, runPt);
                cs.newLineAtOffset(runX, baseY);
                try {
                    cs.showText(sanitize(run.text()));
                } catch (Exception ex) {
                    // Fallback to ASCII if the font cannot encode certain glyphs
                    String safe = sanitizeAscii(run.text());
                    if (!safe.isEmpty()) {
                        try { cs.showText(safe); } catch (Exception ignore) {}
                    }
                }
                cs.endText();

                // Underline / strike
                if (run.underline()) {
                    cs.setStrokingColor(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
                    cs.setLineWidth(Math.max(0.5f, runPt * 0.05f));
                    cs.moveTo(runX, baseY - runPt * 0.08f);
                    cs.lineTo(runX + runW, baseY - runPt * 0.08f);
                    cs.stroke();
                }
                if (run.strike()) {
                    cs.setStrokingColor(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
                    cs.setLineWidth(Math.max(0.5f, runPt * 0.05f));
                    cs.moveTo(runX, baseY + runPt * 0.30f);
                    cs.lineTo(runX + runW, baseY + runPt * 0.30f);
                    cs.stroke();
                }
                runX += runW;
            }
            cursorTopY -= lineHeights[i];
        }
    }

    // =======================================================================
    // COMPONENT mode
    // =======================================================================

    private static void drawComponentBand(HeaderFooter band,
                                           PDDocument doc, PDPage pdPage,
                                           PDPageContentStream cs,
                                           int page, int pages,
                                           float pageW, float pageH,
                                           float marginLeft, float marginRight, float margin,
                                           boolean isHeader,
                                           DefaultFontMapper fontMapper,
                                           HandlerRegistry registry,
                                           DeduplicatingImageEncoder imageEncoder,
                                           ExportConfig config,
                                           float baseScale) throws IOException {
        JComponent comp = band.component();
        if (comp == null) return;

        float printW = pageW - marginLeft - marginRight;

        // Resolve {page}/{pages} in the component tree; remember originals for restore
        TokenSnapshot snap = EdtHelper.callOnEdt(() -> substituteTokens(comp, page, pages));

        try {
            // Band geometry. If no explicit height, fall back to component's preferred
            // height at the configured DPI, clamped to the margin.
            float bandH;
            if (band.height() > 0) {
                bandH = Math.min(band.height(), margin);
            } else {
                Dimension pref = EdtHelper.callOnEdt(comp::getPreferredSize);
                float prefHpt = (pref != null ? pref.height : 0) * baseScale;
                if (prefHpt <= 0) prefHpt = margin;
                bandH = Math.min(prefHpt, margin);
            }

            float printWpx = printW / baseScale;
            float bandHpx  = bandH / baseScale;

            // Component width: span the full printable width so that opaque
            // backgrounds, borders, and BorderLayout children stretch edge to
            // edge. Internal alignment (e.g. JLabel.setHorizontalAlignment or
            // the layout manager) positions the visible content within the
            // band. HeaderFooter.alignment() is therefore a no-op in COMPONENT
            // mode -- use the component's own alignment API instead.
            int compWpx = (int) printWpx;
            int compXpx = 0;

            // Layout the component at its band-local bounds
            final int fCompWpx = compWpx, fCompHpx = Math.max(1, Math.round(bandHpx)), fCompXpx = compXpx;
            EdtHelper.runOnEdt(() -> {
                comp.setBounds(fCompXpx, 0, fCompWpx, fCompHpx);
                comp.doLayout();
                // Ensure nested components pick up their sizes (e.g. BoxLayout panels)
                forceValidate(comp);
            });

            // Band background (spans full page width, behind the component)
            float bandY = isHeader ? pageH - bandH : 0;
            paintBackground(cs, band, pageW, bandY, bandH);

            // Dedicated page writer scoped to the band's coordinate frame
            float bandMarginTop = isHeader ? 0f : (pageH - bandH);
            PdfPageWriter bandWriter = new PdfPageWriter(
                    cs, baseScale,
                    marginLeft, bandMarginTop,
                    pageH,
                    0f,                // sliceTopPx
                    bandHpx,           // pageHeightPx
                    printW,            // printableWidthPt
                    bandH);            // printableHeightPt

            HandlerContext bandCtx = new HandlerContext(
                    bandWriter, fontMapper, config, doc, pdPage,
                    0f, bandHpx, printWpx, imageEncoder, null /* no acroForm in bands */);
            ComponentTraverser bandTraverser = new ComponentTraverser(registry, bandCtx);
            bandCtx.setTraverser(bandTraverser);

            EdtHelper.runOnEdt(() -> {
                try {
                    bandTraverser.traverse(comp, 0, 0);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException re) {
            if (re.getCause() instanceof IOException ioe) throw ioe;
            throw re;
        } finally {
            EdtHelper.runOnEdt(() -> restoreTokens(snap));
        }
    }

    // =======================================================================
    // Token substitution for COMPONENT mode
    // =======================================================================

    /** Captured text values keyed by component reference, for restoration after render. */
    private record TokenSnapshot(List<JLabel> labels, List<String> labelTexts,
                                  List<JTextComponent> texts, List<String> textValues) {}

    private static TokenSnapshot substituteTokens(Component root, int page, int pages) {
        List<JLabel>           labels     = new ArrayList<>();
        List<String>           labelTexts = new ArrayList<>();
        List<JTextComponent>   texts      = new ArrayList<>();
        List<String>           textValues = new ArrayList<>();
        walkForTokens(root, page, pages, labels, labelTexts, texts, textValues);
        return new TokenSnapshot(labels, labelTexts, texts, textValues);
    }

    private static void walkForTokens(Component c, int page, int pages,
                                       List<JLabel> labels, List<String> labelTexts,
                                       List<JTextComponent> texts, List<String> textValues) {
        if (c instanceof JLabel label) {
            String orig = label.getText();
            if (orig != null && (orig.contains("{page}") || orig.contains("{pages}"))) {
                labels.add(label);
                labelTexts.add(orig);
                label.setText(resolve(orig, page, pages));
            }
        } else if (c instanceof JTextComponent tc) {
            String orig = tc.getText();
            if (orig != null && (orig.contains("{page}") || orig.contains("{pages}"))) {
                texts.add(tc);
                textValues.add(orig);
                tc.setText(resolve(orig, page, pages));
            }
        }
        if (c instanceof Container container) {
            for (int i = 0; i < container.getComponentCount(); i++) {
                walkForTokens(container.getComponent(i), page, pages,
                              labels, labelTexts, texts, textValues);
            }
        }
    }

    private static void restoreTokens(TokenSnapshot snap) {
        for (int i = 0; i < snap.labels.size(); i++) {
            snap.labels.get(i).setText(snap.labelTexts.get(i));
        }
        for (int i = 0; i < snap.texts.size(); i++) {
            snap.texts.get(i).setText(snap.textValues.get(i));
        }
    }

    private static String resolve(String s, int page, int pages) {
        return s.replace("{page}",  String.valueOf(page))
                .replace("{pages}", String.valueOf(pages));
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private static void paintBackground(PDPageContentStream cs, HeaderFooter band,
                                         float pageW, float bandY, float bandH) throws IOException {
        Color bg = band.backgroundColor();
        if (bg == null) return;
        cs.setNonStrokingColor(bg.getRed() / 255f, bg.getGreen() / 255f, bg.getBlue() / 255f);
        cs.addRect(0, bandY, pageW, bandH);
        cs.fill();
    }

    private static float alignX(Alignment alignment, float marginLeft, float printW, float contentW) {
        return switch (alignment) {
            case LEFT   -> marginLeft + EDGE_INSET_PT;
            case RIGHT  -> marginLeft + printW - contentW - EDGE_INSET_PT;
            case CENTER -> marginLeft + (printW - contentW) / 2f;
        };
    }

    private static List<String> wrapPlainText(String text, PDType1Font font,
                                                float fontSize, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            float w = font.getStringWidth(candidate) / 1000f * fontSize;
            if (w <= maxWidth || cur.length() == 0) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.isEmpty() ? List.of(text) : out;
    }

    /**
     * Word-wrap a single styled line into multiple lines that each fit {@code maxWidth}.
     * Run styling is preserved across wraps; wrapping only occurs at whitespace
     * boundaries inside runs.
     */
    private static List<List<HtmlStyledTextRenderer.StyledRun>> wrapStyledLine(
            List<HtmlStyledTextRenderer.StyledRun> line, float maxWidth) {
        float lineW = 0f;
        for (HtmlStyledTextRenderer.StyledRun run : line) lineW += measureRunPt(run);
        if (lineW <= maxWidth) {
            List<List<HtmlStyledTextRenderer.StyledRun>> single = new ArrayList<>();
            single.add(new ArrayList<>(line));
            return single;
        }

        List<List<HtmlStyledTextRenderer.StyledRun>> out = new ArrayList<>();
        List<HtmlStyledTextRenderer.StyledRun> current = new ArrayList<>();
        float currentW = 0f;

        for (HtmlStyledTextRenderer.StyledRun run : line) {
            String[] tokens = splitKeepingSpaces(run.text());
            StringBuilder buf = new StringBuilder();
            float bufW = 0f;
            for (String tok : tokens) {
                float tokW = measureTextPt(tok, run);
                if (currentW + bufW + tokW <= maxWidth || (currentW == 0 && bufW == 0)) {
                    buf.append(tok);
                    bufW += tokW;
                } else {
                    // Flush the current buffered piece of THIS run
                    if (buf.length() > 0) {
                        current.add(new HtmlStyledTextRenderer.StyledRun(
                                buf.toString(), run.font(), run.color(),
                                run.underline(), run.strike()));
                        buf.setLength(0);
                        bufW = 0f;
                    }
                    // Break line
                    if (!current.isEmpty()) {
                        out.add(trimTrailingSpaces(current));
                        current = new ArrayList<>();
                        currentW = 0f;
                    }
                    String ltok = tok.stripLeading();
                    float ltokW = measureTextPt(ltok, run);
                    buf.append(ltok);
                    bufW += ltokW;
                }
            }
            if (buf.length() > 0) {
                current.add(new HtmlStyledTextRenderer.StyledRun(
                        buf.toString(), run.font(), run.color(),
                        run.underline(), run.strike()));
                currentW += bufW;
            }
        }
        if (!current.isEmpty()) out.add(current);
        return out.isEmpty() ? List.of(line) : out;
    }

    /** Splits on whitespace while preserving the whitespace as separate tokens. */
    private static String[] splitKeepingSpaces(String s) {
        if (s == null || s.isEmpty()) return new String[0];
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            boolean ws = Character.isWhitespace(ch);
            if (ws != inSpace && cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(ch);
            inSpace = ws;
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static List<HtmlStyledTextRenderer.StyledRun> trimTrailingSpaces(
            List<HtmlStyledTextRenderer.StyledRun> line) {
        if (line.isEmpty()) return line;
        HtmlStyledTextRenderer.StyledRun last = line.get(line.size() - 1);
        String trimmed = stripTrailing(last.text());
        if (!trimmed.equals(last.text())) {
            if (trimmed.isEmpty()) {
                line.remove(line.size() - 1);
            } else {
                line.set(line.size() - 1, new HtmlStyledTextRenderer.StyledRun(
                        trimmed, last.font(), last.color(), last.underline(), last.strike()));
            }
        }
        return line;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    /** Measure run width in points using its own font size (treated as points). */
    private static float measureRunPt(HtmlStyledTextRenderer.StyledRun run) {
        return measureTextPt(run.text(), run);
    }

    private static float measureTextPt(String text, HtmlStyledTextRenderer.StyledRun run) {
        if (text == null || text.isEmpty()) return 0f;
        try {
            PDType1Font font = resolveStandardFont(run.font());
            return font.getStringWidth(sanitize(text)) / 1000f * run.font().getSize2D();
        } catch (IOException e) {
            // Fallback: rough character-width estimate
            return text.length() * run.font().getSize2D() * 0.5f;
        }
    }

    /** Map an AWT Font's style to one of the four Helvetica base faces. */
    private static PDType1Font resolveStandardFont(Font awtFont) {
        int style = awtFont.getStyle();
        boolean bold   = (style & Font.BOLD)   != 0;
        boolean italic = (style & Font.ITALIC) != 0;
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold)           return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic)         return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private static String sanitize(String s) {
        return s.codePoints()
                .filter(cp -> cp >= 0x20 || cp == 0x09)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static String sanitizeAscii(String s) {
        return s.codePoints()
                .filter(cp -> cp >= 0x20 && cp < 128)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static void forceValidate(Component c) {
        if (c instanceof Container container) {
            container.doLayout();
            for (int i = 0; i < container.getComponentCount(); i++) {
                forceValidate(container.getComponent(i));
            }
        }
    }

}
