package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;

import javax.swing.JLayeredPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively walks a Swing component tree, dispatching each component to the
 * appropriate {@link ComponentHandler} via the {@link HandlerRegistry}.
 *
 * <p>Components without a registered handler are rendered by {@link RasterFallback}.
 * Invisible components (where {@code isVisible()} is false) are skipped.
 */
final class ComponentTraverser {

    private static final Logger log = LoggerFactory.getLogger(ComponentTraverser.class);

    private final HandlerRegistry registry;
    private final HandlerContext   ctx;

    ComponentTraverser(HandlerRegistry registry, HandlerContext ctx) {
        this.registry = registry;
        this.ctx      = ctx;
    }

    /**
     * Traverse {@code comp} and all its descendants.
     *
     * @param comp   the component to render
     * @param parentAbsX absolute X of the parent's top-left corner in root space
     * @param parentAbsY absolute Y of the parent's top-left corner in root space
     */
    void traverse(Component comp, int parentAbsX, int parentAbsY) throws IOException {
        if (!comp.isVisible()) return;
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) {
            if (comp instanceof Container c && c.getComponentCount() > 0) {
                log.warn("Skipping zero-size container with {} children: {} — call setSize() or pack() before exporting",
                         c.getComponentCount(), comp.getClass().getName());
            }
            return;
        }
        // Scrollbars that belong to a JScrollPane are non-functional in a static PDF  -- skip them.
        // L&F title panes (e.g. MetalInternalFrameTitlePane) are already covered by
        // JInternalFrameHandler's vector title bar  -- skip to avoid a needless rasterization warning.
        if ((comp instanceof JScrollBar && comp.getParent() instanceof JScrollPane) || comp.getClass().getName().contains("InternalFrameTitlePane")) return;

        int absX = parentAbsX + comp.getX();
        int absY = parentAbsY + comp.getY();

        ComponentHandler handler = registry.lookup(comp.getClass());
        if (handler != null) {
            handler.render(comp, absX, absY, ctx);
        } else if (comp instanceof Container container && !hasCustomPaint(comp)) {
            // Plain container with no handler and no custom painting  -- just recurse
            traverseChildren(container, absX, absY);
        } else {
            RasterFallback.render(comp, absX, absY, ctx);
        }
    }

    /**
     * Iterate all direct children of {@code parent} and traverse each.
     * Called by container handlers after they have drawn their own background/border.
     *
     * <p>For {@link JLayeredPane} (and its subclass {@code JDesktopPane}), children
     * are iterated in reverse index order so that the topmost component (index 0)
     * is rendered last in the PDF  -- matching Swing's back-to-front painting order.
     */
    void traverseChildren(Container parent, int absX, int absY) throws IOException {
        if (parent instanceof JLayeredPane) {
            // JLayeredPane: index 0 = front (highest z-order, painted last on screen)
            // Render back-to-front so the front-most frame is drawn last in the PDF.
            for (int i = parent.getComponentCount() - 1; i >= 0; i--) {
                traverse(parent.getComponent(i), absX, absY);
            }
        } else {
            for (int i = 0; i < parent.getComponentCount(); i++) {
                traverse(parent.getComponent(i), absX, absY);
            }
        }
    }

    /**
     * Render {@code comp} at the explicit absolute position ({@code absX}, {@code absY})
     * without adding the component's own {@code getX()}/{@code getY()} offset.
     *
     * <p>Unlike {@link #traverse}, this method also ignores {@code isVisible()} so that
     * logically hidden components (e.g. non-selected {@code JTabbedPane} panels) can be
     * rendered in DATA_REPORT mode.
     */
    void traverseAt(Component comp, int absX, int absY) throws IOException {
        if (comp.getWidth() <= 0 || comp.getHeight() <= 0) return;

        ComponentHandler handler = registry.lookup(comp.getClass());
        if (handler != null) {
            handler.render(comp, absX, absY, ctx);
        } else if (comp instanceof Container container && !hasCustomPaint(comp)) {
            traverseChildren(container, absX, absY);
        } else {
            RasterFallback.render(comp, absX, absY, ctx);
        }
    }

    /**
     * Heuristic: does this component class override {@code paintComponent} or
     * {@code paint}? If so, it likely has custom drawing that we cannot
     * replicate by just recursing into children  -- rasterize instead.
     *
     * <p>Standard Swing containers (JPanel, JScrollPane, etc.) do not override
     * {@code paint} in a meaningful way, so this only catches user-defined
     * anonymous/named subclasses.
     */
    private static boolean hasCustomPaint(Component comp) {
        Class<?> cls = comp.getClass();
        // Anonymous or non-standard subclass of a known container type
        try {
            cls.getDeclaredMethod("paintComponent", java.awt.Graphics.class);
            return true; // overrides paintComponent  -- treat as custom
        } catch (NoSuchMethodException ignored) {}
        try {
            cls.getDeclaredMethod("paint", java.awt.Graphics.class);
            return true;
        } catch (NoSuchMethodException ignored) {}
        return false;
    }
}
