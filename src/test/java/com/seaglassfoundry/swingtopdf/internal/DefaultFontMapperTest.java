package com.seaglassfoundry.swingtopdf.internal;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;


import java.awt.Font;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultFontMapper} resolution and caching.
 */
class DefaultFontMapperTest {

    // -----------------------------------------------------------------------
    // Never returns null
    // -----------------------------------------------------------------------

    @Test
    void resolve_neverReturnsNull() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font("NonExistentFont", Font.PLAIN, 12));
            assertThat(font).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // Type-1 fallback selection
    // -----------------------------------------------------------------------

    @Test
    void sansSerifFont_fallsBackToHelvetica() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            assertThat(font).isInstanceOf(PDType1Font.class);
            assertThat(font.getName()).containsIgnoringCase("Helvetica");
        }
    }

    @Test
    void serifFont_fallsBackToTimes() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            // Use a name that contains "Serif" but not "Sans"
            PDFont font = mapper.resolve(new Font(Font.SERIF, Font.PLAIN, 12));
            assertThat(font.getName()).containsIgnoringCase("Times");
        }
    }

    @Test
    void monospaceFont_fallsBackToCourier() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            assertThat(font.getName()).containsIgnoringCase("Courier");
        }
    }

    // -----------------------------------------------------------------------
    // Bold / italic variants
    // -----------------------------------------------------------------------

    @Test
    void boldFont_returnsBoldVariant() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            assertThat(font.getName()).containsIgnoringCase("Bold");
        }
    }

    @Test
    void italicFont_returnsObliqueVariant() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            assertThat(font.getName()).containsIgnoringCase("Oblique");
        }
    }

    @Test
    void boldItalicFont_returnsBoldObliqueVariant() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont font = mapper.resolve(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 12));
            assertThat(font.getName()).contains("Bold");
            // Helvetica uses "Oblique" not "Italic"
            assertThat(font.getName()).containsIgnoringCase("Oblique");
        }
    }

    // -----------------------------------------------------------------------
    // Caching
    // -----------------------------------------------------------------------

    @Test
    void sameFontResolvedTwice_returnsCachedInstance() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            Font awtFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            PDFont first  = mapper.resolve(awtFont);
            PDFont second = mapper.resolve(awtFont);
            assertThat(second).isSameAs(first);
        }
    }

    @Test
    void differentStyles_differentCacheEntries() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            DefaultFontMapper mapper = new DefaultFontMapper(doc, null);
            PDFont plain = mapper.resolve(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            PDFont bold  = mapper.resolve(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            assertThat(bold).isNotSameAs(plain);
        }
    }

    // -----------------------------------------------------------------------
    // User-supplied resolver
    // -----------------------------------------------------------------------

    @Test
    void userResolver_calledBeforeFallback() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            boolean[] called = { false };
            DefaultFontMapper mapper = new DefaultFontMapper(doc, font -> {
                called[0] = true;
                return java.util.Optional.empty(); // decline — let fallback handle it
            });
            mapper.resolve(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            assertThat(called[0]).isTrue();
        }
    }
}
