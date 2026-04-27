package com.seaglassfoundry.swingtopdf;

import java.awt.Component;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComponent;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.FontResolver;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooterProvider;
import com.seaglassfoundry.swingtopdf.api.ImageHandler;
import com.seaglassfoundry.swingtopdf.api.LayoutException;
import com.seaglassfoundry.swingtopdf.api.Orientation;
import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.SwingPdfExportException;
import com.seaglassfoundry.swingtopdf.api.VectorComponentHandler;
import com.seaglassfoundry.swingtopdf.internal.ExportConfig;
import com.seaglassfoundry.swingtopdf.internal.ExportEngine;

/**
 * Fluent builder for exporting a Swing component tree to a vector PDF.
 *
 * <h2>Preventing page splits</h2>
 * Mark any {@link JComponent} with the {@link #KEEP_TOGETHER} client property
 * to prevent it from being split across a page boundary:
 * <pre>{@code
 * myPanel.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);
 * }</pre>
 * If the component is taller than a single page it will be split regardless.
 *
 * <h2>Basic usage</h2>
 * <pre>{@code
 * SwingPdfExporter.from(myPanel)
 *     .pageSize(PageSize.A4)
 *     .orientation(Orientation.PORTRAIT)
 *     .margins(36, 36, 36, 36)
 *     .export(Path.of("output.pdf"));
 * }</pre>
 *
 * <h2>Export modes</h2>
 * <ul>
 *   <li>{@link ExportMode#DATA_REPORT} (default)  -- renders all data regardless of
 *       scroll position or selected tab; auto-paginates; generates PDF bookmarks.</li>
 *   <li>{@link ExportMode#UI_SNAPSHOT}  -- renders only what is currently visible,
 *       equivalent to a high-quality screenshot.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class SwingPdfExporter {

    /**
     * {@link javax.swing.JComponent} client property key.
     * Set to {@link Boolean#TRUE} to prevent the component from being split
     * across a page boundary during pagination.
     *
     * <pre>{@code
     * myPanel.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);
     * }</pre>
     *
     * @since 1.0.0
     */
    public static final String KEEP_TOGETHER = "swing2pdf.keepTogether";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final JComponent root;

    private PageSize    pageSize    = PageSize.A4;
    private Orientation orientation = Orientation.PORTRAIT;
    /** Margins in points: [top, right, bottom, left] */
    private float[]     margins     = { 36f, 36f, 36f, 36f };
    private float       dpi         = 96f;
    private ExportMode  exportMode  = ExportMode.DATA_REPORT;

    private FontResolver fontResolver;
    private ImageHandler imageHandler;

    private String title;
    private String author;
    private String subject;
    private String keywords;

    private HeaderFooterProvider headerProvider;
    private HeaderFooterProvider footerProvider;

    private boolean acroFormEnabled = false;

    private final Map<Class<?>, VectorComponentHandler> vectorHandlers = new LinkedHashMap<>();

    // -----------------------------------------------------------------------
    // Constructor / factory
    // -----------------------------------------------------------------------

    private SwingPdfExporter(JComponent root) {
        this.root = Objects.requireNonNull(root, "root component must not be null");
    }

    /**
     * Begin building an export for {@code root}.
     *
     * @param root the component (or container) to export; must not be {@code null}
     * @return a new builder configured with defaults
     * @throws NullPointerException if {@code root} is {@code null}
     *
     * @since 1.0.0
     */
    public static SwingPdfExporter from(JComponent root) {
        return new SwingPdfExporter(root);
    }

    // -----------------------------------------------------------------------
    // Builder methods
    // -----------------------------------------------------------------------

    /**
     * Set the output page size.
     *
     * @param pageSize the page size to use; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code pageSize} is {@code null}
     * @see PageSize#A4
     */
    public SwingPdfExporter pageSize(PageSize pageSize) {
        this.pageSize = Objects.requireNonNull(pageSize);
        return this;
    }

    /**
     * Set the page orientation.
     *
     * @param orientation the orientation to use; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code orientation} is {@code null}
     * @see Orientation#PORTRAIT
     */
    public SwingPdfExporter orientation(Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation);
        return this;
    }

    /**
     * Set page margins in PDF points (1 pt = 1/72 inch). Values are specified in
     * CSS order: top, right, bottom, left. Default: 36 pt on all sides (0.5 in).
     *
     * @param top    top margin in points
     * @param right  right margin in points
     * @param bottom bottom margin in points
     * @param left   left margin in points
     * @return this builder
     * @throws IllegalArgumentException if any margin is negative
     */
    public SwingPdfExporter margins(float top, float right, float bottom, float left) {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("Margins must not be negative");
        }
        this.margins = new float[]{ top, right, bottom, left };
        return this;
    }

    /**
     * Set the DPI used for converting Swing pixel coordinates to PDF points.
     * The default of 96 matches the standard desktop screen resolution.
     *
     * @param dpi dots per inch; must be positive
     * @return this builder
     * @throws IllegalArgumentException if {@code dpi} is not positive
     */
    public SwingPdfExporter dpi(float dpi) {
        if (dpi <= 0) throw new IllegalArgumentException("DPI must be positive");
        this.dpi = dpi;
        return this;
    }

    /**
     * Set the export mode.
     * <ul>
     *   <li>{@link ExportMode#DATA_REPORT} (default) -- all rows, all tabs, full scroll content,
     *       auto-pagination, PDF bookmarks for tabs.</li>
     *   <li>{@link ExportMode#UI_SNAPSHOT} -- only the currently visible content.</li>
     * </ul>
     *
     * @param mode the export mode; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public SwingPdfExporter exportMode(ExportMode mode) {
        this.exportMode = Objects.requireNonNull(mode);
        return this;
    }

    /**
     * Register a custom font file resolver. The resolver is consulted when the
     * library cannot automatically locate a {@code .ttf} or {@code .otf} file
     * for an AWT font. If the resolver also returns empty, the library falls
     * back to a standard PDF Type 1 font.
     *
     * @param resolver the font resolver; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code resolver} is {@code null}
     * @see FontResolver
     */
    public SwingPdfExporter withFontResolver(FontResolver resolver) {
        this.fontResolver = Objects.requireNonNull(resolver);
        return this;
    }

    /**
     * Register a custom image embedding handler. The handler is called whenever
     * a {@link java.awt.image.BufferedImage} needs to be embedded in the PDF.
     * Returning {@link java.util.Optional#empty()} from the handler causes the
     * library to use its default lossless PNG embedding.
     *
     * @param handler the image handler; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code handler} is {@code null}
     * @see ImageHandler
     */
    public SwingPdfExporter withImageHandler(ImageHandler handler) {
        this.imageHandler = Objects.requireNonNull(handler);
        return this;
    }

    /**
     * Set the PDF document title (stored in the document information dictionary).
     *
     * @param title the document title, or {@code null} to omit
     * @return this builder
     */
    public SwingPdfExporter title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the PDF document author (stored in the document information dictionary).
     *
     * @param author the document author, or {@code null} to omit
     * @return this builder
     */
    public SwingPdfExporter author(String author) {
        this.author = author;
        return this;
    }

    /**
     * Set the PDF document subject (stored in the document information dictionary).
     *
     * @param subject the document subject, or {@code null} to omit
     * @return this builder
     */
    public SwingPdfExporter subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Set the PDF document keywords (stored in the document information dictionary).
     *
     * @param keywords comma-separated keywords, or {@code null} to omit
     * @return this builder
     */
    public SwingPdfExporter keywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     * Set a header band rendered in the top margin on every page.
     *
     * <p>For per-page variation (e.g. a different header on the cover page),
     * use {@link #header(HeaderFooterProvider)} instead.
     *
     * @param header the header definition; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code header} is {@code null}
     * @see HeaderFooter
     */
    public SwingPdfExporter header(HeaderFooter header) {
        Objects.requireNonNull(header, "header must not be null");
        this.headerProvider = HeaderFooterProvider.of(header);
        return this;
    }

    /**
     * Set a per-page header provider. Invoked once per page during rendering;
     * the returned band is drawn in the top margin, or omitted if the provider
     * returns {@code null} for that page.
     *
     * @param provider the per-page header provider; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code provider} is {@code null}
     * @see HeaderFooterProvider
     * @since 1.3.0
     */
    public SwingPdfExporter header(HeaderFooterProvider provider) {
        this.headerProvider = Objects.requireNonNull(provider, "provider must not be null");
        return this;
    }

    /**
     * Set a footer band rendered in the bottom margin on every page.
     *
     * <p>For per-page variation (e.g. omitting the page number on the cover
     * page), use {@link #footer(HeaderFooterProvider)} instead.
     *
     * @param footer the footer definition; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code footer} is {@code null}
     * @see HeaderFooter
     */
    public SwingPdfExporter footer(HeaderFooter footer) {
        Objects.requireNonNull(footer, "footer must not be null");
        this.footerProvider = HeaderFooterProvider.of(footer);
        return this;
    }

    /**
     * Set a per-page footer provider. Invoked once per page during rendering;
     * the returned band is drawn in the bottom margin, or omitted if the
     * provider returns {@code null} for that page.
     *
     * @param provider the per-page footer provider; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code provider} is {@code null}
     * @see HeaderFooterProvider
     * @since 1.3.0
     */
    public SwingPdfExporter footer(HeaderFooterProvider provider) {
        this.footerProvider = Objects.requireNonNull(provider, "provider must not be null");
        return this;
    }

    /**
     * Enable PDF AcroForm generation.
     *
     * <p>When enabled, interactive widget annotations are added to the PDF for each
     * {@code JTextField}, {@code JTextArea}, {@code JPasswordField}, {@code JCheckBox},
     * {@code JRadioButton}, and {@code JComboBox} encountered in the component tree.
     * The resulting PDF can be filled in by the user in any standard PDF viewer.
     *
     * <p>AcroForm is disabled by default because it changes the document's interactive
     * behaviour, which is not always desired (e.g. read-only reports).
     *
     * @return this builder
     */
    public SwingPdfExporter enableAcroForm() {
        this.acroFormEnabled = true;
        return this;
    }

    /**
     * Register a custom vector handler for a specific component type.
     *
     * <p>When the rendering pipeline encounters a component matching {@code type}
     * (or a subclass of it), the handler is called with a PDF-backed
     * {@link java.awt.Graphics2D}. All drawing operations performed on this
     * {@code Graphics2D} are emitted as vector PDF primitives — text remains
     * selectable and shapes are resolution-independent.
     *
     * <p>This is useful for components that perform custom painting (e.g. chart
     * libraries like JFreeChart) that would otherwise be rasterized.
     *
     * <p>User-registered handlers override built-in handlers for the same type.
     * Multiple handlers may be registered for different types.
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
     * @param <T>     the component type
     * @param type    the component class to handle; must not be {@code null}
     * @param handler the vector handler; must not be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code type} or {@code handler} is {@code null}
     * @see VectorComponentHandler
     * @since 1.1.0
     */
    public <T extends Component> SwingPdfExporter registerHandler(
            Class<T> type, VectorComponentHandler handler) {
        vectorHandlers.put(Objects.requireNonNull(type, "type must not be null"),
                           Objects.requireNonNull(handler, "handler must not be null"));
        return this;
    }

    // -----------------------------------------------------------------------
    // Terminal methods
    // -----------------------------------------------------------------------

    /**
     * Render the component tree and write the PDF to the given file path.
     * If the file already exists it is overwritten. Parent directories are
     * <em>not</em> created automatically.
     *
     * @param outputPath the file path to write to; must not be {@code null}
     * @throws NullPointerException    if {@code outputPath} is {@code null}
     * @throws SwingPdfExportException if rendering or writing fails
     * @throws LayoutException         if layout cannot be computed (e.g. headless with no size set)
     */
    public void export(Path outputPath) {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        buildEngine().export(outputPath);
    }

    /**
     * Render the component tree and write the PDF to the given output stream.
     * The caller is responsible for closing the stream after this method returns.
     *
     * @param outputStream the stream to write to; must not be {@code null}
     * @throws NullPointerException    if {@code outputStream} is {@code null}
     * @throws SwingPdfExportException if rendering or writing fails
     * @throws LayoutException         if layout cannot be computed (e.g. headless with no size set)
     */
    public void export(OutputStream outputStream) {
        Objects.requireNonNull(outputStream, "outputStream must not be null");
        buildEngine().export(outputStream);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private ExportEngine buildEngine() {
        ExportConfig config = new ExportConfig(
                root, pageSize, orientation, margins, dpi,
                exportMode, fontResolver, imageHandler,
                title, author, subject, keywords,
                headerProvider, footerProvider, acroFormEnabled,
                Map.copyOf(vectorHandlers)
        );
        return new ExportEngine(config);
    }
}
