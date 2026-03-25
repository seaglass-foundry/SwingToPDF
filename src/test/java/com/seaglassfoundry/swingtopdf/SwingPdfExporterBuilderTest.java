package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.Orientation;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the builder API surface: correct chaining, argument validation,
 * and that terminal methods reject null inputs before any rendering occurs.
 */
class SwingPdfExporterBuilderTest {

    @Test
    void from_nullRoot_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderChain_allMethods_returnsNonNull() {
        JPanel panel = new JPanel();
        assertThatCode(() -> SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .orientation(Orientation.LANDSCAPE)
                .margins(10, 10, 10, 10)
                .dpi(72)
                .exportMode(ExportMode.DATA_REPORT)
                .withFontResolver(font -> java.util.Optional.empty())
                .withImageHandler((img, ctx) -> java.util.Optional.empty())
        ).doesNotThrowAnyException();
    }

    @Test
    void pageSize_null_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).pageSize(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void orientation_null_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).orientation(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exportMode_null_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).exportMode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void margins_negativeValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).margins(-1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dpi_zero_throwsIllegalArgument() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).dpi(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dpi_negative_throwsIllegalArgument() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JPanel()).dpi(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void export_nullPath_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JLabel("hi")).export((java.nio.file.Path) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void export_nullStream_throwsNullPointer() {
        assertThatThrownBy(() -> SwingPdfExporter.from(new JLabel("hi")).export((java.io.OutputStream) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void metadata_isWrittenToPdf(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("meta.pdf");
        SwingPdfExporter.from(new JLabel("hi"))
                .title("My Report")
                .author("Jane Smith")
                .subject("Quarterly Results")
                .keywords("finance, Q4, 2025")
                .export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            PDDocumentInformation info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("My Report");
            assertThat(info.getAuthor()).isEqualTo("Jane Smith");
            assertThat(info.getSubject()).isEqualTo("Quarterly Results");
            assertThat(info.getKeywords()).isEqualTo("finance, Q4, 2025");
            assertThat(info.getCreator()).isEqualTo("swingtopdf");
        }
    }

    @Test
    void export_toOutputStream_producesValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SwingPdfExporter.from(new JLabel("Stream Export Test"))
                .title("Stream Test")
                .export(baos);

        assertThat(baos.size()).isGreaterThan(0);

        try (PDDocument doc = Loader.loadPDF(baos.toByteArray())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            PDDocumentInformation info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("Stream Test");
        }
    }

    @Test
    void metadata_omitted_creatorStillSet(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("meta-default.pdf");
        SwingPdfExporter.from(new JLabel("hi")).export(out);

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            PDDocumentInformation info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isNull();
            assertThat(info.getCreator()).isEqualTo("swingtopdf");
        }
    }
}
