package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates header and footer bands rendered in the page margins on every page.
 *
 * <p>Features shown:
 * <ul>
 *   <li>Left-aligned header with a document title</li>
 *   <li>Right-aligned header with a date</li>
 *   <li>Centered footer with {@code {page} of {pages}} token substitution</li>
 *   <li>Custom font size and color on the footer</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.HeaderFooterDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class HeaderFooterDemo {

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("swingtopdf-headerfooter-demo.pdf").toAbsolutePath();

        JPanel[] holder = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildPanel();
            holder[0].setSize(750, 3000);
            holder[0].validate();
            latch.countDown();
        });
        latch.await();

        System.out.println("Exporting to: " + out);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .margins(48, 36, 48, 36)   // extra top/bottom margin for bands
                .header(HeaderFooter.of("Quarterly Sales Report — Q1 2026")
                        .align(HeaderFooter.Alignment.LEFT)
                        .fontSize(10f)
                        .color(new Color(0x2B4C7E)))
                .footer(HeaderFooter.of("Page {page} of {pages}")
                        .align(HeaderFooter.Alignment.CENTER)
                        .fontSize(9f)
                        .color(Color.GRAY))
                .export(out);
        System.out.println("Done.");

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(out.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JPanel buildPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(heading("Quarterly Sales Report — Q1 2026"));
        root.add(vgap(4));
        root.add(subheading("Generated 12 March 2026"));
        root.add(vgap(16));

        // Page 1 content: summary table
        root.add(section("Executive Summary"));
        root.add(vgap(6));
        root.add(new JLabel("Revenue exceeded targets by 14% across all regions."));
        root.add(vgap(6));

        String[] sumCols = {"Region", "Target", "Actual", "Variance"};
        Object[][] sumRows = {
            {"North",   "$1.2M", "$1.38M", "+15%"},
            {"South",   "$0.9M", "$1.02M", "+13%"},
            {"East",    "$1.1M", "$1.25M", "+14%"},
            {"West",    "$0.8M", "$0.91M", "+14%"},
            {"Central", "$0.6M", "$0.67M", "+12%"},
        };
        root.add(tableOf(sumCols, sumRows));
        root.add(vgap(20));

        // Enough rows to force 3 pages — header/footer appear on each
        root.add(section("Monthly Detail"));
        root.add(vgap(6));

        String[] detailCols = {"Month", "Product", "Units", "Revenue", "Rep"};
        Object[][] detailRows = buildDetailRows();
        root.add(tableOf(detailCols, detailRows));
        root.add(vgap(20));

        root.add(section("Notes"));
        root.add(vgap(6));
        root.add(wrappedLabel(
                "All figures are reported in USD. Exchange-rate adjustments are applied " +
                "using end-of-quarter rates published by the Federal Reserve. Individual " +
                "product lines with revenue below $10 000 are aggregated under 'Other'. " +
                "This report was generated automatically by the swingtopdf library and " +
                "contains the header/footer feature demonstration."));

        return root;
    }

    private static Object[][] buildDetailRows() {
        String[] months  = {"January", "February", "March"};
        String[] products = {"Widget A", "Widget B", "Widget C", "Widget D", "Widget E",
                             "Gadget X", "Gadget Y", "Gadget Z"};
        String[] reps    = {"Alice", "Bob", "Carol", "David", "Eva"};
        Object[][] rows = new Object[months.length * products.length][5];
        int row = 0;
        for (String month : months) {
            for (int p = 0; p < products.length; p++) {
                int units   = 100 + (p * 37 + row * 13) % 400;
                int revenue = units * (120 + p * 15);
                rows[row] = new Object[]{
                    month, products[p], units,
                    String.format("$%,d", revenue),
                    reps[row % reps.length]
                };
                row++;
            }
        }
        return rows;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(new Color(0x2B4C7E));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel subheading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 11f));
        l.setForeground(Color.GRAY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel section(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBackground(new Color(0xE8EEF8));
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setForeground(new Color(0x2B4C7E));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static Component tableOf(String[] cols, Object[][] rows) {
        JTable table = new JTable(new DefaultTableModel(rows, cols) {
            private static final long serialVersionUID = -1981823904802680942L;
			@Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane sp = new JScrollPane(table);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.min(300, rows.length * 22 + 26)));
        return sp;
    }

    private static JLabel wrappedLabel(String text) {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }
}
