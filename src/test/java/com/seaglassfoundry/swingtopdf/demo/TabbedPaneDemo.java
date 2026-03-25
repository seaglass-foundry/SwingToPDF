package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates {@code JTabbedPane} export in both modes.
 *
 * <ul>
 *   <li><b>UI_SNAPSHOT</b> — only the selected tab is rendered (what you see on screen).</li>
 *   <li><b>DATA_REPORT</b> — all tabs are stacked vertically with titled section headers,
 *       so every panel's data is captured regardless of which tab is selected.</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.TabbedPaneDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class TabbedPaneDemo {

    public static void main(String[] args) throws Exception {
        Path outSnapshot   = Paths.get("swingtopdf-tabbed-snapshot.pdf").toAbsolutePath();
        Path outDataReport = Paths.get("swingtopdf-tabbed-datareport.pdf").toAbsolutePath();

        JTabbedPane[] holder = new JTabbedPane[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildTabbedPane();
            holder[0].setPreferredSize(new Dimension(750, 600));
            holder[0].setSize(750, 600);
            holder[0].validate();
            latch.countDown();
        });
        latch.await();

        HeaderFooter footer = HeaderFooter.of("Page {page} of {pages}");

        // UI_SNAPSHOT — only the selected (first) tab is in the PDF
        System.out.println("Exporting UI_SNAPSHOT → " + outSnapshot);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.UI_SNAPSHOT)
                .footer(footer)
                .export(outSnapshot);

        // DATA_REPORT — all four tabs are stacked vertically with section headers
        System.out.println("Exporting DATA_REPORT → " + outDataReport);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .footer(footer)
                .export(outDataReport);

        System.out.println("Done.");
        System.out.println("  snapshot:    only the 'Overview' tab is present.");
        System.out.println("  data report: all four tabs appear as labelled sections.");

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(outSnapshot.toFile());
            java.awt.Desktop.getDesktop().open(outDataReport.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JTabbedPane buildTabbedPane() {
        JTabbedPane tp = new JTabbedPane();

        // Tab 1: Overview with KPIs
        tp.addTab("Overview", overviewTab());

        // Tab 2: Revenue detail table
        tp.addTab("Revenue",  revenueTab());

        // Tab 3: Headcount data
        tp.addTab("Headcount", headcountTab());

        // Tab 4: Notes
        tp.addTab("Notes", notesTab());

        tp.setSelectedIndex(0);
        return tp;
    }

    // -----------------------------------------------------------------------
    // Tab content builders
    // -----------------------------------------------------------------------

    private static JPanel overviewTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        p.add(heading("Q1 2026 — Executive Overview"));
        p.add(vgap(12));

        // KPI row
        JPanel kpis = new JPanel(new GridLayout(1, 3, 12, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        kpis.add(kpiCard("Total Revenue", "$4.83M", "+14%", new Color(0xDFEAFB)));
        kpis.add(kpiCard("New Customers", "1 240",  "+22%", new Color(0xD6F0E0)));
        kpis.add(kpiCard("Avg Deal Size", "$3 895", "+6%",  new Color(0xFFF3CC)));
        p.add(kpis);
        p.add(vgap(16));

        p.add(label("Revenue exceeded targets by 14% across all regions. " +
                    "See the Revenue tab for a full breakdown by product and region."));
        return p;
    }

    private static JPanel revenueTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Revenue by Product and Region — Q1 2026");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(0x2B4C7E));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"Product", "North", "South", "East", "West", "Central", "Total"};
        Object[][] rows = {
            {"Widget A",  "$280K", "$210K", "$245K", "$195K", "$145K", "$1 075K"},
            {"Widget B",  "$220K", "$175K", "$190K", "$160K", "$120K", "$865K"},
            {"Widget C",  "$310K", "$240K", "$270K", "$205K", "$150K", "$1 175K"},
            {"Gadget X",  "$180K", "$135K", "$155K", "$125K", "$90K",  "$685K"},
            {"Gadget Y",  "$195K", "$150K", "$170K", "$135K", "$100K", "$750K"},
            {"Other",     "$195K", "$110K", "$225K", "$110K", "$75K",  "$715K"},
            {"Total",     "$1 38K","$1 02K","$1 255K","$930K","$680K", "$5 265K"},
        };
        @SuppressWarnings("serial")
        DefaultTableModel dtm = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(dtm);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private static JPanel headcountTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Headcount Summary — End of Q1 2026");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(0x2B4C7E));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"Department", "Headcount", "Open Roles", "Avg Tenure (yrs)"};
        Object[][] rows = {
            {"Engineering",    "142", "8",  "3.2"},
            {"Sales",          "87",  "5",  "2.8"},
            {"Marketing",      "34",  "2",  "3.9"},
            {"Finance",        "28",  "1",  "5.1"},
            {"HR & Legal",     "19",  "0",  "4.7"},
            {"Customer Ops",   "63",  "4",  "2.1"},
            {"Total",          "373", "20", "3.3"},
        };
        @SuppressWarnings("serial")
        DefaultTableModel dtm = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(dtm);
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private static JPanel notesTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        p.add(heading("Analyst Notes"));
        p.add(vgap(10));

        String[] notes = {
            "1. All revenue figures are unaudited and subject to revision.",
            "2. Exchange rates as of 31 March 2026 (Federal Reserve published rates).",
            "3. Headcount includes full-time employees only; contractors are excluded.",
            "4. Open roles reflect approved requisitions as of the reporting date.",
            "5. This report was generated automatically by swing2pdf DATA_REPORT mode,",
            "   which renders all tabs — including this Notes tab — regardless of which",
            "   tab was selected in the UI at the time of export.",
        };
        for (String note : notes) {
            JLabel l = new JLabel(note);
            l.setFont(l.getFont().deriveFont(11f));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(l);
            p.add(vgap(4));
        }
        return p;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JPanel kpiCard(String label, String value, String delta, Color bg) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bg);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(10f));
        lbl.setForeground(Color.DARK_GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.BOLD, 20f));
        val.setForeground(new Color(0x2B4C7E));
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dlt = new JLabel(delta + " vs Q1 2025");
        dlt.setFont(dlt.getFont().deriveFont(10f));
        dlt.setForeground(new Color(0x2A7A3A));
        dlt.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lbl);
        card.add(val);
        card.add(dlt);
        return card;
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(new Color(0x2B4C7E));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel("<html><body style='width:600px'>" + text + "</body></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }
}
