/**
 * swingtopdf  -- Java Swing to true vector PDF converter.
 *
 * <p>Exports any {@link javax.swing.JComponent} tree to a searchable, multi-page PDF
 * with selectable text, embedded fonts, and de-duplicated images. Powered by
 * <a href="https://pdfbox.apache.org/">Apache PDFBox 3.x</a>.</p>
 *
 * <h2>Exported packages</h2>
 * <ul>
 *   <li>{@code com.seaglassfoundry.swingtopdf}  -- the {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter} entry point</li>
 *   <li>{@code com.seaglassfoundry.swingtopdf.api}  -- configuration types, hook interfaces, and exceptions</li>
 * </ul>
 *
 * <h2>Required module opens</h2>
 * <p>For optimal font resolution, the launching module or JVM command line should provide:</p>
 * <pre>
 * --add-opens java.desktop/javax.swing=com.seaglassfoundry.swingtopdf
 * --add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
 * --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
 * </pre>
 * <p>Without these opens, the library falls back to OS directory scanning and standard
 * PDF Type 1 fonts (Helvetica, Times, Courier).</p>
 *
 * @see com.seaglassfoundry.swingtopdf.SwingPdfExporter
 */
module com.seaglassfoundry.swingtopdf {

    // Public API  -- exported to all consumers
    exports com.seaglassfoundry.swingtopdf;
    exports com.seaglassfoundry.swingtopdf.api;

    // com.seaglassfoundry.swingtopdf.internal is intentionally NOT exported.
    // All internal types are package-private or module-scoped.

    requires transitive java.desktop;   // javax.swing, java.awt, sun.font (via --add-opens)
    requires transitive org.apache.pdfbox; // PDFBox 3.x PDF document model and content streams
    requires org.slf4j;                 // Logging (slf4j-api)
}
