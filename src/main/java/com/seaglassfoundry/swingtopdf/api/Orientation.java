package com.seaglassfoundry.swingtopdf.api;

/**
 * Page orientation for the exported PDF.
 *
 * <p>When {@link #LANDSCAPE} is selected, the width and height of the chosen
 * {@link PageSize} are swapped automatically by the export engine. Margins
 * retain their semantic meaning (top is still the top of the rotated page).</p>
 *
 * @see com.seaglassfoundry.swingtopdf.SwingPdfExporter#orientation(Orientation)
 *
 * @since 1.0.0
 */
public enum Orientation {
    /**
     * Standard upright orientation (height &gt; width for most standard sizes).
     *
     * @since 1.0.0
     */
    PORTRAIT,
    /**
     * Rotated orientation (width &gt; height). Page size dimensions are swapped.
     *
     * @since 1.0.0
     */
    LANDSCAPE
}
