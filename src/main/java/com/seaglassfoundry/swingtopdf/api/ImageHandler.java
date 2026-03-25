package com.seaglassfoundry.swingtopdf.api;

import java.awt.image.BufferedImage;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Hook interface for custom image embedding.
 *
 * <p>Called whenever the rendering pipeline needs to embed a
 * {@link BufferedImage} into the PDF (e.g., an {@code ImageIcon} background).
 * Returning {@link Optional#empty()} causes the library to use its default
 * handling (lossless PNG compression).
 *
 * <p>Register via {@code SwingPdfExporter.withImageHandler(ImageHandler)}.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ImageHandler {

    /**
     * Create a {@link PDImageXObject} for {@code image}, or return empty to
     * use the library's default embedding logic.
     *
     * @param image    the image to embed
     * @param document the PDF document, needed to construct the XObject
     * @return a fully constructed {@code PDImageXObject}, or empty
     */
    Optional<PDImageXObject> handle(BufferedImage image, PDDocument document);
}
