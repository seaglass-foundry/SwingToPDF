package com.seaglassfoundry.swingtopdf.api;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * Hook interface for rendering a Swing component as vector PDF content
 * instead of rasterizing it.
 *
 * <p>When registered via
 * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#registerHandler
 * SwingPdfExporter.registerHandler(Class, VectorComponentHandler)}, the handler
 * receives a {@link Graphics2D} backed by the PDF content stream. All drawing
 * operations (shapes, text, images) performed on this {@code Graphics2D} are
 * emitted as vector PDF primitives — text remains selectable and shapes are
 * resolution-independent.
 *
 * <p>This is particularly useful for components that perform custom painting,
 * such as chart libraries (e.g. JFreeChart), diagram renderers, or any
 * {@code JComponent} subclass that overrides {@code paintComponent}. Without a
 * registered handler these components are rasterized to a bitmap.
 *
 * <h2>Example — JFreeChart</h2>
 * <pre>{@code
 * SwingPdfExporter.from(chartPanel)
 *     .registerHandler(ChartPanel.class, (comp, g2, bounds) -> {
 *         ((ChartPanel) comp).getChart().draw(g2, bounds);
 *     })
 *     .pageSize(PageSize.A4)
 *     .export(file);
 * }</pre>
 *
 * <h2>Coordinate system</h2>
 * The {@code bounds} rectangle always starts at {@code (0, 0)} and has the
 * component's width and height. The {@code Graphics2D} is pre-configured so
 * that drawing within {@code bounds} maps to the component's position on the
 * PDF page.
 *
 * <h2>Children are not traversed</h2>
 * When a vector handler is registered for a component type, that component is
 * treated as a leaf node during rendering. Its child components are
 * <em>not</em> automatically rendered — the handler is responsible for drawing
 * everything within the component's bounds.
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface VectorComponentHandler {

    /**
     * Render {@code component} by drawing into {@code g2}.
     *
     * @param component the Swing component to render
     * @param g2        a PDF-backed {@link Graphics2D} — draw into this
     * @param bounds    the component's bounds as {@code (0, 0, width, height)}
     * @throws IOException if writing to the PDF stream fails
     */
    void render(Component component, Graphics2D g2, Rectangle2D bounds) throws IOException;
}
