/**
 * Internal implementation of the swingtopdf rendering pipeline.
 *
 * <p><b>This package is intentionally not exported from the {@code com.seaglassfoundry.swingtopdf} module.</b>
 * All types here are package-private or module-internal. External code should interact
 * exclusively through {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter} and the types in
 * {@link com.seaglassfoundry.swingtopdf.api}. Internal APIs may change without notice between releases.
 *
 * <h2>Architecture overview</h2>
 *
 * <h3>Export pipeline</h3>
 * <p>A single export proceeds through these stages:</p>
 * <ol>
 *   <li><b>Configuration</b>  -- {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter} builds an immutable
 *       {@link ExportConfig} from the fluent builder settings.</li>
 *   <li><b>Layout</b>  -- {@link LayoutEnsurer} ensures the component tree has a valid size
 *       and layout, calling {@code addNotify()}, {@code validate()}, and {@code doLayout()}
 *       as needed.</li>
 *   <li><b>Scroll expansion</b>  -- In {@code DATA_REPORT} mode, {@link ExportEngine}
 *       temporarily expands all {@code JScrollPane} views to their full preferred size
 *       so the complete content is measured and rendered.</li>
 *   <li><b>Pagination</b>  -- {@link ExportEngine} computes page break positions, snapping
 *       breaks to table row boundaries and respecting
 *       {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#KEEP_TOGETHER KEEP_TOGETHER} markers.</li>
 *   <li><b>Rendering</b>  -- For each page, a {@link ComponentTraverser} walks the component
 *       tree depth-first. Each component is dispatched to a registered
 *       {@link ComponentHandler} via the {@link HandlerRegistry}. Handlers read component
 *       state (text, font, color, model data) and write PDF primitives through
 *       {@link PdfPageWriter}.</li>
 *   <li><b>Font embedding</b>  -- {@link DefaultFontMapper} resolves AWT {@code Font} objects
 *       to PDFBox {@code PDFont} instances, trying JVM-internal resolution
 *       ({@link AwtFontFileResolver}), OS directory scanning ({@link SystemFontFinder}),
 *       and standard Type 1 fallback, in that order.</li>
 *   <li><b>Image encoding</b>  -- {@link DeduplicatingImageEncoder} ensures each
 *       {@code BufferedImage} instance is encoded only once per export, regardless of how
 *       many components reference it.</li>
 *   <li><b>AcroForm</b>  -- When enabled, {@link AcroFormEmitter} adds interactive widget
 *       annotations (text fields, checkboxes, radio buttons, combo boxes) during the
 *       per-page traversal.</li>
 * </ol>
 *
 * <h3>Handler dispatch</h3>
 * <p>{@link HandlerRegistry} maps component classes to {@link ComponentHandler} implementations.
 * Lookup walks the class hierarchy (superclasses only, not interfaces) until a registered handler
 * is found. Components without a handler are rendered by {@link RasterFallback}, which paints the
 * component to a {@code BufferedImage} using its own {@code paint()} method. Every rasterized
 * component is logged at {@code WARN} level so missing handlers can be identified.</p>
 *
 * <h3>Coordinate system</h3>
 * <p>All handler methods receive coordinates in <em>Swing root-space pixels</em>  -- absolute
 * positions measured from the root component's top-left corner. {@link PdfPageWriter} converts
 * these to PDF page coordinates (bottom-left origin, Y-up) internally, applying the scale factor,
 * margins, and per-page vertical slice offset.</p>
 *
 * <h3>EDT safety</h3>
 * <p>All Swing property reads are marshalled to the Event Dispatch Thread via
 * {@link EdtHelper}. If the calling thread is already the EDT, the action runs inline;
 * otherwise it is submitted via {@code SwingUtilities.invokeAndWait}.</p>
 *
 * <h3>Registered component handlers</h3>
 * <table>
 *   <caption>Component types and their handlers</caption>
 *   <tr><th>Component(s)</th><th>Handler</th><th>Notes</th></tr>
 *   <tr><td>{@code JLabel}</td><td>{@link JLabelHandler}</td>
 *       <td>Icon + text via {@code layoutCompoundLabel}; HTML text with inline styling</td></tr>
 *   <tr><td>{@code JTextField}, {@code JFormattedTextField}, {@code JTextArea},
 *       {@code JPasswordField}</td><td>{@link JTextComponentHandler}</td>
 *       <td>Single/multi-line text; word-wrap for JTextArea; echo char for password fields</td></tr>
 *   <tr><td>{@code JTextPane}</td><td>{@link JTextPaneHandler}</td>
 *       <td>StyledDocument walk with bold/italic/colour/underline/strikethrough/highlight</td></tr>
 *   <tr><td>{@code JEditorPane}</td><td>{@link JEditorPaneHandler}</td>
 *       <td>HTMLDocument walk with CSS-aware fonts/colours; non-HTML kits rasterized</td></tr>
 *   <tr><td>{@code JButton}, {@code JToggleButton}, {@code JCheckBox},
 *       {@code JRadioButton}</td><td>{@link AbstractButtonHandler}</td>
 *       <td>Icon + text; vector check/radio indicators</td></tr>
 *   <tr><td>{@code JTable}</td><td>{@link JTableHandler}</td>
 *       <td>All model rows in DATA_REPORT; cell renderers delegate to JLabelHandler</td></tr>
 *   <tr><td>{@code JTableHeader}</td><td>{@link JTableHeaderHandler}</td>
 *       <td>Repeats on continuation pages; delegates cells to JLabelHandler</td></tr>
 *   <tr><td>{@code JList}</td><td>{@link JListHandler}</td>
 *       <td>All model items in DATA_REPORT; JLabel renderers delegate to JLabelHandler</td></tr>
 *   <tr><td>{@code JTree}</td><td>{@link JTreeHandler}</td>
 *       <td>Full model walk in DATA_REPORT; indented with folder/leaf icons</td></tr>
 *   <tr><td>{@code JComboBox}</td><td>{@link JComboBoxHandler}</td>
 *       <td>Selected item only; framed box with arrow indicator</td></tr>
 *   <tr><td>{@code JTabbedPane}</td><td>{@link JTabbedPaneHandler}</td>
 *       <td>Tab bar with icons; DATA_REPORT stacks all panels vertically</td></tr>
 *   <tr><td>{@code JProgressBar}</td><td>{@link JProgressBarHandler}</td>
 *       <td>Filled rect + percentage or custom string label</td></tr>
 *   <tr><td>{@code JSlider}</td><td>{@link JSliderHandler}</td>
 *       <td>Track, thumb, optional tick marks and labels</td></tr>
 *   <tr><td>{@code JScrollBar}</td><td>{@link JScrollBarHandler}</td>
 *       <td>Track + proportional thumb + chevron end-cap buttons</td></tr>
 *   <tr><td>{@code JSpinner}</td><td>{@link JSpinnerHandler}</td>
 *       <td>Up/down chevron arrows + editor text via JTextComponentHandler</td></tr>
 *   <tr><td>{@code JSeparator}</td><td>{@link JSeparatorHandler}</td>
 *       <td>Single vector line at mid-height/mid-width</td></tr>
 *   <tr><td>{@code JSplitPane}</td><td>{@link JSplitPaneHandler}</td>
 *       <td>Vector divider bar with grab dots; recurses into left/right panels</td></tr>
 *   <tr><td>{@code JInternalFrame}</td><td>{@link JInternalFrameHandler}</td>
 *       <td>Vector title bar with icon + title + button outlines; recurses into content</td></tr>
 *   <tr><td>{@code JPanel}, {@code JScrollPane}, {@code JToolBar}, etc.</td>
 *       <td>{@link ContainerHandler}</td>
 *       <td>Background + border + child recursion</td></tr>
 * </table>
 */
package com.seaglassfoundry.swingtopdf.internal;
