package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.Orientation;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.awt.Desktop;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates wide-table column scaling.
 *
 * <p>Builds a JTable with 10 columns × 200 px = 2 000 px total width, then
 * exports it twice — portrait and landscape — so you can compare how the
 * automatic column-scaling changes readability.
 *
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.WideTableDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class WideTableDemo {

    private static final String[] COLUMNS = {
        "Order ID", "Customer",   "Product",   "Category", "SKU",
        "Qty",      "Unit Price", "Discount",  "Total",    "Status"
    };

    private static final Object[][] ROWS = {
        { "1001", "Acme Corp",   "Widget A",  "Hardware", "WGT-001",  5, "$9.99",   "0%",  "$49.95",  "Shipped"   },
        { "1002", "Globex",      "Gadget B",  "Software", "GDG-002",  2, "$49.99",  "5%",  "$94.98",  "Pending"   },
        { "1003", "Initech",     "Doohickey", "Hardware", "DOO-003", 12, "$3.50",   "0%",  "$42.00",  "Delivered" },
        { "1004", "Umbrella",    "Thingamaj", "Parts",    "THG-004",  1, "$199.00", "10%", "$179.10", "Backorder" },
        { "1005", "Soylent",     "Sprocket",  "Parts",    "SPR-005",  8, "$12.75",  "0%",  "$102.00", "Shipped"   },
        { "1006", "Cyberdyne",   "Module X",  "Software", "MOD-006",  3, "$89.00",  "15%", "$226.95", "Pending"   },
        { "1007", "Weyland",     "Panel Z",   "Hardware", "PNL-007",  6, "$34.50",  "5%",  "$196.65", "Shipped"   },
        { "1008", "Tyrell Corp", "Replicant", "Parts",    "REP-008",  1, "$999.00", "0%",  "$999.00", "Backorder" },
    };

    public static void main(String[] args) throws Exception {
        export(Orientation.PORTRAIT,  "wide-table-portrait.pdf");
        export(Orientation.LANDSCAPE, "wide-table-landscape.pdf");
    }

    private static void export(Orientation orientation, String filename) throws Exception {
        Path out = Paths.get(filename).toAbsolutePath();

        JPanel[] holder = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildPanel();
            latch.countDown();
        });
        latch.await();

        System.out.printf("Exporting %-10s → %s%n", orientation, out);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .orientation(orientation)
                .margins(28, 28, 28, 28)
                .export(out);
        System.out.println("Done.");

        if (Desktop.isDesktopSupported())
            Desktop.getDesktop().open(out.toFile());
    }

    private static JPanel buildPanel() {
        @SuppressWarnings("serial")
        DefaultTableModel dtm = new DefaultTableModel(ROWS, COLUMNS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(dtm);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < COLUMNS.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(200);  // 2 000 px total
        table.setRowHeight(22);

        int headerH = table.getTableHeader().getPreferredSize().height;
        int panelH  = headerH + ROWS.length * table.getRowHeight() + 8;

        JScrollPane sp = new JScrollPane(table);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(sp, BorderLayout.CENTER);
        root.setSize(800, panelH);
        root.validate();
        return root;
    }
}
