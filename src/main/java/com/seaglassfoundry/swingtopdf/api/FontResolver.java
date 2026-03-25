package com.seaglassfoundry.swingtopdf.api;

import java.awt.Font;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Hook interface for custom font file resolution.
 *
 * <p>When the library cannot automatically locate the {@code .ttf} or
 * {@code .otf} file for an AWT {@link Font}, it delegates to the registered
 * {@code FontResolver} before falling back to standard PDF Type 1 fonts.
 *
 * <p>Register via {@code SwingPdfExporter.withFontResolver(FontResolver)}.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface FontResolver {

    /**
     * Attempt to find the font file for {@code font}.
     *
     * @param font the AWT font that needs to be embedded in the PDF
     * @return the path to the {@code .ttf} or {@code .otf} file, or
     *         {@link Optional#empty()} if this resolver cannot locate it
     */
    Optional<Path> resolve(Font font);
}
