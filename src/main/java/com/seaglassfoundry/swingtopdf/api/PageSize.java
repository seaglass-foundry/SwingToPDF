package com.seaglassfoundry.swingtopdf.api;

/**
 * Standard PDF page dimensions in points (1 pt = 1/72 inch).
 *
 * <p>Provides constants for the most common page sizes used in PDF documents.
 * Custom sizes can be created via {@link #of(float, float)}.</p>
 *
 * <h2>Standard sizes</h2>
 * <table>
 *   <caption>Available standard page sizes</caption>
 *   <tr><th>Constant</th><th>Width (pt)</th><th>Height (pt)</th><th>Approx. (inches)</th></tr>
 *   <tr><td>{@link #A3}</td><td>842</td><td>1191</td><td>11.7 x 16.5</td></tr>
 *   <tr><td>{@link #A4}</td><td>595</td><td>842</td><td>8.3 x 11.7</td></tr>
 *   <tr><td>{@link #A5}</td><td>420</td><td>595</td><td>5.8 x 8.3</td></tr>
 *   <tr><td>{@link #LETTER}</td><td>612</td><td>792</td><td>8.5 x 11.0</td></tr>
 *   <tr><td>{@link #LEGAL}</td><td>612</td><td>1008</td><td>8.5 x 14.0</td></tr>
 *   <tr><td>{@link #TABLOID}</td><td>792</td><td>1224</td><td>11.0 x 17.0</td></tr>
 * </table>
 *
 * <p>All dimensions are given in portrait orientation. When
 * {@link Orientation#LANDSCAPE} is selected, width and height are swapped
 * automatically by the export engine.</p>
 *
 * @since 1.0.0
 */
public final class PageSize {

    /** ISO A3: 842 x 1191 pt (11.7 x 16.5 in). @since 1.0.0 */
    public static final PageSize A3     = new PageSize(842f, 1191f, "A3");
    /** ISO A4: 595 x 842 pt (8.3 x 11.7 in). The default page size. @since 1.0.0 */
    public static final PageSize A4     = new PageSize(595f,  842f, "A4");
    /** ISO A5: 420 x 595 pt (5.8 x 8.3 in). @since 1.0.0 */
    public static final PageSize A5     = new PageSize(420f,  595f, "A5");
    /** US Letter: 612 x 792 pt (8.5 x 11 in). @since 1.0.0 */
    public static final PageSize LETTER = new PageSize(612f,  792f, "Letter");
    /** US Legal: 612 x 1008 pt (8.5 x 14 in). @since 1.0.0 */
    public static final PageSize LEGAL  = new PageSize(612f, 1008f, "Legal");
    /** US Tabloid / ANSI B: 792 x 1224 pt (11 x 17 in). @since 1.0.0 */
    public static final PageSize TABLOID = new PageSize(792f, 1224f, "Tabloid");

    private final float widthPt;
    private final float heightPt;
    private final String name;

    private PageSize(float widthPt, float heightPt, String name) {
        this.widthPt  = widthPt;
        this.heightPt = heightPt;
        this.name     = name;
    }

    /**
     * Creates a custom page size with the given dimensions.
     *
     * @param widthPt  page width in points (1 pt = 1/72 inch); must be positive
     * @param heightPt page height in points; must be positive
     * @return a new {@code PageSize} instance
     * @throws IllegalArgumentException if either dimension is not positive
     */
    public static PageSize of(float widthPt, float heightPt) {
        if (widthPt <= 0 || heightPt <= 0) {
            throw new IllegalArgumentException("Page dimensions must be positive");
        }
        return new PageSize(widthPt, heightPt, "Custom(" + widthPt + "x" + heightPt + ")");
    }

    /**
     * Returns the page width in points (portrait orientation).
     *
     * @return width in PDF points (1 pt = 1/72 inch)
     */
    public float getWidthPt()  { return widthPt;  }

    /**
     * Returns the page height in points (portrait orientation).
     *
     * @return height in PDF points (1 pt = 1/72 inch)
     */
    public float getHeightPt() { return heightPt; }

    @Override
    public String toString() { return name; }
}
