package com.seaglassfoundry.swingtopdf.api;

/**
 * Controls how a component tree that is larger than the page is handled.
 *
 * <ul>
 *   <li>{@link #SINGLE_PAGE}  -- scale the component uniformly to fit on one page.</li>
 *   <li>{@link #AUTO}         -- paginate across multiple pages; no scaling applied
 *       (this is the default behaviour and is used by both export modes).</li>
 *   <li>{@link #NONE}         -- render at natural size; content that overflows the
 *       page is clipped at the printable boundary.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum PaginationMode {
    /**
     * Scale the entire component tree uniformly to fit on a single page.
     * Useful for dashboards or forms that should never span multiple pages.
     *
     * @since 1.0.0
     */
    SINGLE_PAGE,
    /**
     * Automatically paginate content across multiple pages at natural scale.
     * Page breaks are snapped to table row boundaries and
     * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#KEEP_TOGETHER KEEP_TOGETHER} markers
     * are respected to avoid splitting atomic widgets.
     *
     * @since 1.0.0
     */
    AUTO,
    /**
     * Render at natural size with no pagination. Content that extends beyond
     * the printable area is clipped. Suitable for single-page exports where
     * the component is known to fit.
     *
     * @since 1.0.0
     */
    NONE
}
