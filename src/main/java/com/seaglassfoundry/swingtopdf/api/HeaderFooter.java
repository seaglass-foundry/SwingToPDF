package com.seaglassfoundry.swingtopdf.api;

import java.awt.Color;
import java.util.Objects;

import javax.swing.JComponent;

/**
 * Defines a header or footer band rendered in the page margin on every page.
 *
 * <h2>Three rendering modes</h2>
 * A {@code HeaderFooter} is created in one of three modes via the corresponding
 * factory method:
 * <ul>
 *   <li>{@link #of(String)}      -- plain text (original behaviour)</li>
 *   <li>{@link #html(String)}    -- HTML string parsed for inline styling
 *       ({@code <b>}, {@code <i>}, {@code <font color>}, inline CSS, etc.)</li>
 *   <li>{@link #of(JComponent)}  -- any Swing component (JLabel, JPanel, ...)
 *       rendered through the normal handler pipeline, so icons, borders, custom
 *       painting, and HTML labels all work</li>
 * </ul>
 *
 * <h2>Token substitution</h2>
 * Text strings (TEXT and HTML modes) may contain the following tokens:
 * <ul>
 *   <li><code>&#123;page&#125;</code>   -- replaced with the current page number (1-based)</li>
 *   <li><code>&#123;pages&#125;</code>  -- replaced with the total page count</li>
 * </ul>
 * For component mode, these tokens are resolved in the text of any
 * {@code JLabel} or {@code JTextComponent} in the component tree before each
 * page is rendered.
 *
 * <h2>Usage</h2>
 * <pre>
 * // Plain text
 * HeaderFooter.of("Quarterly Report").align(Alignment.LEFT);
 *
 * // HTML string
 * HeaderFooter.html("&lt;b style='color:#036'&gt;Quarterly Report&lt;/b&gt; " +
 *                   "&amp;mdash; Page &#123;page&#125;/&#123;pages&#125;");
 *
 * // JLabel with HTML + background
 * JLabel lbl = new JLabel("&lt;html&gt;&lt;b&gt;Quarterly Report&lt;/b&gt;&lt;/html&gt;");
 * lbl.setOpaque(true);
 * lbl.setBackground(new Color(0xEFEFEF));
 * lbl.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
 * HeaderFooter.of(lbl);
 * </pre>
 *
 * <p>Instances are immutable; each styling method returns a new object.
 *
 * @since 1.0.0
 */
public final class HeaderFooter {

    /**
     * Horizontal alignment of the band text within the printable width.
     *
     * @since 1.0.0
     */
    public enum Alignment {
        /** Align text to the left margin. */
        LEFT,
        /** Center text between the left and right margins. */
        CENTER,
        /** Align text to the right margin. */
        RIGHT
    }

    /**
     * Which rendering path the band uses. Selected by the chosen factory
     * method; cannot be changed after construction.
     *
     * @since 1.2.0
     */
    public enum Mode {
        /** Plain text, drawn with Helvetica. */
        TEXT,
        /** HTML string, parsed and rendered as styled vector text. */
        HTML,
        /** Arbitrary Swing component, rendered through the handler pipeline. */
        COMPONENT
    }

    private final Mode       mode;
    private final String     text;             // TEXT / HTML; "" when COMPONENT
    private final JComponent component;        // COMPONENT; null otherwise
    private final Alignment  alignment;
    private final float      fontSize;
    private final Color      color;
    private final Color      backgroundColor;
    private final float      height;
    private final boolean    wrap;             // opt-in word wrap for TEXT mode

    private HeaderFooter(Mode mode, String text, JComponent component,
                         Alignment alignment, float fontSize, Color color,
                         Color backgroundColor, float height, boolean wrap) {
        this.mode            = Objects.requireNonNull(mode,      "mode must not be null");
        this.text            = text;
        this.component       = component;
        this.alignment       = Objects.requireNonNull(alignment, "alignment must not be null");
        this.fontSize        = fontSize;
        this.color           = Objects.requireNonNull(color,     "color must not be null");
        this.backgroundColor = backgroundColor;
        this.height          = height;
        this.wrap            = wrap;
    }

    /**
     * Create a plain-text band with the given text, centered, 9 pt, gray.
     *
     * @param text band text; may contain <code>&#123;page&#125;</code> and <code>&#123;pages&#125;</code> tokens;
     *             must not be {@code null}
     * @return a new {@code HeaderFooter} in {@link Mode#TEXT} with default styling
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static HeaderFooter of(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return new HeaderFooter(Mode.TEXT, text, null,
                Alignment.CENTER, 9f, Color.GRAY, null, 0f, false);
    }

    /**
     * Create an HTML band. The string is parsed for inline styling (bold, italic,
     * underline, strikethrough, {@code <font color>}, {@code <font size>}, inline
     * CSS via {@code style="..."}). Wraps automatically to fit the printable width.
     *
     * <p>The {@link #color(Color)} and {@link #fontSize(float)} settings act as
     * defaults for unstyled runs; HTML markup overrides them locally.
     *
     * @param html HTML string; may contain <code>&#123;page&#125;</code>/<code>&#123;pages&#125;</code> tokens;
     *             must not be {@code null}
     * @return a new {@code HeaderFooter} in {@link Mode#HTML} with default styling
     * @throws NullPointerException if {@code html} is {@code null}
     * @since 1.2.0
     */
    public static HeaderFooter html(String html) {
        Objects.requireNonNull(html, "html must not be null");
        return new HeaderFooter(Mode.HTML, html, null,
                Alignment.CENTER, 9f, Color.GRAY, null, 0f, false);
    }

    /**
     * Create a band backed by a Swing component. The component is rendered
     * through the normal handler pipeline, so anything that works in the
     * document body (JLabel with HTML, JPanel with icons and borders, custom
     * painting via registered handlers) also works in the band.
     *
     * <p>{@link #color(Color)} and {@link #fontSize(float)} are ignored in
     * component mode -- the component supplies its own font and foreground.
     * {@link #align(Alignment)} is also ignored: the component always spans
     * the full printable width so that borders, opaque backgrounds, and
     * {@code BorderLayout} children stretch edge to edge; use the component's
     * own alignment API (e.g. {@link javax.swing.JLabel#setHorizontalAlignment}
     * or the layout manager) to position visible content within the band.
     * {@link #backgroundColor(Color)} paints a band-wide fill behind the
     * component, and {@link #height(float)} overrides the band height.
     *
     * @param component the component to render; must not be {@code null}
     * @return a new {@code HeaderFooter} in {@link Mode#COMPONENT}
     * @throws NullPointerException if {@code component} is {@code null}
     * @since 1.2.0
     */
    public static HeaderFooter of(JComponent component) {
        Objects.requireNonNull(component, "component must not be null");
        return new HeaderFooter(Mode.COMPONENT, "", component,
                Alignment.CENTER, 9f, Color.GRAY, null, 0f, false);
    }

    /**
     * Return a copy of this band with a different horizontal alignment.
     *
     * @param alignment the alignment; must not be {@code null}
     * @return a new {@code HeaderFooter} with the specified alignment
     * @throws NullPointerException if {@code alignment} is {@code null}
     */
    public HeaderFooter align(Alignment alignment) {
        return new HeaderFooter(mode, text, component, alignment, fontSize,
                color, backgroundColor, height, wrap);
    }

    /**
     * Return a copy of this band with a different font size.
     * Ignored in {@link Mode#COMPONENT} (the component supplies its own font).
     *
     * @param size font size in points; must be positive
     * @return a new {@code HeaderFooter} with the specified font size
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public HeaderFooter fontSize(float size) {
        if (size <= 0) throw new IllegalArgumentException("fontSize must be positive");
        return new HeaderFooter(mode, text, component, alignment, size,
                color, backgroundColor, height, wrap);
    }

    /**
     * Return a copy of this band with a different text color.
     * Ignored in {@link Mode#COMPONENT} (the component supplies its own foreground).
     *
     * @param color the text color; must not be {@code null}
     * @return a new {@code HeaderFooter} with the specified color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public HeaderFooter color(Color color) {
        Objects.requireNonNull(color, "color must not be null");
        return new HeaderFooter(mode, text, component, alignment, fontSize,
                color, backgroundColor, height, wrap);
    }

    /**
     * Return a copy of this band with a different background color. When set,
     * the entire band area is filled with this color before the content is drawn.
     * Pass {@code null} to remove the background (the default).
     *
     * @param backgroundColor the background fill color, or {@code null} for transparent
     * @return a new {@code HeaderFooter} with the specified background color
     */
    public HeaderFooter backgroundColor(Color backgroundColor) {
        return new HeaderFooter(mode, text, component, alignment, fontSize,
                color, backgroundColor, height, wrap);
    }

    /**
     * Return a copy of this band with a specific height in points. When set to a
     * positive value, the background rectangle and content are sized to this
     * height instead of filling the entire margin. The band is anchored to the
     * page edge (top for headers, bottom for footers). Values greater than the
     * margin are clamped to the margin at render time. A value of {@code 0}
     * (the default) means "use the full margin height".
     *
     * @param height band height in points; must not be negative
     * @return a new {@code HeaderFooter} with the specified height
     * @throws IllegalArgumentException if {@code height} is negative
     */
    public HeaderFooter height(float height) {
        if (height < 0) throw new IllegalArgumentException("height must not be negative");
        return new HeaderFooter(mode, text, component, alignment, fontSize,
                color, backgroundColor, height, wrap);
    }

    /**
     * Return a copy of this band with word-wrapping enabled or disabled. Only
     * meaningful in {@link Mode#TEXT}: when {@code true}, text that exceeds the
     * printable width is broken at word boundaries and rendered across multiple
     * lines (lines that don't fit in the band height are clipped). HTML and
     * component bands always wrap to fit the printable width, so this setting
     * is a no-op for those modes.
     *
     * @param wrap {@code true} to enable word-wrap (TEXT mode only)
     * @return a new {@code HeaderFooter} with the specified wrap setting
     * @since 1.2.0
     */
    public HeaderFooter wrap(boolean wrap) {
        return new HeaderFooter(mode, text, component, alignment, fontSize,
                color, backgroundColor, height, wrap);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the band's rendering mode.
     *
     * @return the mode
     * @since 1.2.0
     */
    public Mode mode() { return mode; }

    /**
     * Returns the band text (TEXT or HTML modes), which may contain
     * <code>&#123;page&#125;</code> and <code>&#123;pages&#125;</code> tokens. Returns an empty string
     * in {@link Mode#COMPONENT}.
     *
     * @return the raw text template
     */
    public String    text()      { return text; }

    /**
     * Returns the component backing this band in {@link Mode#COMPONENT},
     * or {@code null} for TEXT/HTML modes.
     *
     * @return the backing component, or {@code null}
     * @since 1.2.0
     */
    public JComponent component() { return component; }

    /**
     * Returns the horizontal alignment of the content within the printable width.
     *
     * @return the alignment
     */
    public Alignment alignment() { return alignment; }

    /**
     * Returns the font size in points.
     *
     * @return the font size
     */
    public float     fontSize()  { return fontSize; }

    /**
     * Returns the text color.
     *
     * @return the color
     */
    public Color     color()     { return color; }

    /**
     * Returns the background fill color, or {@code null} if transparent (the default).
     *
     * @return the background color, or {@code null}
     */
    public Color     backgroundColor() { return backgroundColor; }

    /**
     * Returns the explicit band height in points, or {@code 0} if the band should
     * fill the full margin.
     *
     * @return the band height, or {@code 0} for full-margin
     */
    public float     height()          { return height; }

    /**
     * Returns {@code true} if word-wrapping is enabled for TEXT-mode bands.
     * Always effectively {@code true} for HTML and COMPONENT modes regardless
     * of this value.
     *
     * @return the wrap setting
     * @since 1.2.0
     */
    public boolean   wrap()            { return wrap; }

    /**
     * Resolve <code>&#123;page&#125;</code> and <code>&#123;pages&#125;</code> tokens in the text template.
     * For {@link Mode#COMPONENT}, returns an empty string -- tokens inside the
     * component's text are resolved by the renderer, not this method.
     *
     * @param page  current page number (1-based)
     * @param pages total page count
     * @return the text with tokens replaced by their values
     */
    public String resolve(int page, int pages) {
        return text.replace("{page}",  String.valueOf(page))
                   .replace("{pages}", String.valueOf(pages));
    }
}
