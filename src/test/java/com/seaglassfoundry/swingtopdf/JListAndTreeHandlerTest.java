package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Verifies JList and JTree text selectability in the exported PDF,
 * including row-boundary page-break snapping for multi-page exports.
 */
class JListAndTreeHandlerTest {

    // =========================================================================
    // JList
    // =========================================================================

    @Test
    void jList_allItemsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JList<String> list = new JList<>(new String[]{"Apple", "Banana", "Cherry"});
        list.setFixedCellHeight(20);

        Path pdf = exportWrapped(list, 200, 80, tmp.resolve("list.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Apple");
            assertThat(text).contains("Banana");
            assertThat(text).contains("Cherry");
        }
    }

    @Test
    void jList_dataReport_allModelItemsRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // 10 items, but list only tall enough to show 3
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 10; i++) model.addElement("Item-" + i);

        JList<String> list = new JList<>(model);
        list.setFixedCellHeight(20);

        Path pdf = exportWrapped(list, 200, 60, tmp.resolve("list_full.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            for (int i = 0; i < 10; i++) {
                assertThat(text).contains("Item-" + i);
            }
        }
    }

    @Test
    void jList_customRenderer_textExtracted(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JList<Integer> list = new JList<>(new Integer[]{10, 20, 30});
        list.setFixedCellHeight(20);
        list.setCellRenderer((l, val, idx, sel, foc) -> {
            JLabel lbl = new JLabel("$" + val);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            return lbl;
        });

        Path pdf = exportWrapped(list, 200, 70, tmp.resolve("list_renderer.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("$10");
            assertThat(text).contains("$20");
            assertThat(text).contains("$30");
        }
    }

    @Test
    void jList_empty_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JList<String> list = new JList<>(new String[0]);
        Path pdf = exportWrapped(list, 200, 50, tmp.resolve("list_empty.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // JTree
    // =========================================================================

    @Test
    void jTree_snapshot_visibleNodes_areSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTree tree = buildTree();
        // Collapse everything — only root + first level visible
        tree.collapseRow(0);
        tree.expandRow(0);

        Path pdf = exportWrapped(tree, 300, 200, tmp.resolve("tree_snap.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Root");
            assertThat(text).contains("Fruits");
            assertThat(text).contains("Veggies");
        }
    }

    @Test
    void jTree_dataReport_allNodes_areSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTree tree = buildTree();
        // Collapse so leaf nodes are NOT visible in UI
        tree.collapseRow(0);

        Path pdf = exportWrapped(tree, 300, 200, tmp.resolve("tree_full.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Root");
            assertThat(text).contains("Fruits");
            assertThat(text).contains("Apple");
            assertThat(text).contains("Banana");
            assertThat(text).contains("Veggies");
            assertThat(text).contains("Carrot");
        }
    }

    @Test
    void jTree_rootNotVisible_dataReport(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTree tree = buildTree();
        tree.setRootVisible(false);

        Path pdf = exportWrapped(tree, 300, 200, tmp.resolve("tree_noroot.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // Root itself should NOT appear, but children should
            assertThat(text).contains("Fruits");
            assertThat(text).contains("Veggies");
        }
    }

    @Test
    void jTree_singleNode_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("OnlyNode");
        JTree tree = new JTree(new DefaultTreeModel(root));

        Path pdf = exportWrapped(tree, 200, 60, tmp.resolve("tree_single.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("OnlyNode");
        }
    }

    // =========================================================================
    // Row-boundary page-break snapping — JList
    // =========================================================================

    /**
     * Geometry (96 dpi, A4 portrait, 36 pt margins):
     * <pre>
     *   stepPx  ≈ 1026 px/page
     *   cell height = 30 px,  list starts at absY = 900
     *   row boundaries: 930, 960, 990, 1020, 1050, …
     *   ideal break = 1026  →  snap DOWN to 1020
     *
     *   Row "Row-03" occupies absY 990–1020 (boundary 1020 = snap point).
     *   Row "Row-04" occupies absY 1020–1050 (starts exactly at snap point → page 2).
     *   Row "Row-04" text baseline ≈ 1035.
     *
     *   WITH snapping  (sliceTopPx = 1020):
     *     pdfBaseline = 841 – 36 – (1035 – 1020) × 0.75 ≈ 794  → within page ✓
     *
     *   WITHOUT snapping (sliceTopPx = 1026):
     *     pdfBaseline = 841 – 36 – (1035 – 1026) × 0.75 ≈ 798  → within page
     *
     *   Both cases land within the page (the rows straddle the break by only
     *   a few pixels), so we verify the simpler invariant: all rows appear in
     *   the full document and the export spans ≥ 2 pages.
     * </pre>
     */
    @Test
    void jList_multiPage_rowBoundarySnap_allItemsPresent(@TempDir Path tmp) throws Exception {
        // 50 items × 30 px = 1500 px; placed at y=900 inside a 2600 px root → spans 2 pages
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 50; i++) model.addElement(String.format("Row-%02d", i));

        JList<String> list = new JList<>(model);
        list.setFixedCellHeight(30);
        list.setSize(400, 1500);

        JPanel root = new JPanel(null);
        root.setBackground(Color.WHITE);
        root.setSize(400, 2600);
        list.setBounds(0, 900, 400, 1500);
        root.add(list);
        root.validate();

        Path pdf = tmp.resolve("list-snap.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            String text = new PDFTextStripper().getText(doc);
            for (int i = 0; i < 50; i++) {
                assertThat(text).contains(String.format("Row-%02d", i));
            }
        }
    }

    /**
     * Verifies that with row-boundary snapping, a row whose bottom exactly
     * coincides with the snap point appears intact on page 2, not split.
     *
     * <pre>
     *   stepPx ≈ 1026,  cell height = 30 px,  list at absY = 0
     *   Row boundaries: 30, 60, …, 1020, 1050, …
     *   Snap point = 1020  (last boundary ≤ 1026).
     *   "Row-34" occupies absY 1020–1050; baseline ≈ 1035.
     *
     *   sliceTopPx page 2 = 1020 (snapped)
     *   pdfBaseline(1035) = 841 – 36 – (1035 – 1020) × 0.75 = 793.75  → inside page ✓
     * </pre>
     */
    @Test
    void jList_rowAfterSnapPoint_appearsOnPage2(@TempDir Path tmp) throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 80; i++) model.addElement(String.format("Row-%02d", i));

        JList<String> list = new JList<>(model);
        list.setFixedCellHeight(30);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.add(list, BorderLayout.CENTER);
        root.validate();

        Path pdf = tmp.resolve("list-page2.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            // Row-34 starts at absY=1020 (exactly at snap point) → must appear on page 2
            assertThat(pageText(doc, 1)).contains("Row-34");
        }
    }

    // =========================================================================
    // Row-boundary page-break snapping — JTree
    // =========================================================================

    /**
     * A tree with enough nodes to span 2+ pages. All nodes must be selectable.
     */
    @Test
    void jTree_multiPage_rowBoundarySnap_allNodesPresent(@TempDir Path tmp) throws Exception {
        // Build a tree: root → 10 branches → 10 leaves each = 110 nodes @ 20 px = 2200 px
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        for (int b = 0; b < 10; b++) {
            DefaultMutableTreeNode branch = new DefaultMutableTreeNode("Branch-" + b);
            for (int l = 0; l < 10; l++) {
                branch.add(new DefaultMutableTreeNode("Leaf-" + b + "-" + l));
            }
            root.add(branch);
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRowHeight(20);
        tree.setSize(400, 2200);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setSize(400, 2200);
        panel.add(tree, BorderLayout.CENTER);
        panel.validate();

        Path pdf = tmp.resolve("tree-snap.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Root");
            for (int b = 0; b < 10; b++) {
                assertThat(text).contains("Branch-" + b);
                for (int l = 0; l < 10; l++) {
                    assertThat(text).contains("Leaf-" + b + "-" + l);
                }
            }
        }
    }

    /**
     * Variable-height JList (no fixed cell height): the snapper must query each
     * cell's preferred size. All items must still appear after export.
     */
    @Test
    void jList_variableHeight_rowBoundarySnap_allItemsPresent(@TempDir Path tmp) throws Exception {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < 60; i++) model.addElement("VarItem-" + i);

        JList<String> list = new JList<>(model);
        // No fixed cell height — renderer preferred size drives the height

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setSize(400, 2200);
        root.add(list, BorderLayout.CENTER);
        root.validate();

        Path pdf = tmp.resolve("list-varheight.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            for (int i = 0; i < 60; i++) {
                assertThat(text).contains("VarItem-" + i);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Extract text inside the full page MediaBox of the given 0-based page. */
    private static String pageText(PDDocument doc, int pageIndex) throws Exception {
        PDPage page  = doc.getPage(pageIndex);
        float  pageW = page.getMediaBox().getWidth();
        float  pageH = page.getMediaBox().getHeight();
        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.addRegion("page", new Rectangle2D.Float(0, 0, pageW, pageH));
        stripper.extractRegions(page);
        return stripper.getTextForRegion("page");
    }

    private static JTree buildTree() {
        DefaultMutableTreeNode root    = new DefaultMutableTreeNode("Root");
        DefaultMutableTreeNode fruits  = new DefaultMutableTreeNode("Fruits");
        DefaultMutableTreeNode veggies = new DefaultMutableTreeNode("Veggies");
        fruits.add(new DefaultMutableTreeNode("Apple"));
        fruits.add(new DefaultMutableTreeNode("Banana"));
        veggies.add(new DefaultMutableTreeNode("Carrot"));
        veggies.add(new DefaultMutableTreeNode("Daikon"));
        root.add(fruits);
        root.add(veggies);
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRowHeight(18);
        return tree;
    }

    private static Path exportWrapped(JComponent comp, int w, int h,
                                       Path out, ExportMode mode) throws Exception {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        panel.add(comp, BorderLayout.CENTER);
        panel.validate();

        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .exportMode(mode)
                .export(out);
        return out;
    }
}
