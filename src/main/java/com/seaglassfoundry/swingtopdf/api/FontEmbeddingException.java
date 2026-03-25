package com.seaglassfoundry.swingtopdf.api;

import java.awt.Font;

/**
 * Thrown when a font file cannot be loaded or embedded into the output PDF.
 *
 * <p>By default the library does <em>not</em> throw this exception. When a font
 * file cannot be found, the library falls back to a standard PDF Type 1 font
 * (Helvetica, Times Roman, or Courier based on the font family name) and logs
 * a warning via SLF4J.</p>
 *
 * <p>Common causes include:
 * <ul>
 *   <li>The font file is missing from the system font directories</li>
 *   <li>The font file is corrupt or in an unsupported format</li>
 *   <li>The JVM's internal font path is inaccessible (no {@code --add-opens})</li>
 * </ul>
 *
 * @see com.seaglassfoundry.swingtopdf.SwingPdfExporter#withFontResolver(com.seaglassfoundry.swingtopdf.api.FontResolver)
 *
 * @since 1.0.0
 */
public class FontEmbeddingException extends SwingPdfExportException {

    private static final long serialVersionUID = 1L;

    private final Font font;

    /**
     * Constructs a font embedding exception.
     *
     * @param font  the AWT font that could not be embedded
     * @param cause the underlying I/O or font parsing error
     */
    public FontEmbeddingException(Font font, Throwable cause) {
        super("Failed to embed font: " + font.getFontName(), cause);
        this.font = font;
    }

    /**
     * Returns the AWT {@link Font} that could not be embedded.
     *
     * @return the problematic font instance
     */
    public Font getFont() { return font; }
}
