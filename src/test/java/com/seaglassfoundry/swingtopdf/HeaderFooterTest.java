package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter.Mode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooterProvider;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that header and footer bands appear as selectable text in the PDF.
 */
class HeaderFooterTest {

    @Test
    void header_appearsOnEveryPage(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf.pdf");

        // Tall panel forces two pages
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header(HeaderFooter.of("Quarterly Report").align(HeaderFooter.Alignment.LEFT))
                .footer(HeaderFooter.of("Page {page} of {pages}"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();

            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                assertThat(text).contains("Quarterly Report");
                assertThat(text).contains("Page " + p + " of " + doc.getNumberOfPages());
            }
        }
    }

    @Test
    void tokenSubstitution_pageAndPages() {
        HeaderFooter hf = HeaderFooter.of("Page {page} of {pages}");
        assertThat(hf.resolve(3, 10)).isEqualTo("Page 3 of 10");
    }

    @Test
    void fluent_returnsNewInstance() {
        HeaderFooter a = HeaderFooter.of("x");
        HeaderFooter b = a.fontSize(12f).color(Color.BLACK).align(HeaderFooter.Alignment.RIGHT);
        assertThat(b).isNotSameAs(a);
        assertThat(b.fontSize()).isEqualTo(12f);
        assertThat(b.alignment()).isEqualTo(HeaderFooter.Alignment.RIGHT);
    }

    // -----------------------------------------------------------------------
    // Mode factory tests (v1.2.0)
    // -----------------------------------------------------------------------

    @Test
    void textFactory_producesTextMode() {
        HeaderFooter hf = HeaderFooter.of("hello");
        assertThat(hf.mode()).isEqualTo(Mode.TEXT);
        assertThat(hf.text()).isEqualTo("hello");
        assertThat(hf.component()).isNull();
        assertThat(hf.wrap()).isFalse();
    }

    @Test
    void htmlFactory_producesHtmlMode() {
        HeaderFooter hf = HeaderFooter.html("<b>bold</b>");
        assertThat(hf.mode()).isEqualTo(Mode.HTML);
        assertThat(hf.text()).isEqualTo("<b>bold</b>");
        assertThat(hf.component()).isNull();
    }

    @Test
    void componentFactory_producesComponentMode() {
        JLabel label = new JLabel("hi");
        HeaderFooter hf = HeaderFooter.of(label);
        assertThat(hf.mode()).isEqualTo(Mode.COMPONENT);
        assertThat(hf.component()).isSameAs(label);
        assertThat(hf.text()).isEmpty();
    }

    @Test
    void nullArguments_rejected() {
        assertThatThrownBy(() -> HeaderFooter.of((String) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HeaderFooter.html(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HeaderFooter.of((JComponent) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void wrap_togglesImmutably() {
        HeaderFooter a = HeaderFooter.of("long sentence");
        HeaderFooter b = a.wrap(true);
        assertThat(a.wrap()).isFalse();
        assertThat(b.wrap()).isTrue();
        assertThat(b).isNotSameAs(a);
    }

    @Test
    void modePreserved_acrossFluentChain() {
        HeaderFooter a = HeaderFooter.html("<i>x</i>");
        HeaderFooter b = a.color(Color.RED).fontSize(14f).align(HeaderFooter.Alignment.RIGHT)
                         .backgroundColor(Color.BLACK).height(20f).wrap(true);
        assertThat(b.mode()).isEqualTo(Mode.HTML);
        assertThat(b.text()).isEqualTo("<i>x</i>");

        JLabel label = new JLabel("c");
        HeaderFooter c = HeaderFooter.of(label).align(HeaderFooter.Alignment.LEFT).height(30f);
        assertThat(c.mode()).isEqualTo(Mode.COMPONENT);
        assertThat(c.component()).isSameAs(label);
    }

    // -----------------------------------------------------------------------
    // End-to-end rendering tests for each mode
    // -----------------------------------------------------------------------

    @Test
    void htmlHeader_rendersSelectableText(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-html.pdf");
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 600);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header(HeaderFooter.html("<b>HTML Header</b> &mdash; Page {page}/{pages}"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("HTML Header");
            assertThat(text).contains("Page 1/1");
        }
    }

    @Test
    void componentHeader_rendersJLabelText(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-component.pdf");
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 600);
        root.validate();

        JLabel header = new JLabel("Component Header");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(new Color(0x336699));

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header(HeaderFooter.of(header))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Component Header");
        }
    }

    @Test
    void componentHeader_resolvesPageTokens(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-component-tokens.pdf");

        // Two-page panel
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        JLabel footer = new JLabel("Report Page {page} of {pages}");
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .footer(HeaderFooter.of(footer))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                assertThat(text).contains("Report Page " + p + " of " + doc.getNumberOfPages());
            }
            // Original template restored after render
            assertThat(footer.getText()).isEqualTo("Report Page {page} of {pages}");
        }
    }

    // -----------------------------------------------------------------------
    // Per-page provider tests (v1.3.0)
    // -----------------------------------------------------------------------

    @Test
    void provider_returnsDifferentBandPerPage(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-provider-per-page.pdf");

        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header((page, pages) -> page == 1
                        ? HeaderFooter.of("ZZCOVER")
                        : HeaderFooter.of("ZZBODY"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();

            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String page1 = stripper.getText(doc);
            assertThat(page1).contains("ZZCOVER");
            assertThat(page1).doesNotContain("ZZBODY");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String page2 = stripper.getText(doc);
            assertThat(page2).contains("ZZBODY");
            assertThat(page2).doesNotContain("ZZCOVER");
        }
    }

    @Test
    void provider_returnsNullSuppressesBand(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-provider-null.pdf");

        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .footer((page, pages) -> page == 1
                        ? null
                        : HeaderFooter.of("ZZFOOTER"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            PDFTextStripper stripper = new PDFTextStripper();

            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(doc)).doesNotContain("ZZFOOTER");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(doc)).contains("ZZFOOTER");
        }
    }

    @Test
    void provider_seesCorrectPageCount(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-provider-count.pdf");

        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        List<int[]> calls = new ArrayList<>();
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header((page, pages) -> {
                    calls.add(new int[]{page, pages});
                    return HeaderFooter.of("X");
                })
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            int n = doc.getNumberOfPages();
            assertThat(calls).hasSize(n);
            for (int i = 0; i < n; i++) {
                assertThat(calls.get(i)[0]).isEqualTo(i + 1);
                assertThat(calls.get(i)[1]).isEqualTo(n);
            }
        }
    }

    @Test
    void provider_tokenSubstitutionStillWorks(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-provider-tokens.pdf");

        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 2400);
        root.validate();

        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header((page, pages) -> HeaderFooter.of("p={page}/{pages}"))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            int n = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= n; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                assertThat(stripper.getText(doc)).contains("p=" + p + "/" + n);
            }
        }
    }

    @Test
    void nullProvider_rejected() {
        JPanel root = new JPanel();
        root.setSize(400, 600);
        SwingPdfExporter exporter = SwingPdfExporter.from(root);
        assertThatThrownBy(() -> exporter.header((HeaderFooterProvider) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> exporter.footer((HeaderFooterProvider) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void providerOf_constantWrapper() {
        HeaderFooter band = HeaderFooter.of("constant");
        HeaderFooterProvider provider = HeaderFooterProvider.of(band);
        assertThat(provider.get(1, 1)).isSameAs(band);
        assertThat(provider.get(2, 5)).isSameAs(band);
        assertThat(provider.get(99, 100)).isSameAs(band);

        assertThatThrownBy(() -> HeaderFooterProvider.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void provider_singlePageDocument_usesCoverBranch(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-provider-single-page.pdf");

        // Small panel that fits on one page
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 200);
        root.validate();

        List<int[]> calls = new ArrayList<>();
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .header((page, pages) -> {
                    calls.add(new int[]{page, pages});
                    return page == 1
                            ? HeaderFooter.of("ZZCOVER")
                            : HeaderFooter.of("ZZSTANDARD");
                })
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("ZZCOVER");
            assertThat(text).doesNotContain("ZZSTANDARD");

            assertThat(calls).hasSize(1);
            assertThat(calls.get(0)).containsExactly(1, 1);
        }
    }

    // -----------------------------------------------------------------------

    @Test
    void textHeader_wrapsLongString(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("hf-wrap.pdf");
        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setSize(400, 600);
        root.validate();

        String longText = "This is a very long header that should wrap across multiple lines " +
                          "when the wrap option is enabled because it exceeds the printable width.";
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .margins(72f, 72f, 72f, 72f)
                .header(HeaderFooter.of(longText).wrap(true).height(48f))
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // The full phrase must be present across wrapped lines
            assertThat(text).contains("This is a very long header");
            assertThat(text).contains("exceeds the printable width");
        }
    }
}
