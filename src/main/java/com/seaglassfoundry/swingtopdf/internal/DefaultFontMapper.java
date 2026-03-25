package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.seaglassfoundry.swingtopdf.api.FontResolver;

/**
 * Resolves Java {@link Font} objects to embedded PDFBox {@link PDFont} instances.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>User-supplied {@link FontResolver} (if set)</li>
 *   <li>{@link AwtFontFileResolver}  -- JVM-internal font file path via reflection</li>
 *   <li>{@link SystemFontFinder}  -- OS font directory scan</li>
 *   <li>Standard PDF Type-1 font (Helvetica / Times / Courier) for logical names</li>
 * </ol>
 *
 * <p>Resolved fonts are cached by family + style. TrueType fonts are subset-embedded
 * (glyph subsetting happens automatically when the document is saved).
 */
final class DefaultFontMapper {

    private static final Logger log = LoggerFactory.getLogger(DefaultFontMapper.class);

    private final PDDocument    doc;
    private final FontResolver  userResolver;
    private final Map<String, PDFont> cache = new HashMap<>();

    DefaultFontMapper(PDDocument doc, FontResolver userResolver) {
        this.doc          = doc;
        this.userResolver = userResolver;
    }

    /**
     * Resolve {@code font} to an embedded {@link PDFont}.
     * Never returns {@code null}  -- falls back to Helvetica as a last resort.
     */
    PDFont resolve(Font font) {
        String key = font.getName() + "-" + font.getStyle();
        return cache.computeIfAbsent(key, k -> {
            try {
                return doResolve(font);
            } catch (Exception e) {
                log.warn("Font resolution failed for '{}'; using Helvetica fallback: {}",
                         font.getFontName(), e.getMessage());
                return helvetica(font);
            }
        });
    }

    // -----------------------------------------------------------------------

    private PDFont doResolve(Font font) throws IOException {
        // 1. User-supplied resolver
        if (userResolver != null) {
            Optional<Path> p = userResolver.resolve(font);
            if (p.isPresent()) return loadTrueType(p.get().toFile(), font);
        }

        // 2. JVM-internal (fastest)
        Optional<File> jvmFile = AwtFontFileResolver.resolve(font);
        if (jvmFile.isPresent()) return loadTrueType(jvmFile.get(), font);

        // 3. OS font directory scan
        Optional<File> sysFile = SystemFontFinder.getInstance().findFile(font);
        if (sysFile.isPresent()) return loadTrueType(sysFile.get(), font);

        // 4. Standard Type-1 fallback
        log.debug("No font file found for '{}'; using Type-1 fallback", font.getFontName());
        return type1Fallback(font);
    }

    private PDFont loadTrueType(File file, Font font) throws IOException {
        log.debug("Embedding font '{}' from {}", font.getFontName(), file.getName());
        try (InputStream is = Files.newInputStream(file.toPath())) {
            return PDType0Font.load(doc, is, true); // true = subset embed
        }
    }

    private PDFont type1Fallback(Font font) {
        String nameLower = font.getName().toLowerCase();
        boolean serif  = nameLower.contains("serif") && !nameLower.contains("sans");
        boolean mono   = nameLower.contains("mono") || nameLower.contains("courier")
                      || font.getFamily().equalsIgnoreCase("Monospaced");
        boolean bold   = font.isBold();
        boolean italic = font.isItalic();

        if (mono) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);
            if (bold)           return new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            if (italic)         return new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);
            return               new PDType1Font(Standard14Fonts.FontName.COURIER);
        }
        if (serif) {
            if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            if (bold)           return new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            if (italic)         return new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            return               new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        }
        return helvetica(font);
    }

    private PDFont helvetica(Font font) {
        boolean bold   = font.isBold();
        boolean italic = font.isItalic();
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold)           return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic)         return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return               new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
