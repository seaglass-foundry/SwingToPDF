package com.seaglassfoundry.swingtopdf.api;

/**
 * Controls whether the export renders the full data content of each
 * component or only what is currently visible on screen.
 *
 * @see com.seaglassfoundry.swingtopdf.SwingPdfExporter#exportMode(ExportMode)
 *
 * @since 1.0.0
 */
public enum ExportMode {
    /**
     * Render all data regardless of scroll position or selected tab.
     * JTable shows all rows, JTabbedPane renders every tab on its own page,
     * JScrollPane shows the full preferred-size content.
     * PDF bookmarks are auto-generated for JTabbedPane tabs.
     * This is the recommended mode for reporting and data export.
     *
     * @since 1.0.0
     */
    DATA_REPORT,

    /**
     * Render only what is currently visible on screen -- equivalent to a
     * high-quality screenshot. Scroll position, viewport clip, and selected
     * tab are preserved as-is.
     *
     * @since 1.0.0
     */
    UI_SNAPSHOT
}
