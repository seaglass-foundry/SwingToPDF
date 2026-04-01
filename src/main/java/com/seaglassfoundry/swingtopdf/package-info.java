/**
 * Root package of the swingtopdf library  -- a Java Swing to true vector PDF converter.
 *
 * <h2>Overview</h2>
 * <p>swingtopdf exports any {@link javax.swing.JComponent} tree to a multi-page, searchable
 * vector PDF. Unlike screenshot-based approaches, text remains selectable, fonts are
 * embedded (or substituted with standard PDF Type 1 fonts), and images are de-duplicated
 * across pages. The library uses <a href="https://pdfbox.apache.org/">Apache PDFBox 3.x</a>
 * as its PDF engine.</p>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
 * import com.seaglassfoundry.swingtopdf.api.PageSize;
 * import com.seaglassfoundry.swingtopdf.api.Orientation;
 *
 * SwingPdfExporter.from(myPanel)
 *     .pageSize(PageSize.A4)
 *     .orientation(Orientation.PORTRAIT)
 *     .margins(36, 36, 36, 36)
 *     .title("Quarterly Report")
 *     .footer(HeaderFooter.of("Page {page} of {pages}"))
 *     .export(Path.of("report.pdf"));
 * }</pre>
 *
 * <h2>Export modes</h2>
 * <ul>
 *   <li><b>{@link com.seaglassfoundry.swingtopdf.api.ExportMode#DATA_REPORT DATA_REPORT}</b> (default)  --
 *       renders all data regardless of scroll position or selected tab. JTable shows every
 *       model row, JTabbedPane renders every tab on its own section, and JScrollPane
 *       expands to its full preferred-size content. Auto-paginates across multiple PDF pages.</li>
 *   <li><b>{@link com.seaglassfoundry.swingtopdf.api.ExportMode#UI_SNAPSHOT UI_SNAPSHOT}</b>  --
 *       renders only what is currently visible on screen, equivalent to a high-quality
 *       screenshot with selectable text.</li>
 * </ul>
 *
 * <h2>AcroForm support</h2>
 * <p>Call {@link SwingPdfExporter#enableAcroForm()} to generate interactive PDF form fields
 * for {@code JTextField}, {@code JTextArea}, {@code JPasswordField}, {@code JCheckBox},
 * {@code JRadioButton}, and {@code JComboBox}. The resulting PDF can be filled in by end
 * users in any standard PDF viewer (Adobe Acrobat, Chrome, Preview, etc.).</p>
 *
 * <h2>Extensibility hooks</h2>
 * <ul>
 *   <li>{@link com.seaglassfoundry.swingtopdf.api.FontResolver}  -- custom font file resolution for
 *       fonts the library cannot locate automatically.</li>
 *   <li>{@link com.seaglassfoundry.swingtopdf.api.ImageHandler}  -- custom image encoding (e.g. JPEG
 *       compression instead of the default lossless PNG).</li>
 *   <li>{@link com.seaglassfoundry.swingtopdf.api.VectorComponentHandler}  -- custom vector rendering for
 *       components that perform custom painting (e.g. chart libraries). The handler receives a PDF-backed
 *       {@link java.awt.Graphics2D} so all drawing operations produce vector PDF output instead of
 *       rasterised bitmaps.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>{@link SwingPdfExporter} instances are not thread-safe. Each export creates its own
 * PDFBox {@code PDDocument} and font/image caches, so concurrent exports from different
 * builder instances are safe. The rendering pipeline marshals Swing property reads to the
 * EDT automatically via {@code SwingUtilities.invokeAndWait}.</p>
 *
 * <h2>JVM flags</h2>
 * <p>The library requires the following module opens for font file resolution via JVM
 * internals. Without them, fonts are resolved via OS directory scanning (slower) or fall
 * back to standard PDF Type 1 fonts:</p>
 * <pre>
 * --add-opens java.desktop/javax.swing=com.seaglassfoundry.swingtopdf
 * --add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
 * --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
 * </pre>
 *
 * @see SwingPdfExporter
 * @see com.seaglassfoundry.swingtopdf.api
 */
package com.seaglassfoundry.swingtopdf;
