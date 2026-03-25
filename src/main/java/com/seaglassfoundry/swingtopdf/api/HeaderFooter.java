package com.seaglassfoundry.swingtopdf.api;

import java.awt.Color;
import java.util.Objects;

/**
 * Defines a header or footer band rendered in the page margin on every page.
 *
 * <h2>Token substitution</h2>
 * The text string may contain the following tokens:
 * <ul>
 *   <li>{@code {page}}   -- replaced with the current page number (1-based)</li>
 *   <li>{@code {pages}}  -- replaced with the total page count</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SwingPdfExporter.from(panel)
 *     .header(HeaderFooter.of("Quarterly Report").align(HeaderFooter.Alignment.LEFT))
 *     .footer(HeaderFooter.of("Page {page} of {pages}"))
 *     .export(path);
 * }</pre>
 *
 * <p>Instances are immutable; each {@code with*} / {@code align} / {@code fontSize} /
 * {@code color} call returns a new object.
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

    private final String    text;
    private final Alignment alignment;
    private final float     fontSize;
    private final Color     color;
    private final Color     backgroundColor;
    private final float     height;

    private HeaderFooter(String text, Alignment alignment, float fontSize, Color color,
                         Color backgroundColor, float height) {
        this.text            = Objects.requireNonNull(text,      "text must not be null");
        this.alignment       = Objects.requireNonNull(alignment, "alignment must not be null");
        this.fontSize        = fontSize;
        this.color           = Objects.requireNonNull(color,     "color must not be null");
        this.backgroundColor = backgroundColor;
        this.height          = height;
    }

    /**
     * Create a band with the given text, centered, 9 pt, gray.
     *
     * @param text band text; may contain {@code {page}} and {@code {pages}} tokens;
     *             must not be {@code null}
     * @return a new {@code HeaderFooter} instance with default styling
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static HeaderFooter of(String text) {
        return new HeaderFooter(text, Alignment.CENTER, 9f, Color.GRAY, null, 0f);
    }

    /**
     * Return a copy of this band with a different horizontal alignment.
     *
     * @param alignment the alignment; must not be {@code null}
     * @return a new {@code HeaderFooter} with the specified alignment
     * @throws NullPointerException if {@code alignment} is {@code null}
     */
    public HeaderFooter align(Alignment alignment) {
        return new HeaderFooter(text, alignment, fontSize, color, backgroundColor, height);
    }

    /**
     * Return a copy of this band with a different font size.
     *
     * @param size font size in points; must be positive
     * @return a new {@code HeaderFooter} with the specified font size
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    public HeaderFooter fontSize(float size) {
        if (size <= 0) throw new IllegalArgumentException("fontSize must be positive");
        return new HeaderFooter(text, alignment, size, color, backgroundColor, height);
    }

    /**
     * Return a copy of this band with a different text color.
     *
     * @param color the text color; must not be {@code null}
     * @return a new {@code HeaderFooter} with the specified color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public HeaderFooter color(Color color) {
        return new HeaderFooter(text, alignment, fontSize, color, backgroundColor, height);
    }

    /**
     * Return a copy of this band with a different background color. When set,
     * the entire band area is filled with this color before the text is drawn.
     * Pass {@code null} to remove the background (the default).
     *
     * @param backgroundColor the background fill color, or {@code null} for transparent
     * @return a new {@code HeaderFooter} with the specified background color
     */
    public HeaderFooter backgroundColor(Color backgroundColor) {
        return new HeaderFooter(text, alignment, fontSize, color, backgroundColor, height);
    }

    /**
     * Return a copy of this band with a specific height in points. When set to a
     * positive value, the background rectangle and text are sized to this height
     * instead of filling the entire margin. The band is anchored to the page edge
     * (top for headers, bottom for footers). Values greater than the margin are
     * clamped to the margin at render time. A value of {@code 0} (the default)
     * means "use the full margin height".
     *
     * @param height band height in points; must not be negative
     * @return a new {@code HeaderFooter} with the specified height
     * @throws IllegalArgumentException if {@code height} is negative
     */
    public HeaderFooter height(float height) {
        if (height < 0) throw new IllegalArgumentException("height must not be negative");
        return new HeaderFooter(text, alignment, fontSize, color, backgroundColor, height);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the band text, which may contain {@code {page}} and {@code {pages}} tokens.
     *
     * @return the raw text template
     */
    public String    text()      { return text; }

    /**
     * Returns the horizontal alignment of the text within the printable width.
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
     * Resolve {@code {page}} and {@code {pages}} tokens in the text template.
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
