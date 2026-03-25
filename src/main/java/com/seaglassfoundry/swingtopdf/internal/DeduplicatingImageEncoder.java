package com.seaglassfoundry.swingtopdf.internal;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.seaglassfoundry.swingtopdf.api.ImageHandler;

/**
 * Encodes {@link BufferedImage} instances to {@link PDImageXObject} with two
 * enhancements over calling {@link LosslessFactory} directly:
 *
 * <ol>
 *   <li><b>Deduplication</b>  -- the same {@code BufferedImage} instance (compared
 *       by reference / identity) is encoded only once per export. Subsequent
 *       references return the cached {@code PDImageXObject}, so shared icon images
 *       appear in the PDF stream a single time instead of once per component.</li>
 *   <li><b>ImageHandler hook</b>  -- if the caller registered a custom
 *       {@link ImageHandler} via {@code SwingPdfExporter.withImageHandler()}, it
 *       is called first. Returning {@link Optional#empty()} falls back to lossless
 *       PNG encoding via {@link LosslessFactory}.</li>
 * </ol>
 *
 * <p>One instance is created per export and shared across all pages so that the
 * cache spans the entire document.
 */
final class DeduplicatingImageEncoder {

    private final ImageHandler userHandler; // may be null
    private final IdentityHashMap<BufferedImage, PDImageXObject> cache = new IdentityHashMap<>();

    DeduplicatingImageEncoder(ImageHandler userHandler) {
        this.userHandler = userHandler;
    }

    /**
     * Return the {@link PDImageXObject} for {@code img}, encoding it if this is
     * the first time this exact instance has been seen.
     *
     * @param img the rasterized component image to embed
     * @param doc the PDF document being built
     * @return a {@code PDImageXObject} suitable for drawing into a content stream
     */
    PDImageXObject encode(BufferedImage img, PDDocument doc) throws IOException {
        PDImageXObject cached = cache.get(img);
        if (cached != null) return cached;

        PDImageXObject result = null;
        if (userHandler != null) {
            Optional<PDImageXObject> opt = userHandler.handle(img, doc);
            if (opt.isPresent()) result = opt.get();
        }
        if (result == null) {
            result = LosslessFactory.createFromImage(doc, img);
        }
        cache.put(img, result);
        return result;
    }
}
