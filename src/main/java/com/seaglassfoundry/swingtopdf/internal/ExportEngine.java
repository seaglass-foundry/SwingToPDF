package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeModel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.SwingPdfExportException;

/**
 * Orchestrates the full export pipeline for a single {@link ExportConfig}.
 *
 * <p>This class is internal API; it is not exported from the module.
 * The public entry point is {@code SwingPdfExporter}.
 */
public final class ExportEngine {

    private static final Logger log = LoggerFactory.getLogger(ExportEngine.class);

    private final ExportConfig config;

    public ExportEngine(ExportConfig config) {
        this.config = config;
    }

    public void export(Path outputPath) {
        log.debug("Exporting to file: {}", outputPath);
        try (OutputStream os = Files.newOutputStream(outputPath)) {
            export(os);
        } catch (IOException e) {
            throw new SwingPdfExportException("Failed to write PDF to " + outputPath, e);
        }
    }

    public void export(OutputStream outputStream) {
        log.debug("Exporting to stream; root={}, pageSize={}, orientation={}, mode={}",
                config.root().getClass().getSimpleName(),
                config.pageSize(),
                config.orientation(),
                config.exportMode());

        try (PDDocument doc = new PDDocument()) {
            applyMetadata(doc);
            AcroFormEmitter acroFormEmitter = config.acroFormEnabled()
                    ? new AcroFormEmitter(doc) : null;
            renderPages(doc, acroFormEmitter);
            if (acroFormEmitter != null) acroFormEmitter.finalizeGroups();
            doc.save(outputStream);
        } catch (IOException e) {
            throw new SwingPdfExportException("Failed to render PDF", e);
        }
    }

    // -----------------------------------------------------------------------

    private void applyMetadata(PDDocument doc) {
        PDDocumentInformation info = doc.getDocumentInformation();
        if (config.title()    != null) info.setTitle(config.title());
        if (config.author()   != null) info.setAuthor(config.author());
        if (config.subject()  != null) info.setSubject(config.subject());
        if (config.keywords() != null) info.setKeywords(config.keywords());
        info.setCreator("swingtopdf");
    }

    private void renderPages(PDDocument doc, AcroFormEmitter acroFormEmitter) throws IOException {
        JComponent root = config.root();
        // Compute the printable width in Swing pixels so that LayoutEnsurer can
        // size zero-width components to fill the page (e.g. BoxLayout panels).
        float baseScaleForHint = 72f / config.dpi();
        int printWidthPx = (int) (config.printableWidthPt() / baseScaleForHint);
        LayoutEnsurer.ensureLayout(root, printWidthPx);

        // In DATA_REPORT mode, expand all JScrollPane views to their full preferred
        // size BEFORE measuring dimensions or computing page breaks. This ensures
        // the page count accommodates expanded content.
        ExpansionState expansions = config.exportMode() == ExportMode.DATA_REPORT
                ? EdtHelper.callOnEdt(() -> expandScrollPanes(root))
                : new ExpansionState(List.of(), List.of());

        try {
            int compW = EdtHelper.callOnEdt(root::getWidth);
            // When root is the view of a scrolled JViewport (e.g. a content JPanel exported
            // directly while its enclosing JScrollPane is scrolled), root.getY() is negative
            // (= -scrollOffset). Normalise so root's top-left corner always lands at PDF origin.
            int rootOffX = -EdtHelper.callOnEdt(root::getX);
            int rootOffY = -EdtHelper.callOnEdt(root::getY);

            // Compute effective height: accounts for expanded scroll content AND
            // DATA_REPORT model-based content that extends beyond component bounds
            // (e.g. JTable/JList/JTree rendering all model rows).
            int visualH = EdtHelper.callOnEdt(() -> computeEffectiveHeight(root, rootOffX, rootOffY));
            List<Float> rowBounds = EdtHelper.callOnEdt(() -> collectRowBounds(root, rootOffX, rootOffY));
            Collections.sort(rowBounds);
            float maxRowBound = rowBounds.isEmpty() ? 0f : rowBounds.get(rowBounds.size() - 1);
            int compH = Math.max(visualH, (int) Math.ceil(maxRowBound));

            // Collect every JTable header so we can both (a) reserve space in
            // the page-break computation for the repeated header on
            // continuation pages, and (b) draw those repeated headers in a
            // reserved band at the top of continuation pages.
            List<TableHeaderInfo> tableHeaders = EdtHelper.callOnEdt(
                    () -> collectTableHeaders(root, rootOffX, rootOffY));

            float dpi        = config.dpi();
            float baseScale  = 72f / dpi;
            float marginLeft = config.margins()[3];
            float marginTop  = config.margins()[0];
            float printW     = config.printableWidthPt();
            float printH     = config.printableHeightPt();
            float pageW      = config.effectivePageWidthPt();
            float pageH      = config.effectivePageHeightPt();

            // Scale down horizontally if component is wider than the printable area
            float compWpt   = compW * baseScale;
            float fitScale  = compWpt > printW ? printW / compWpt : 1.0f;
            float scale     = baseScale * fitScale;
            float pageWidthPx = printW / scale;   // printable width in Swing pixels

            float stepPx    = printH / scale; // page height in Swing pixels
            List<Float> breaksPx = computePageBreaks(root, compH, stepPx, rootOffX, rootOffY, rowBounds, tableHeaders);
            int   numPages  = Math.max(1, breaksPx.size());

            log.debug("Rendering {} page(s): comp={}x{} px, scale={:.4f}, fitScale={:.4f}",
                      numPages, compW, compH, scale, fitScale);

            DefaultFontMapper        fontMapper    = new DefaultFontMapper(doc, config.fontResolver());
            HandlerRegistry          registry      = buildRegistry();
            DeduplicatingImageEncoder imageEncoder = new DeduplicatingImageEncoder(config.imageHandler());

            for (int pageIdx = 0; pageIdx < numPages; pageIdx++) {
                float sliceTopPx = breaksPx.get(pageIdx);
                float sliceBottomPx = (pageIdx + 1 < breaksPx.size())
                        ? breaksPx.get(pageIdx + 1) : compH;

                // On continuation pages, reserve a band at the top of the
                // content area for repeating each table's column header.
                // headerReservePt is the total PDF height consumed.
                float headerReservePt = 0f;
                List<TableHeaderInfo> continuingHeaders = new ArrayList<>();
                if (pageIdx > 0) {
                    for (TableHeaderInfo th : tableHeaders) {
                        if (th.tableAbsY() < sliceTopPx) {
                            continuingHeaders.add(th);
                            headerReservePt += th.headerHeightPx() * scale;
                        }
                    }
                }

                PDPage page = new PDPage(new PDRectangle(pageW, pageH));
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    // Content writer: marginTop is shifted down by the reserved
                    // band so rows at their natural positions fit below the
                    // repeated header. printableHeightPt is similarly reduced
                    // to clamp the clip rect.
                    float contentMarginTop     = marginTop + headerReservePt;
                    float contentPrintableH    = printH    - headerReservePt;
                    PdfPageWriter writer = new PdfPageWriter(
                            cs, scale, marginLeft, contentMarginTop, pageH, sliceTopPx, stepPx,
                            printW, contentPrintableH);
                    HandlerContext ctx = new HandlerContext(
                            writer, fontMapper, config, doc, page,
                            sliceTopPx, sliceBottomPx, pageWidthPx, imageEncoder, acroFormEmitter);
                    ComponentTraverser traverser = new ComponentTraverser(registry, ctx);
                    ctx.setTraverser(traverser);

                    writer.applyPageClip(sliceBottomPx - sliceTopPx);

                    final int fPageIdx = pageIdx;
                    EdtHelper.runOnEdt(() -> {
                        try {
                            traverser.traverse(root, rootOffX, rootOffY);
                        } catch (IOException e) {
                            throw new SwingPdfExportException(
                                    "Rendering failed on page " + (fPageIdx + 1), e);
                        }
                    });

                    writer.restore(); // pop clip rect before reserved band + header/footer

                    // Render repeated JTable column headers in the reserved
                    // band at the top of the content area (unclipped). Each
                    // header is stacked below the previous one.
                    if (!continuingHeaders.isEmpty()) {
                        drawRepeatedTableHeaders(
                                cs, continuingHeaders, sliceTopPx,
                                scale, marginLeft, marginTop, pageH, printW,
                                fontMapper, registry, imageEncoder,
                                pageWidthPx, doc, page);
                    }

                    int pageNum = pageIdx + 1;
                    if (config.header() != null)
                        HeaderFooterRenderer.draw(config.header(), doc, page, cs,
                                 pageNum, numPages,
                                 pageW, pageH, marginLeft, config.margins()[1],
                                 config.margins()[0], true,
                                 fontMapper, registry, imageEncoder, config, baseScale);
                    if (config.footer() != null)
                        HeaderFooterRenderer.draw(config.footer(), doc, page, cs,
                                 pageNum, numPages,
                                 pageW, pageH, marginLeft, config.margins()[1],
                                 config.margins()[2], false,
                                 fontMapper, registry, imageEncoder, config, baseScale);
                }

                log.debug("Page {} rendered (sliceTopPx={:.1f})", pageIdx + 1, sliceTopPx);
            }
        } finally {
            if (!expansions.scrollExpansions().isEmpty()) {
                EdtHelper.runOnEdt(() -> restoreScrollPanes(expansions));
            }
        }
    }

    /**
     * Compute page-break positions (in Swing pixels from the root top).
     *
     * <p>Starts from 0 and advances by {@code stepPx} per page. Two snapping
     * rules are applied at each break, in priority order:
     * <ol>
     *   <li><b>Keep-together</b>: if the ideal break would split a component
     *       marked with {@link SwingPdfExporter#KEEP_TOGETHER}, the break snaps
     *       UP to that component's top edge (provided it fits on one page).</li>
     *   <li><b>JTable row boundary</b>: otherwise, the break snaps DOWN to the
     *       bottom of the last complete row that fits, preventing rows from
     *       being cut in half.</li>
     * </ol>
     */
    private List<Float> computePageBreaks(JComponent root, float formHPx, float stepPx,
                                           int offX, int offY, List<Float> rowBounds,
                                           List<TableHeaderInfo> tableHeaders) {

        List<float[]> keepBounds = EdtHelper.callOnEdt(
                () -> collectKeepTogetherBounds(root, offX, offY, stepPx));

        List<Float> breaks = new ArrayList<>();
        float cursor = 0f;
        while (cursor < formHPx) {
            breaks.add(cursor);

            // On continuation pages (cursor > 0), each JTable that started on
            // a previous page has its column header repeated at the top of
            // this page. Reserve that vertical space so rows don't overflow.
            float headerReservePx = 0f;
            if (cursor > 0) {
                for (TableHeaderInfo th : tableHeaders) {
                    if (th.tableAbsY() < cursor) headerReservePx += th.headerHeightPx();
                }
            }
            float ideal = cursor + stepPx - headerReservePx;
            if (ideal >= formHPx) break;

            // 1. Keep-together: snap UP to the top of the earliest component
            //    that would be split, if it fits on a single page.
            float ktSnap = Float.MAX_VALUE;
            for (float[] kt : keepBounds) {
                float top = kt[0], bot = kt[1];
                if (top > cursor && top < ideal && bot > ideal && (bot - top) <= stepPx) {
                    ktSnap = Math.min(ktSnap, top);
                }
            }

            float snapped;
            if (ktSnap < Float.MAX_VALUE) {
                snapped = ktSnap;
            } else {
                // 2. Row boundary: snap DOWN to the last complete row <= ideal.
                snapped = ideal;
                for (int i = rowBounds.size() - 1; i >= 0; i--) {
                    float b = rowBounds.get(i);
                    if (b <= ideal && b > cursor) {
                        snapped = b;
                        break;
                    }
                }
            }
            cursor = snapped;
        }
        return breaks;
    }

    /**
     * Metadata for a JTable whose column header should be repeated on
     * continuation pages: its absolute position in root space, the header's
     * height in Swing pixels, and a reference to the JTableHeader itself
     * (needed to re-draw it in the reserved band).
     */
    private record TableHeaderInfo(int tableAbsX, float tableAbsY,
                                    float headerHeightPx,
                                    javax.swing.table.JTableHeader header) {}

    /** Collect one {@link TableHeaderInfo} per JTable in the tree. */
    private static List<TableHeaderInfo> collectTableHeaders(Component comp, int offX, int offY) {
        List<TableHeaderInfo> result = new ArrayList<>();
        collectTableHeadersFrom(comp, offX, offY, result);
        return result;
    }

    private static void collectTableHeadersFrom(Component comp, int offX, int offY,
                                                 List<TableHeaderInfo> result) {
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;
        int absY = offY + comp.getY();
        int absX = offX + comp.getX();

        if (comp instanceof JTable table) {
            javax.swing.table.JTableHeader header = table.getTableHeader();
            if (header != null && header.getHeight() > 0) {
                result.add(new TableHeaderInfo(absX, absY, header.getHeight(), header));
            }
        }

        if (comp instanceof Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                collectTableHeadersFrom(c.getComponent(i), absX, absY, result);
            }
        }
    }

    /**
     * Draw the repeated JTable column headers in the reserved band at the
     * top of the content area. The band lives between pdfY {@code pageH - marginTop}
     * (top of content) and pdfY {@code pageH - marginTop - sum(headerHeight*scale)}.
     * Each header stacks below the previous one.
     */
    private static void drawRepeatedTableHeaders(
            PDPageContentStream cs,
            List<TableHeaderInfo> continuingHeaders,
            float sliceTopPx,
            float scale, float marginLeft, float marginTop, float pageH, float printW,
            DefaultFontMapper fontMapper, HandlerRegistry registry,
            DeduplicatingImageEncoder imageEncoder,
            float pageWidthPx, PDDocument doc, PDPage page) throws IOException {

        // A dedicated writer whose content area coincides with the reserved
        // band. marginTop = the original top margin (so pdfY at swing Y =
        // sliceTopPx maps to the top of the content area).
        float bandHeightPt = 0f;
        for (TableHeaderInfo th : continuingHeaders) bandHeightPt += th.headerHeightPx() * scale;
        float bandHeightPx = bandHeightPt / scale;

        PdfPageWriter bandWriter = new PdfPageWriter(
                cs, scale, marginLeft, marginTop, pageH,
                sliceTopPx, bandHeightPx,
                printW, bandHeightPt);
        HandlerContext bandCtx = new HandlerContext(
                bandWriter, fontMapper, null /* config unused by header handler */, doc, page,
                sliceTopPx, sliceTopPx + bandHeightPx, pageWidthPx, imageEncoder,
                null /* no AcroForm in reserved band */);
        // No traverser required: JTableHeaderHandler doesn't recurse.
        bandCtx.setTraverser(new ComponentTraverser(registry, bandCtx));

        // Stack each header at swing Y = sliceTopPx + cumulative previous heights.
        int cursorPx = (int) sliceTopPx;
        for (TableHeaderInfo th : continuingHeaders) {
            JTableHeaderHandler.renderAt(th.header(), th.tableAbsX(), cursorPx, bandCtx);
            cursorPx += (int) th.headerHeightPx();
        }
    }

    /**
     * Collect absolute Y positions of all row boundaries under {@code comp}.
     * Handles {@link JTable}, {@link JList}, and {@link JTree}.
     */
    private static List<Float> collectRowBounds(Component comp, int offX, int offY) {
        List<Float> result = new ArrayList<>();
        collectRowBoundsFrom(comp, offX, offY, result);
        return result;
    }

    private static void collectRowBoundsFrom(Component comp, int offX, int offY,
                                              List<Float> result) {
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;
        int absY = offY + comp.getY();
        int absX = offX + comp.getX();

        if (comp instanceof JTable table) {
            int cumH = absY;
            int rowCount = table.getModel().getRowCount();
            for (int row = 0; row < rowCount; row++) {
                cumH += table.getRowHeight(row);
                result.add((float) cumH);
            }
            // fall through: recurse into children (picks up JTableHeader via scroll pane)
        } else if (comp instanceof JList<?> list) {
            collectListRowBounds(list, absY, result);
            return; // no meaningful Swing children to recurse into
        } else if (comp instanceof JTree tree) {
            collectTreeRowBounds(tree, absY, result);
            return; // no meaningful Swing children to recurse into
        }

        if (comp instanceof Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                collectRowBoundsFrom(c.getComponent(i), absX, absY, result);
            }
        }
    }

    /**
     * Emit the bottom-Y of every item in the list model, stacked from {@code absY}.
     * Uses {@link JList#getFixedCellHeight()} when set; falls back to the renderer's
     * preferred height (or 16 px) for variable-height lists.
     */
    @SuppressWarnings("unchecked")
    private static void collectListRowBounds(JList<?> list, int absY, List<Float> result) {
        int total = list.getModel().getSize();
        int fixed = list.getFixedCellHeight();
        ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) list.getCellRenderer();
        int curH = absY;
        for (int i = 0; i < total; i++) {
            int cellH;
            if (fixed > 0) {
                cellH = fixed;
            } else {
                Object value = list.getModel().getElementAt(i);
                Component cell = renderer.getListCellRendererComponent(
                        list, value, i, false, false);
                Dimension pref = cell.getPreferredSize();
                cellH = (pref != null && pref.height > 0) ? pref.height : 16;
            }
            curH += cellH;
            result.add((float) curH);
        }
    }

    /**
     * Emit the bottom-Y of every node in the tree model, stacked from {@code absY}.
     * Walks the entire model (not just expanded rows) so that DATA_REPORT output
     * is covered. Uses {@link JTree#getRowHeight()} or 18 px as the row height.
     */
    private static void collectTreeRowBounds(JTree tree, int absY, List<Float> result) {
        int fallbackH = UIManager.getInt("Tree.rowHeight");
        if (fallbackH <= 0) fallbackH = 18;
        int rowH = tree.getRowHeight() > 0 ? tree.getRowHeight() : fallbackH;
        TreeModel model = tree.getModel();
        Object root = model.getRoot();
        int[] curY = { absY };
        if (tree.isRootVisible()) {
            curY[0] += rowH;
            result.add((float) curY[0]);
        }
        collectTreeNodeBounds(model, root, rowH, curY, result);
    }

    private static void collectTreeNodeBounds(TreeModel model, Object node,
                                               int rowH, int[] curY, List<Float> result) {
        int childCount = model.getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(node, i);
            curY[0] += rowH;
            result.add((float) curY[0]);
            if (!model.isLeaf(child)) {
                collectTreeNodeBounds(model, child, rowH, curY, result);
            }
        }
    }

    /**
     * Collect absolute Y extents of components that should not be split across
     * page boundaries. Includes:
     * <ul>
     *   <li>Components explicitly marked with {@link SwingPdfExporter#KEEP_TOGETHER}</li>
     *   <li>Atomic widgets (form fields, buttons, labels, etc.) automatically
     *       detected via {@link #isAtomicWidget}</li>
     * </ul>
     * Returns a list of {@code float[]{topY, bottomY}}.
     */
    private static List<float[]> collectKeepTogetherBounds(Component comp,
                                                            int offX, int offY,
                                                            float stepPx) {
        List<float[]> result = new ArrayList<>();
        collectKeepTogetherFrom(comp, offX, offY, stepPx, result);
        return result;
    }

    private static void collectKeepTogetherFrom(Component comp, int offX, int offY,
                                                 float stepPx, List<float[]> result) {
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;
        int absY = offY + comp.getY();
        int absX = offX + comp.getX();
        int h    = comp.getHeight();

        // Explicit KEEP_TOGETHER  -- don't recurse into children
        if (comp instanceof JComponent jc &&
                Boolean.TRUE.equals(jc.getClientProperty(SwingPdfExporter.KEEP_TOGETHER))) {
            result.add(new float[]{ absY, absY + h });
            return;
        }

        // Auto keep-together: atomic widgets that fit on one page.
        // This prevents form fields, buttons, labels, sliders, etc. from
        // being bisected at page boundaries.
        if (h <= stepPx && isAtomicWidget(comp)) {
            result.add(new float[]{ absY, absY + h });
        }

        if (comp instanceof Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                collectKeepTogetherFrom(c.getComponent(i), absX, absY, stepPx, result);
            }
        }
    }

    /**
     * Returns true if the component is an atomic widget that should not be
     * split across page boundaries. Data components (JTable, JList, JTree)
     * handle their own row-level pagination and are excluded. Pure layout
     * containers (JScrollPane, JViewport, large JPanels) are excluded so
     * that their children can be individually protected.
     */
    private static boolean isAtomicWidget(Component comp) {
        if (comp instanceof JTable || comp instanceof JList || comp instanceof JTree) return false;
        if (comp instanceof JScrollPane || comp instanceof JViewport) return false;
        // Large JPanels with children are layout containers, not atomic widgets.
        // Small panels (e.g. form rows ~36px) are treated as atomic.
        if (comp instanceof JPanel panel && panel.getComponentCount() > 0
                && comp.getHeight() > 80) return false;
        return true;
    }

    // -----------------------------------------------------------------------
    // JScrollPane pre-render expansion (DATA_REPORT mode)
    // -----------------------------------------------------------------------

    /**
     * Saved state of a JScrollPane and its view before expansion.
     */
    private record ScrollExpansion(
            JScrollPane scrollPane, Dimension savedSpSize,
            Dimension savedVpSize,
            Component view, int savedX, int savedY, Dimension savedSize) {}

    /**
     * Saved state of a component whose position or size was adjusted during
     * scroll pane expansion propagation.
     */
    private record LayoutAdjustment(Component comp, int savedY, Dimension savedSize) {}

    /**
     * Combined result of scroll pane expansion: the per-view state and the
     * ancestor/sibling adjustments needed to prevent overlap.
     */
    private record ExpansionState(List<ScrollExpansion> scrollExpansions,
                                  List<LayoutAdjustment> layoutAdjustments) {}

    /**
     * Expand all JScrollPane views to their full preferred size. Must be called
     * on the EDT. Returns saved state for {@link #restoreScrollPanes}.
     */
    private static ExpansionState expandScrollPanes(Component comp) {
        List<ScrollExpansion> scrollExps = new ArrayList<>();
        List<LayoutAdjustment> layoutAdjs = new ArrayList<>();
        expandScrollPanesFrom(comp, scrollExps, layoutAdjs);
        return new ExpansionState(scrollExps, layoutAdjs);
    }

    private static void expandScrollPanesFrom(Component comp,
            List<ScrollExpansion> scrollExps, List<LayoutAdjustment> layoutAdjs) {
        // Recurse into children FIRST so that inner (nested) scroll panes expand
        // before their outer containers. This ensures that when an outer scroll
        // pane measures its view's preferred size, the inner content is already
        // at its expanded height  -- preventing height double-counting.
        if (comp instanceof Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                expandScrollPanesFrom(c.getComponent(i), scrollExps, layoutAdjs);
            }
        }
        if (comp instanceof JScrollPane sp) {
            JViewport vp = sp.getViewport();
            if (vp != null && vp.getView() != null) {
                Component view = vp.getView();
                scrollExps.add(new ScrollExpansion(
                        sp, sp.getSize(), vp.getSize(),
                        view, view.getX(), view.getY(), view.getSize()));

                view.setLocation(0, 0); // reset scroll offset

                // For wrapping text components (JTextArea with line wrap, JTextPane, etc.),
                // the preferred height depends on the wrap width. Set the view's width to
                // the viewport width before measuring preferred size.
                if (view instanceof javax.swing.Scrollable scrollable
                        && scrollable.getScrollableTracksViewportWidth()) {
                    view.setSize(vp.getWidth(), view.getHeight());
                }

                // JTextArea's getPreferredSize() is constrained by its rows/columns
                // properties, which represent an initial hint, not the content size.
                // Temporarily zero them so preferred size reflects actual content.
                int savedRows = 0, savedCols = 0;
                boolean isTextArea = view instanceof JTextArea;
                if (isTextArea) {
                    JTextArea ta = (JTextArea) view;
                    savedRows = ta.getRows();
                    savedCols = ta.getColumns();
                    ta.setRows(0);
                    ta.setColumns(0);
                }

                Dimension preferred;
                try {
                    preferred = view.getPreferredSize();
                } finally {
                    if (isTextArea) {
                        ((JTextArea) view).setRows(savedRows);
                        ((JTextArea) view).setColumns(savedCols);
                    }
                }
                Dimension current   = view.getSize();
                int newW = Math.max(current.width, preferred.width);
                int newH = Math.max(current.height, preferred.height);
                if (newW != current.width || newH != current.height) {
                    view.setSize(newW, newH);
                    view.validate();
                }

                // The view may already be at its full content height (Swing sizes
                // the view independently of the viewport). Compare against the
                // VIEWPORT height to determine the expansion delta.
                int expandedViewH = Math.max(newH, view.getHeight());
                int deltaH = expandedViewH - vp.getHeight();
                if (deltaH > 0) {
                    // Expand viewport and scroll pane to match the full view
                    vp.setSize(vp.getWidth(), vp.getHeight() + deltaH);
                    sp.setSize(sp.getWidth(), sp.getHeight() + deltaH);
                    // Propagate the height increase up through the ancestor chain
                    // so sibling components shift down and don't paint over content.
                    propagateHeightDelta(sp, deltaH, layoutAdjs);
                }
            }
        }
    }

    /**
     * Walk up from {@code from} through all ancestors, expanding each container's
     * height by {@code deltaH} and shifting all siblings that are visually below
     * the expanded branch downward by the same amount.
     *
     * <p>Siblings are identified by Y position (not component index order) so that
     * this works correctly with layout managers where add-order differs from
     * visual order (e.g. BorderLayout, GridBagLayout, null layout).
     */
    private static void propagateHeightDelta(Component from, int deltaH,
                                              List<LayoutAdjustment> adjustments) {
        Component child = from;
        Container parent = child.getParent();
        while (parent != null) {
            // The bottom edge of the expanded child BEFORE the delta was applied
            // to the parent. Siblings whose top edge is at or below this line
            // need to shift down.
            int expandedBottomY = child.getY() + child.getHeight();
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component sibling = parent.getComponent(i);
                if (sibling == child) continue;
                if (sibling.getHeight() > 0 && sibling.getY() >= expandedBottomY - deltaH) {
                    adjustments.add(new LayoutAdjustment(sibling, sibling.getY(), sibling.getSize()));
                    sibling.setLocation(sibling.getX(), sibling.getY() + deltaH);
                }
            }
            // Expand this container's height
            adjustments.add(new LayoutAdjustment(parent, parent.getY(), parent.getSize()));
            parent.setSize(parent.getWidth(), parent.getHeight() + deltaH);

            child = parent;
            parent = parent.getParent();
        }
    }

    /**
     * Restore all JScrollPane views and layout adjustments to their original state.
     * Must be called on the EDT. Restores in reverse order.
     */
    private static void restoreScrollPanes(ExpansionState state) {
        // Restore layout adjustments (sibling positions, ancestor sizes) in reverse
        List<LayoutAdjustment> adjs = state.layoutAdjustments;
        for (int i = adjs.size() - 1; i >= 0; i--) {
            LayoutAdjustment adj = adjs.get(i);
            adj.comp.setLocation(adj.comp.getX(), adj.savedY);
            adj.comp.setSize(adj.savedSize);
        }
        // Restore scroll pane views, viewports, and scroll panes
        List<ScrollExpansion> exps = state.scrollExpansions;
        for (int i = exps.size() - 1; i >= 0; i--) {
            ScrollExpansion exp = exps.get(i);
            exp.scrollPane.setSize(exp.savedSpSize);
            JViewport vp = exp.scrollPane.getViewport();
            if (vp != null) vp.setSize(exp.savedVpSize);
            exp.view.setSize(exp.savedSize);
            exp.view.validate();
            exp.view.setLocation(exp.savedX, exp.savedY);
        }
    }

    /**
     * Compute the effective height of the component tree, accounting for
     * children that overflow their parent bounds (e.g. expanded JScrollPane views).
     * Must be called on the EDT.
     */
    private static int computeEffectiveHeight(Component comp, int offX, int offY) {
        int absY = offY + comp.getY();
        int bottom = absY + comp.getHeight();

        if (comp instanceof Container c) {
            for (int i = 0; i < c.getComponentCount(); i++) {
                Component child = c.getComponent(i);
                if (child.getWidth() <= 0 || child.getHeight() <= 0) continue;
                int childAbsX = offX + comp.getX();
                bottom = Math.max(bottom, computeEffectiveHeight(child, childAbsX, absY));
            }
        }
        return bottom;
    }

    /**
     * Build and populate the handler registry with all built-in handlers.
     * More-specific types must be registered before their supertypes.
     */
    private HandlerRegistry buildRegistry() {
        HandlerRegistry r = new HandlerRegistry();

        // Specific handlers (registered before their supertypes)
        r.register(JLabel.class,        JLabelHandler.INSTANCE);

        // Text components  -- most-specific first so JPasswordField wins over JTextField
        // which wins over JTextComponent.
        // JTextPane: vector text via StyledDocument element walk + modelToView2D positioning.
        // Registered before JEditorPane so the exact-class match wins.
        r.register(JTextPane.class,      JTextPaneHandler.INSTANCE);
        // JEditorPane: vector text for HTML kit; rasterizes non-HTML kits.
        // Registered before JTextComponent so it doesn't fall through to getText().
        r.register(JEditorPane.class,    JEditorPaneHandler.INSTANCE);
        r.register(JPasswordField.class, JTextComponentHandler.INSTANCE);
        r.register(JTextField.class,     JTextComponentHandler.INSTANCE);
        r.register(JTextArea.class,      JTextComponentHandler.INSTANCE);
        r.register(JTextComponent.class, JTextComponentHandler.INSTANCE);

        // Buttons  -- JCheckBox and JRadioButton before the AbstractButton catch-all.
        r.register(JCheckBox.class,      AbstractButtonHandler.INSTANCE);
        r.register(JRadioButton.class,   AbstractButtonHandler.INSTANCE);
        r.register(AbstractButton.class, AbstractButtonHandler.INSTANCE);

        // Table  -- header handled separately (it lives in the scroll pane's column-header
        // viewport and is visited by the traverser at the correct absolute position).
        r.register(JTableHeader.class,   JTableHeaderHandler.INSTANCE);
        r.register(JTable.class,         JTableHandler.INSTANCE);

        // Tabbed pane
        r.register(JTabbedPane.class,    JTabbedPaneHandler.INSTANCE);

        // List and tree
        r.register(JList.class,          JListHandler.INSTANCE);
        r.register(JTree.class,          JTreeHandler.INSTANCE);

        // Selection and value widgets
        r.register(JComboBox.class,      JComboBoxHandler.INSTANCE);
        r.register(JProgressBar.class,   JProgressBarHandler.INSTANCE);
        r.register(JSlider.class,        JSliderHandler.INSTANCE);
        r.register(JScrollBar.class,     JScrollBarHandler.INSTANCE);
        r.register(JSpinner.class,       JSpinnerHandler.INSTANCE);

        // Separators  -- covers JSeparator, JToolBar.Separator, JPopupMenu.Separator
        r.register(JSeparator.class,     JSeparatorHandler.INSTANCE);

        // Box.Filler  -- the invisible spacer produced by Box.createVerticalStrut,
        // createHorizontalStrut, createGlue, and createRigidArea. It is a pure
        // layout component with no visual content; register it so it is never
        // handed to RasterFallback.
        r.register(Box.Filler.class,    ContainerHandler.INSTANCE);

        // Niche containers with custom rendering
        r.register(JSplitPane.class,    JSplitPaneHandler.INSTANCE);
        r.register(JInternalFrame.class, JInternalFrameHandler.INSTANCE);

        // Generic container handler covers JPanel, JScrollPane, JToolBar, JViewport,
        // JLayeredPane (and its subclass JDesktopPane), and any other JComponent that
        // is primarily a container.
        // JLayeredPane overrides paint() for layer-ordering, which would trigger
        // RasterFallback; registering it explicitly ensures child recursion instead.
        r.register(JLayeredPane.class,  ContainerHandler.INSTANCE);
        r.register(JPanel.class,        ContainerHandler.INSTANCE);
        // JScrollPane: in AcroForm mode, when the viewport's view is a JTextComponent,
        // skip the Swing scrollbar children  -- the PDF annotation provides its own scroll UI.
        // In DATA_REPORT mode, JScrollPane views are already expanded to their preferred
        // size by expandScrollPanes() before rendering begins.
        // In all other cases fall through to normal container rendering.
        r.register(JScrollPane.class, (comp, absX, absY, ctx) -> {
            if (!(comp instanceof JScrollPane sp)) return;
            JViewport vp = sp.getViewport();
            boolean acroTextField = ctx.acroFormEmitter() != null
                    && vp != null && vp.getView() instanceof JTextComponent;
            if (acroTextField) {
                // Draw background + border only, then recurse into the viewport.
                // The viewport's JTextComponent handler emits the AcroForm annotation.
                if (sp.isOpaque() && sp.getBackground() != null)
                    ctx.writer().fillRect(absX, absY, sp.getWidth(), sp.getHeight(), sp.getBackground());
                ContainerHandler.renderBorderOnly(sp, absX, absY, ctx);
                ctx.traverseAt(vp, absX + vp.getX(), absY + vp.getY());
            } else {
                ContainerHandler.INSTANCE.render(comp, absX, absY, ctx);
            }
        });
        r.register(JToolBar.class,      ContainerHandler.INSTANCE);
        r.register(JViewport.class,     ContainerHandler.INSTANCE);

        // User-supplied vector handlers override built-in handlers for the same type
        for (var entry : config.vectorHandlers().entrySet()) {
            @SuppressWarnings("unchecked")
            Class<? extends java.awt.Component> type =
                    (Class<? extends java.awt.Component>) entry.getKey();
            r.register(type, new VectorHandlerAdapter(entry.getValue()));
        }

        return r;
    }
}
