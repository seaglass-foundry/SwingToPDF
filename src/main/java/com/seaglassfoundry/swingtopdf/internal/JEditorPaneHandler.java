package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JViewport;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a {@link JEditorPane} with an {@link HTMLEditorKit} as PDF vector text.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Walk the {@link HTMLDocument} element tree recursively.</li>
 *   <li>For each leaf text run, resolve font and colour via
 *       {@link StyleSheet#getFont} / {@link StyleSheet#getForeground} (CSS-aware).</li>
 *   <li>Use {@link JEditorPane#modelToView2D(int)} to obtain positions the Swing HTML
 *       layout engine already computed  -- no re-implementation of HTML layout needed.</li>
 *   <li>Draw block-level backgrounds (e.g. coloured table header rows) before text.</li>
 *   <li>Draw table cell grid lines using collected row/cell geometry.</li>
 * </ol>
 *
 * <p>Non-HTML editor kits (RTF etc.) continue to fall back to {@link RasterFallback}.
 */
final class JEditorPaneHandler implements ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(JEditorPaneHandler.class);

    static final JEditorPaneHandler INSTANCE = new JEditorPaneHandler();

    private JEditorPaneHandler() {}

    // -----------------------------------------------------------------------

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JEditorPane pane)) return;

        // Only vectorise HTML; RTF and other kits fall back to raster.
        if (!(pane.getEditorKit() instanceof HTMLEditorKit)
                || !(pane.getDocument() instanceof HTMLDocument htmlDoc)) {
            log.debug("JEditorPaneHandler: non-HTML kit  -- rasterizing");
            RasterFallback.renderSized(comp, comp.getWidth(), comp.getHeight(), absX, absY, ctx);
            return;
        }

        int w = pane.getWidth();
        int h = pane.getHeight();
        if (w <= 0 || h <= 0) return;

        // Viewport scroll compensation (same logic as JTextPaneHandler)
        if (pane.getParent() instanceof JViewport) {
            absY -= pane.getY();
            absX -= pane.getX();
        }

        // Component background
        if (pane.isOpaque() && pane.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, w, h, pane.getBackground());
        }
        ContainerHandler.renderBorderOnly(pane, absX, absY, ctx);

        StyleSheet ss = htmlDoc.getStyleSheet();
        Element root  = htmlDoc.getDefaultRootElement();

        // Compute View hierarchy root once  -- used for exact element positioning
        if (pane.getUI() == null) {
            RasterFallback.renderSized(comp, w, h, absX, absY, ctx);
            return;
        }
        View rootView = pane.getUI().getRootView(pane);
        Shape rootAlloc = new Rectangle(0, 0, w, h);

        // Pass 1: block backgrounds (coloured rows, etc.) drawn before text
        renderBlockBackgrounds(pane, ss, root, absX, absY, rootView, rootAlloc, ctx);

        // Pass 2: table borders (collected during background pass, drawn here)
        renderTableBorders(pane, ss, root, absX, absY, ctx);

        // Pass 3: vector text leaves
        renderTextLeaves(pane, htmlDoc, ss, root, absX, absY, ctx);
    }

    // -----------------------------------------------------------------------
    // Pass 1  -- block backgrounds
    // -----------------------------------------------------------------------

    private void renderBlockBackgrounds(JEditorPane pane, StyleSheet ss,
                                        Element elem, int absX, int absY,
                                        View rootView, Shape rootAlloc,
                                        HandlerContext ctx) throws IOException {
        if (elem.isLeaf()) return;

        AttributeSet attrs = elem.getAttributes();
        if (attrs.isDefined(CSS.Attribute.BACKGROUND_COLOR)) {
            Color bg = ss.getBackground(attrs);
            if (bg != null && bg.getAlpha() > 0) {
                // Use View allocation for exact bounds (critical for table rows/cells)
                Rectangle2D viewBounds = findViewBounds(rootView, rootAlloc, elem);
                if (viewBounds != null) {
                    ctx.writer().fillRect(
                            (int)(absX + viewBounds.getX()),
                            (int)(absY + viewBounds.getY()),
                            (int) Math.ceil(viewBounds.getWidth()),
                            (int) Math.ceil(viewBounds.getHeight()),
                            bg);
                } else {
                    drawBlockRect(pane, elem, bg, absX, absY, pane.getWidth(), ctx);
                }
            }
        }

        for (int i = 0; i < elem.getElementCount(); i++) {
            renderBlockBackgrounds(pane, ss, elem.getElement(i), absX, absY,
                                   rootView, rootAlloc, ctx);
        }
    }

    /** Fallback: draws a filled rectangle that spans the full component width, vertically
     *  bounded by the element's first and last character positions. Used when View
     *  allocation is unavailable. */
    private void drawBlockRect(JEditorPane pane, Element elem, Color color,
                                int absX, int absY, int fullWidth,
                                HandlerContext ctx) throws IOException {
        try {
            Rectangle2D top = pane.modelToView2D(elem.getStartOffset());
            int lastOff = Math.max(elem.getStartOffset(), elem.getEndOffset() - 2);
            Rectangle2D bot = pane.modelToView2D(lastOff);
            if (top == null || bot == null) return;

            int ey = (int) top.getY();
            int eh = (int)(bot.getY() + bot.getHeight()) - ey;
            if (eh > 0) {
                ctx.writer().fillRect(absX, absY + ey, fullWidth, eh, color);
            }
        } catch (BadLocationException ignored) {}
    }

    // -----------------------------------------------------------------------
    // Pass 2  -- table cell borders
    // -----------------------------------------------------------------------

    private void renderTableBorders(JEditorPane pane, StyleSheet ss,
                                    Element root, int absX, int absY,
                                    HandlerContext ctx) throws IOException {
        collectAndDrawTables(pane, ss, root, absX, absY, ctx);
    }

    private void collectAndDrawTables(JEditorPane pane, StyleSheet ss,
                                      Element elem, int absX, int absY,
                                      HandlerContext ctx) throws IOException {
        if (elem.isLeaf()) return;

        Object tag = elem.getAttributes().getAttribute(AttributeSet.NameAttribute);
        if (tag == HTML.Tag.TABLE) {
            drawTable(pane, ss, elem, absX, absY, ctx);
            return; // table handles its own descendants
        }

        for (int i = 0; i < elem.getElementCount(); i++) {
            collectAndDrawTables(pane, ss, elem.getElement(i), absX, absY, ctx);
        }
    }

    /**
     * Draws the table grid.  We detect a border by checking the HTML {@code border}
     * attribute (integer &gt;= 1) on the TABLE element.  Cell bounds are obtained from the
     * Swing {@link View} hierarchy to get exact cell allocations (not text caret positions),
     * ensuring consistent column dividers across header and data rows.
     */
    private void drawTable(JEditorPane pane, StyleSheet ss,
                           Element tableElem, int absX, int absY,
                           HandlerContext ctx) throws IOException {
        // Honour border attribute  -- skip if border="0" or absent
        Object borderAttr = tableElem.getAttributes().getAttribute(HTML.Attribute.BORDER);
        int borderPx = 0;
        if (borderAttr != null) {
            try { borderPx = Integer.parseInt(borderAttr.toString().trim()); }
            catch (NumberFormatException ignored) {}
        }
        if (borderPx <= 0) return;

        // Structure: TABLE > TBODY (optional) > TR > TD|TH
        List<Element> rows = new ArrayList<>();
        collectRows(tableElem, rows);
        // Use View hierarchy to obtain exact cell allocation rectangles.
        // This avoids the problem of centred <th> text giving different x offsets
        // than left-aligned <td> text when using modelToView2D.
        if (rows.isEmpty() || (pane.getUI() == null)) return;
        View rootView = pane.getUI().getRootView(pane);
        Shape rootAlloc = new Rectangle(0, 0, pane.getWidth(), pane.getHeight());

        // Collect per-row list of cell rectangles
        List<List<Rectangle2D>> rowCellRects = new ArrayList<>();
        for (Element row : rows) {
            List<Element> cells = collectCells(row);
            if (cells.isEmpty()) continue;
            List<Rectangle2D> rects = new ArrayList<>();
            for (Element cell : cells) {
                Rectangle2D r = findViewBounds(rootView, rootAlloc, cell);
                if (r != null) rects.add(r);
            }
            if (!rects.isEmpty()) rowCellRects.add(rects);
        }

        if (rowCellRects.isEmpty()) return;

        // Derive table bounds from cell allocations
        double tableLeft = Double.MAX_VALUE, tableRight = 0;
        double tableTop = Double.MAX_VALUE, tableBot = 0;
        for (List<Rectangle2D> rects : rowCellRects) {
            for (Rectangle2D r : rects) {
                tableLeft  = Math.min(tableLeft,  r.getX());
                tableRight = Math.max(tableRight, r.getMaxX());
                tableTop   = Math.min(tableTop,   r.getY());
                tableBot   = Math.max(tableBot,   r.getMaxY());
            }
        }

        Color borderColor = Color.DARK_GRAY;
        float lw = borderPx;

        // Outer border
        ctx.writer().drawLine((float)(absX + tableLeft),  (float)(absY + tableTop),
                              (float)(absX + tableRight), (float)(absY + tableTop),  borderColor, lw);
        ctx.writer().drawLine((float)(absX + tableLeft),  (float)(absY + tableBot),
                              (float)(absX + tableRight), (float)(absY + tableBot),  borderColor, lw);
        ctx.writer().drawLine((float)(absX + tableLeft),  (float)(absY + tableTop),
                              (float)(absX + tableLeft),  (float)(absY + tableBot),  borderColor, lw);
        ctx.writer().drawLine((float)(absX + tableRight), (float)(absY + tableTop),
                              (float)(absX + tableRight), (float)(absY + tableBot),  borderColor, lw);

        // Horizontal row dividers
        for (int i = 1; i < rowCellRects.size(); i++) {
            float ry = (float)(absY + rowCellRects.get(i).get(0).getY());
            ctx.writer().drawLine((float)(absX + tableLeft), ry,
                                  (float)(absX + tableRight), ry, borderColor, lw);
        }

        // Vertical column dividers  -- use first row's cell x positions (columns are
        // consistent across rows in an HTML table), drawn full table height.
        List<Rectangle2D> refRow = rowCellRects.get(0);
        for (int ci = 1; ci < refRow.size(); ci++) {
            float cx = (float)(absX + refRow.get(ci).getX());
            ctx.writer().drawLine(cx, (float)(absY + tableTop),
                                  cx, (float)(absY + tableBot), borderColor, lw);
        }
    }

    /**
     * Walks the Swing {@link View} tree to find the view whose element matches
     * {@code target} and returns its allocation rectangle, or {@code null}.
     */
    private Rectangle2D findViewBounds(View parent, Shape parentAlloc, Element target) {
        if (parent.getElement() == target) {
            return parentAlloc.getBounds2D();
        }
        for (int i = 0; i < parent.getViewCount(); i++) {
            View child = parent.getView(i);
            Shape childAlloc = parent.getChildAllocation(i, parentAlloc);
            if (childAlloc == null) continue;
            Rectangle2D found = findViewBounds(child, childAlloc, target);
            if (found != null) return found;
        }
        return null;
    }

    private void collectRows(Element elem, List<Element> out) {
        if (elem.isLeaf()) return;
        Object tag = elem.getAttributes().getAttribute(AttributeSet.NameAttribute);
        if (tag == HTML.Tag.TR) { out.add(elem); return; }
        for (int i = 0; i < elem.getElementCount(); i++) collectRows(elem.getElement(i), out);
    }

    private List<Element> collectCells(Element row) {
        List<Element> cells = new ArrayList<>();
        for (int i = 0; i < row.getElementCount(); i++) {
            Element c = row.getElement(i);
            Object tag = c.getAttributes().getAttribute(AttributeSet.NameAttribute);
            if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) cells.add(c);
        }
        return cells;
    }

    /** Returns {@code true} if {@code elem} is inside a {@code <th>} element. */
    private boolean isInsideTh(Element elem) {
        Element e = elem.getParentElement();
        while (e != null) {
            Object tag = e.getAttributes().getAttribute(AttributeSet.NameAttribute);
            if (tag == HTML.Tag.TH) return true;
            if (tag == HTML.Tag.TD || tag == HTML.Tag.TABLE) return false;
            e = e.getParentElement();
        }
        return false;
    }


    // -----------------------------------------------------------------------
    // Pass 3  -- vector text leaves
    // -----------------------------------------------------------------------

    private void renderTextLeaves(JEditorPane pane, HTMLDocument doc, StyleSheet ss,
                                  Element elem, int absX, int absY,
                                  HandlerContext ctx) throws IOException {
        if (elem.isLeaf()) {
            renderLeaf(pane, doc, ss, elem, absX, absY, ctx);
        } else {
            for (int i = 0; i < elem.getElementCount(); i++) {
                renderTextLeaves(pane, doc, ss, elem.getElement(i), absX, absY, ctx);
            }
        }
    }

    private void renderLeaf(JEditorPane pane, HTMLDocument doc, StyleSheet ss,
                             Element leaf, int absX, int absY,
                             HandlerContext ctx) throws IOException {
        int start = leaf.getStartOffset();
        int end   = leaf.getEndOffset();

        AttributeSet attrs = leaf.getAttributes();

        // StyleSheet resolves CSS font attributes (family, size, weight, style).
        // CSS inheritance (e.g. font-weight:bold from <th>) is not reflected in
        // leaf attributes, so we check ancestor elements explicitly.
        Font font = ss.getFont(attrs);
        if (font == null) font = pane.getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        if (!font.isBold() && isInsideTh(leaf)) {
            font = font.deriveFont(font.getStyle() | Font.BOLD);
        }

        // StyleSheet resolves CSS color attribute
        Color fg = ss.getForeground(attrs);
        if (fg == null) fg = pane.getForeground();
        if (fg == null) fg = Color.BLACK;

        FontMetrics fm = pane.getFontMetrics(font);

        // Line-by-line rendering to handle soft word-wrap (same as JTextPaneHandler)
        int pos = start;
        while (pos < end) {
            int rowEnd;
            try {
                rowEnd = Utilities.getRowEnd(pane, pos);
            } catch (BadLocationException e) {
                break;
            }
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
                        ctx.writer().drawText(seg, ctx.fontMapper().resolve(font),
                                              font.getSize2D(), fg,
                                              absX + rx, absY + baseline);
                    }
                }
            } catch (BadLocationException ignored) {}

            pos = segEnd;
        }
    }
}
