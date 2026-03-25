package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.seaglassfoundry.swingtopdf.api.ExportMode;

/**
 * Renders a {@link JTree} as PDF vector graphics.
 *
 * <h3>UI_SNAPSHOT mode</h3>
 * Renders only the currently visible (expanded) rows using
 * {@link JTree#getRowBounds(int)}, which includes the correct indentation
 * for each row's depth.  Expansion / collapse arrows are not drawn.
 *
 * <h3>DATA_REPORT mode</h3>
 * Recursively walks the entire {@link TreeModel} regardless of expansion
 * state, rendering every node.  Indentation is computed from the node depth
 * using the tree's row height as a guide.
 */
final class JTreeHandler implements ComponentHandler {

    static final JTreeHandler INSTANCE = new JTreeHandler();

    private JTreeHandler() {}

    /** Horizontal indent per depth level in DATA_REPORT mode (pixels). */
    private static final int INDENT_PX = 16;

    /** Fallback row height (px) when UIManager has no value. */
    private static final int DEFAULT_ROW_HEIGHT = 18;

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JTree tree = (JTree) comp;

        // Background
        if (tree.isOpaque() && tree.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, tree.getWidth(), tree.getHeight(),
                                  tree.getBackground());
        }

        if (ctx.config().exportMode() == ExportMode.DATA_REPORT) {
            renderAllNodes(tree, absX, absY, ctx);
        } else {
            renderVisibleRows(tree, absX, absY, ctx);
        }
    }

    // -----------------------------------------------------------------------
    // UI_SNAPSHOT  -- visible rows only
    // -----------------------------------------------------------------------

    private void renderVisibleRows(JTree tree, int absX, int absY,
                                    HandlerContext ctx) throws IOException {
        for (int row = 0; row < tree.getRowCount(); row++) {
            Rectangle bounds = tree.getRowBounds(row);
            if (bounds == null) continue;

            TreePath path     = tree.getPathForRow(row);
            Object   node     = path.getLastPathComponent();
            boolean  isLeaf   = tree.getModel().isLeaf(node);
            boolean  expanded = tree.isExpanded(path);

            Component cell = tree.getCellRenderer().getTreeCellRendererComponent(
                    tree, node, false, expanded, isLeaf, row, false);

            int rowX = absX + bounds.x;
            int rowY = absY + bounds.y;

            renderCell(cell, node, rowX, rowY, bounds.width, bounds.height, tree, ctx);
        }
    }

    // -----------------------------------------------------------------------
    // DATA_REPORT  -- all nodes recursively
    // -----------------------------------------------------------------------

    private void renderAllNodes(JTree tree, int absX, int absY,
                                 HandlerContext ctx) throws IOException {
        TreeModel model  = tree.getModel();
        Object    root   = model.getRoot();
        int       rowH   = tree.getRowHeight() > 0 ? tree.getRowHeight() : defaultRowHeight();
        int[]     curY   = { absY };   // mutable cursor shared across recursion

        if (!tree.isRootVisible()) {
            // Skip root, but still recurse into its children
            for (int i = 0; i < model.getChildCount(root); i++) {
                renderNode(tree, model, model.getChild(root, i), absX, curY, 0, rowH, ctx);
            }
        } else {
            renderNode(tree, model, root, absX, curY, 0, rowH, ctx);
        }
    }

    private void renderNode(JTree tree, TreeModel model, Object node,
                             int absX, int[] curY, int depth,
                             int rowH, HandlerContext ctx) throws IOException {
        // Stop once nodes are entirely below the current page slice
        if (curY[0] >= ctx.sliceBottomPx()) return;

        boolean isLeaf = model.isLeaf(node);
        int     nodeX  = absX + depth * INDENT_PX;

        // Only render cells that are on the current page slice
        if (curY[0] + rowH > ctx.sliceTopPx()) {
            TreeCellRenderer renderer = tree.getCellRenderer();
            Component cell = renderer.getTreeCellRendererComponent(
                    tree, node, false, true, isLeaf, 0, false);

            renderCell(cell, node, nodeX, curY[0], tree.getWidth() - depth * INDENT_PX,
                       rowH, tree, ctx);
        }
        curY[0] += rowH;

        if (!isLeaf) {
            int childCount = model.getChildCount(node);
            for (int i = 0; i < childCount; i++) {
                renderNode(tree, model, model.getChild(node, i),
                           absX, curY, depth + 1, rowH, ctx);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Cell rendering  -- shared by both modes
    // -----------------------------------------------------------------------

    private void renderCell(Component cell, Object node,
                             int cellX, int cellY, int cellW, int cellH,
                             JTree tree, HandlerContext ctx) throws IOException {
        if (cell instanceof JLabel label) {
            // Delegate fully to JLabelHandler  -- handles background, icon, and text
            label.setSize(cellW, cellH);
            JLabelHandler.INSTANCE.render(label, cellX, cellY, ctx);
            return;
        }

        // Non-JLabel renderer: fill background if distinct from tree background, draw text
        Color bg = cell.getBackground();
        if (bg == null) bg = tree.getBackground();
        if (bg != null && !bg.equals(tree.getBackground())) {
            ctx.writer().fillRect(cellX, cellY, cellW, cellH, bg);
        }

        String text = node.toString();
        if (text.isBlank()) return;

        Font  font = cell.getFont();
        Color fg   = cell.getForeground();
        if (font == null) font = tree.getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        if (fg   == null) fg   = tree.getForeground();
        if (fg   == null) fg   = Color.BLACK;

        FontMetrics fm        = cell.getFontMetrics(font);
        int         ascent    = fm.getAscent();
        int         textH     = ascent + fm.getDescent();
        int         baselineY = cellY + (cellH - textH) / 2 + ascent;

        ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                              font.getSize2D(), fg, cellX, baselineY);
    }

    private static int defaultRowHeight() {
        int h = UIManager.getInt("Tree.rowHeight");
        return h > 0 ? h : DEFAULT_ROW_HEIGHT;
    }
}
