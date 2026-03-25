package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Verifies that JTable column headers and all data cells are selectable in
 * the exported PDF.
 */
class JTableHandlerTest {

    // -----------------------------------------------------------------------
    // Basic rendering
    // -----------------------------------------------------------------------

    @Test
    void columnHeaders_areSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTable table = buildTable(
                new String[]{"Name", "Score", "Grade"},
                new Object[][]{{"Alice", 95, "A"}, {"Bob", 72, "C"}});

        Path pdf = export(table, tmp.resolve("headers.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Name");
            assertThat(text).contains("Score");
            assertThat(text).contains("Grade");
        }
    }

    @Test
    void dataCells_areSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTable table = buildTable(
                new String[]{"First", "Last"},
                new Object[][]{{"Alice", "Smith"}, {"Bob", "Jones"}});

        Path pdf = export(table, tmp.resolve("cells.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Alice");
            assertThat(text).contains("Smith");
            assertThat(text).contains("Bob");
            assertThat(text).contains("Jones");
        }
    }

    @Test
    void headersAndData_allSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTable table = buildTable(
                new String[]{"City", "Population"},
                new Object[][]{{"London", "9M"}, {"Berlin", "3.6M"}, {"Paris", "2.1M"}});

        Path pdf = export(table, tmp.resolve("full.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("City");
            assertThat(text).contains("Population");
            assertThat(text).contains("London");
            assertThat(text).contains("Berlin");
            assertThat(text).contains("Paris");
        }
    }

    // -----------------------------------------------------------------------
    // DATA_REPORT mode — all model rows rendered
    // -----------------------------------------------------------------------

    @Test
    void dataReport_allRowsRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // Build a table with more rows than the viewport shows
        Object[][] rows = new Object[20][2];
        for (int i = 0; i < 20; i++) {
            rows[i][0] = "Row-" + i;
            rows[i][1] = "Val-" + i;
        }
        JTable table = buildTable(new String[]{"Key", "Value"}, rows);

        // Wrap in a scroll pane that only shows ~5 rows visually
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(300, 120));
        scroll.setSize(300, 120);
        scroll.validate();

        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(300, 120));
        root.setSize(300, 120);
        root.add(scroll, BorderLayout.CENTER);
        root.validate();

        Path pdf = tmp.resolve("datareport.pdf");
        SwingPdfExporter.from(root)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .export(pdf);

        // All 20 rows must be present in the extracted text
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            for (int i = 0; i < 20; i++) {
                assertThat(text).contains("Row-" + i);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Custom cell renderer
    // -----------------------------------------------------------------------

    @Test
    void customCellRenderer_textExtracted(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{{"42"}, {"17"}},
                new String[]{"Amount"});
        JTable table = new JTable(model);

        // Right-align numbers via a custom renderer
        table.getColumnModel().getColumn(0).setCellRenderer(
                (t, val, sel, foc, row, col) -> {
                    JLabel lbl = new JLabel(val != null ? val.toString() : "");
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    return lbl;
                });

        Path pdf = export(table, tmp.resolve("renderer.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("42");
            assertThat(text).contains("17");
        }
    }

    // -----------------------------------------------------------------------
    // Non-JLabel cell renderers
    // -----------------------------------------------------------------------

    /**
     * Boolean column: JTable uses JCheckBox as the default renderer.
     * The exported PDF must contain the header text; the checkbox cells
     * must not crash and the PDF must be valid.
     */
    @Test
    void booleanColumn_checkboxRenderer_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        String[] cols = {"Name", "Active", "Verified"};
        Object[][] rows = {
            {"Alice", Boolean.TRUE,  Boolean.FALSE},
            {"Bob",   Boolean.FALSE, Boolean.TRUE},
            {"Carol", Boolean.TRUE,  Boolean.TRUE},
        };
        @SuppressWarnings("serial")
        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override public Class<?> getColumnClass(int col) {
                return col == 0 ? String.class : Boolean.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);

        Path pdf = export(table, tmp.resolve("bool.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            // Column headers must be selectable
            assertThat(text).contains("Name");
            assertThat(text).contains("Active");
            assertThat(text).contains("Verified");
            // String-column cells must be selectable
            assertThat(text).contains("Alice");
            assertThat(text).contains("Bob");
            assertThat(text).contains("Carol");
        }
    }

    /**
     * Custom renderer that returns a JProgressBar (not a JLabel, not a button).
     * Must fall back to rasterization without throwing.
     */
    @Test
    void customRenderer_nonLabel_rasterizesFallback(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        String[] cols = {"Task", "Progress"};
        Object[][] rows = {
            {"Download", 72},
            {"Install",  45},
            {"Configure", 100},
        };
        @SuppressWarnings("serial")
        DefaultTableModel dtm = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(dtm);
        table.setRowHeight(24);

        // Register a JProgressBar renderer for the Progress column
        table.getColumnModel().getColumn(1).setCellRenderer((tbl, val, sel, foc, row, col) -> {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(val instanceof Integer i ? i : 0);
            bar.setStringPainted(true);
            return bar;
        });

        Path pdf = export(table, tmp.resolve("progress.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            // String cells in the first column must still be selectable
            assertThat(text).contains("Task");
            assertThat(text).contains("Download");
            assertThat(text).contains("Install");
            assertThat(text).contains("Configure");
        }
    }

    /**
     * Mixed table: string column (JLabel), boolean column (JCheckBox vector),
     * and integer column (custom JLabel renderer with right-aligned text).
     * All text must be selectable.
     */
    @Test
    void mixedRenderers_allTextColumnsSelectable(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        String[] cols = {"Employee", "Active", "Score"};
        Object[][] rows = {
            {"Diana",   Boolean.TRUE,  95},
            {"Edward",  Boolean.FALSE, 72},
            {"Fiona",   Boolean.TRUE,  88},
        };
        @SuppressWarnings("serial")
        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override public Class<?> getColumnClass(int col) {
                if (col == 1) return Boolean.class;
                if (col == 2) return Integer.class;
                return String.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(22);

        Path pdf = export(table, tmp.resolve("mixed.pdf"), ExportMode.DATA_REPORT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Employee");
            assertThat(text).contains("Active");
            assertThat(text).contains("Score");
            assertThat(text).contains("Diana");
            assertThat(text).contains("Edward");
            assertThat(text).contains("Fiona");
            // Integer cells rendered by default JLabel renderer — must be selectable
            assertThat(text).contains("95");
            assertThat(text).contains("72");
            assertThat(text).contains("88");
        }
    }

    // -----------------------------------------------------------------------
    // Empty table
    // -----------------------------------------------------------------------

    @Test
    void emptyTable_producesValidPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTable table = buildTable(new String[]{"A", "B"}, new Object[0][0]);

        Path pdf = export(table, tmp.resolve("empty.pdf"), ExportMode.UI_SNAPSHOT);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            // Headers should still appear
            assertThat(text).contains("A");
            assertThat(text).contains("B");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JTable buildTable(String[] cols, Object[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, cols);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        // Size the scroll pane to show the full table
        int rowH    = table.getRowHeight();
        int headerH = 24;
        int totalH  = headerH + rows.length * rowH + 4;
        scroll.setPreferredSize(new Dimension(400, totalH));
        scroll.setSize(400, totalH);
        scroll.validate();
        return table;
    }

    // -----------------------------------------------------------------------
    // Column scaling
    // -----------------------------------------------------------------------

    @Test
    void wideTable_columnsScaleToFit(@TempDir Path tmp) throws Exception {
        // 10 columns × 200px = 2000px, root panel 400px → columns must scale
        String[] cols = {"C1","C2","C3","C4","C5","C6","C7","C8","C9","C10"};
        Object[][] rows = {{"a","b","c","d","e","f","g","h","i","j"}};
        JTable table = new JTable(new DefaultTableModel(rows, cols));
        for (int i = 0; i < 10; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(200);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSize(2000, 30);

        JScrollPane sp = new JScrollPane(table);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(sp, BorderLayout.CENTER);
        root.setSize(400, 60);
        root.validate();

        Path pdf = tmp.resolve("wide.pdf");
        SwingPdfExporter.from(root).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            for (String col : cols) assertThat(text).contains(col);
        }
    }

    /**
     * Export the scroll pane containing the table, sized to show all rows.
     */
    private static Path export(JTable table, Path out, ExportMode mode) throws Exception {
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class, table);

        if (scroll == null) {
            // Table not in a scroll pane — wrap it for this export
            scroll = new JScrollPane(table);
            int totalH = 24 + table.getModel().getRowCount() * table.getRowHeight() + 4;
            scroll.setPreferredSize(new Dimension(400, totalH));
            scroll.setSize(400, totalH);
            scroll.validate();
        }

        SwingPdfExporter.from(scroll)
                .pageSize(PageSize.A4)
                .exportMode(mode)
                .export(out);
        return out;
    }
}
