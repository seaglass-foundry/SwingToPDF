package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seaglassfoundry.swingtopdf.api.LayoutException;

/**
 * Ensures that a component tree has a valid layout before rendering begins.
 *
 * <p>In snapshot mode (component already shown on screen) this is usually a
 * no-op. In headless mode, it triggers {@code validate()} and, if the
 * component still has zero size, applies the preferred size.
 *
 * <p>All operations are run on the EDT via {@link EdtHelper}.
 */
public final class LayoutEnsurer {

    private static final Logger log = LoggerFactory.getLogger(LayoutEnsurer.class);

    private LayoutEnsurer() {}

    /**
     * Ensure {@code root} has a non-zero size and a valid layout.
     *
     * @throws LayoutException if layout cannot be computed
     */
    public static void ensureLayout(JComponent root) {
        ensureLayout(root, 0);
    }

    /**
     * Ensure {@code root} has a non-zero size and a valid layout.
     *
     * @param minWidthPx minimum width in Swing pixels (e.g. printable page width);
     *                   used only when the component has no explicit size and falls
     *                   back to preferred size. Pass 0 to use preferred size as-is.
     * @throws LayoutException if layout cannot be computed
     */
    public static void ensureLayout(JComponent root, int minWidthPx) {
        EdtHelper.runOnEdt(() -> doEnsureLayout(root, minWidthPx));
    }

    // Called on EDT
    private static void doEnsureLayout(JComponent root, int minWidthPx) {
        // If the component has no size, try to use preferred size
        if (root.getWidth() == 0 || root.getHeight() == 0) {
            Dimension pref = root.getPreferredSize();
            if (pref.width <= 0 && pref.height <= 0 && minWidthPx <= 0) {
                throw new LayoutException(
                        "Component has no size and no preferred size. " +
                        "Call setSize() or pack() before exporting: " +
                        root.getClass().getName());
            }
            int w = Math.max(Math.max(pref.width, 1), minWidthPx);
            int h = Math.max(pref.height, 1);
            log.debug("Component has zero size; applying size {}x{} (minWidthPx={})", w, h, minWidthPx);
            root.setSize(w, h);
        }

        // If the component tree has no native peer, validate() is a no-op
        // (Container.validate checks peer != null internally).  Calling
        // addNotify() creates a lightweight peer and, crucially, triggers
        // Swing-level initialization hooks such as
        // JTable.configureEnclosingScrollPane().  Lightweight peers are
        // harmless  -- they are essentially no-op wrappers  -- and the
        // component can still be added to a real window afterwards.
        if (!root.isDisplayable()) {
            log.debug("Component not displayable; calling addNotify() to initialise component tree");
            try {
                root.addNotify();
            } catch (HeadlessException e) {
                // Headless environments may reject peer creation; fall through
                // to the forceLayout() fallback below.
                log.warn("addNotify() failed ({}); layout may be incomplete", e.getMessage());
            }
        }

        // Force layout traversal if the component tree is invalid
        if (!root.isValid()) {
            log.debug("Component is invalid; running validate()");
            root.validate();
        }

        // If still invalid (e.g. peer creation failed in headless), walk manually
        if (!root.isValid()) {
            log.debug("Component still invalid after validate(); forcing doLayout traversal");
            forceLayout(root);
        }

        log.debug("Layout ensured: {}x{}", root.getWidth(), root.getHeight());
    }

    private static void forceLayout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container c) {
                forceLayout(c);
            }
        }
    }
}
