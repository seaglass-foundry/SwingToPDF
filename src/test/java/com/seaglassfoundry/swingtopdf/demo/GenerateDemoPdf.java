package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.awt.Desktop;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Generates a demo PDF exercising every registered component handler and
 * saves it to {@code swingtopdf-demo-output.pdf} in the project root.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.GenerateDemoPdf \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class GenerateDemoPdf {

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("swingtopdf-demo-output.pdf").toAbsolutePath();

        // Build and size the panel on the EDT, then export from the main thread
        JPanel[] holder = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildPanel();
            holder[0].setSize(800, 3200);   // tall enough for all sections
            holder[0].validate();
            latch.countDown();
        });
        latch.await();

        System.out.println("Exporting to: " + out);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .margins(28, 28, 28, 28)
                .export(out);
        System.out.println("Done.");

        // Open with the system viewer if available
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(out.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JPanel buildPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(heading("SwingVecPDF — Handler Coverage Demo"));
        root.add(vgap(8));

        // ---- Labels & basic text ----
        root.add(section("JLabel"));
        root.add(row(
                new JLabel("Plain label"),
                bold("Bold label"),
                italic("Italic label"),
                colored("Coloured label", new Color(0x005A9C))
        ));
        root.add(vgap(12));

        // ---- Buttons ----
        root.add(section("Buttons (JButton, JToggleButton, JCheckBox, JRadioButton)"));
        JPanel btnRow = flow(
                new JButton("OK"),
                new JButton("Cancel"),
                disabled(new JButton("Disabled"))
        );
        JPanel toggleRow = flow(
                new JToggleButton("Off", false),
                new JToggleButton("On",  true)
        );
        JPanel checkRow = flow(
                new JCheckBox("Unchecked", false),
                new JCheckBox("Checked",   true),
                disabled(new JCheckBox("Disabled", false))
        );
        ButtonGroup bg = new ButtonGroup();
        JRadioButton ra = new JRadioButton("Option A", true);
        JRadioButton rb = new JRadioButton("Option B");
        JRadioButton rc = new JRadioButton("Option C");
        bg.add(ra); bg.add(rb); bg.add(rc);
        JPanel radioRow = flow(ra, rb, rc);
        root.add(grid(btnRow, toggleRow, checkRow, radioRow));
        root.add(vgap(12));

        // ---- Text components ----
        root.add(section("Text Components"));
        JTextField tf = new JTextField("Hello, SwingVecPDF!", 30);
        JPasswordField pf = new JPasswordField("secret123", 30);
        JTextArea ta = new JTextArea(
                "Line 1: The quick brown fox jumps over the lazy dog.\n" +
                "Line 2: Pack my box with five dozen liquor jugs.\n" +
                "Line 3: How vexingly quick daft zebras jump!", 4, 30);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        root.add(labeled("JTextField:", tf));
        root.add(labeled("JPasswordField:", pf));
        root.add(labeled("JTextArea:", new JScrollPane(ta)));
        root.add(vgap(12));

        // ---- JComboBox ----
        root.add(section("JComboBox"));
        JComboBox<String> cbo = new JComboBox<>(new String[]{"Alpha", "Beta", "Gamma", "Delta"});
        cbo.setSelectedItem("Gamma");
        root.add(labeled("Selection:", cbo));
        root.add(vgap(12));

        // ---- JProgressBar ----
        root.add(section("JProgressBar"));
        JProgressBar pb1 = new JProgressBar(0, 100); pb1.setValue(72); pb1.setStringPainted(true);
        JProgressBar pb2 = new JProgressBar(0, 100); pb2.setValue(30);
        root.add(labeled("72% (string):", pb1));
        root.add(labeled("30% (no str):", pb2));
        root.add(vgap(12));

        // ---- JSlider ----
        root.add(section("JSlider"));
        JSlider sl = new JSlider(0, 100, 55);
        sl.setPaintTicks(true);
        sl.setPaintLabels(true);
        sl.setMajorTickSpacing(25);
        sl.setMinorTickSpacing(5);
        root.add(labeled("Horizontal:", sl));
        root.add(vgap(12));

        // ---- JList ----
        root.add(section("JList"));
        JList<String> list = new JList<>(new String[]{
                "Apple", "Banana", "Cherry", "Date", "Elderberry",
                "Fig", "Grape", "Honeydew", "Kiwi", "Lemon"});
        list.setFixedCellHeight(20);
        list.setVisibleRowCount(5);
        root.add(new JScrollPane(list));
        root.add(vgap(12));

        // ---- JTree ----
        root.add(section("JTree"));
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Catalogue");
        DefaultMutableTreeNode fruits = new DefaultMutableTreeNode("Fruits");
        fruits.add(new DefaultMutableTreeNode("Apple"));
        fruits.add(new DefaultMutableTreeNode("Banana"));
        fruits.add(new DefaultMutableTreeNode("Cherry"));
        DefaultMutableTreeNode veg = new DefaultMutableTreeNode("Vegetables");
        veg.add(new DefaultMutableTreeNode("Carrot"));
        veg.add(new DefaultMutableTreeNode("Daikon"));
        DefaultMutableTreeNode grains = new DefaultMutableTreeNode("Grains");
        grains.add(new DefaultMutableTreeNode("Wheat"));
        grains.add(new DefaultMutableTreeNode("Oat"));
        treeRoot.add(fruits); treeRoot.add(veg); treeRoot.add(grains);
        JTree tree = new JTree(treeRoot);
        tree.setRowHeight(18);
        tree.setVisibleRowCount(8);
        root.add(new JScrollPane(tree));
        root.add(vgap(12));

        // ---- JTable ----
        root.add(section("JTable"));
        String[] cols = {"ID", "Name", "Department", "Salary"};
        Object[][] rows = {
            {1, "Alice Nguyen",   "Engineering", "$120,000"},
            {2, "Bob Martinez",   "Marketing",    "$85,000"},
            {3, "Carol Williams", "Finance",       "$95,000"},
            {4, "David Kim",      "Engineering", "$115,000"},
            {5, "Eva Rossi",      "HR",           "$78,000"},
            {6, "Frank Chen",     "Engineering", "$130,000"},
            {7, "Grace Patel",    "Marketing",    "$88,000"},
            {8, "Henry Okafor",   "Finance",     "$102,000"},
        };
        JTable table = new JTable(new DefaultTableModel(rows, cols) {
            private static final long serialVersionUID = -7397238024220373200L;

			@Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(700, 200));
        root.add(tableScroll);
        root.add(vgap(12));

        // ---- JTabbedPane ----
        root.add(section("JTabbedPane"));
        JTabbedPane tabs = new JTabbedPane();
        JPanel tab1 = new JPanel(new BorderLayout());
        tab1.add(new JLabel("Content of the first tab — Summary"), BorderLayout.CENTER);
        JPanel tab2 = new JPanel(new BorderLayout());
        tab2.add(new JLabel("Content of the second tab — Details"), BorderLayout.CENTER);
        JPanel tab3 = new JPanel(new BorderLayout());
        tab3.add(new JLabel("Content of the third tab — Notes"), BorderLayout.CENTER);
        tabs.addTab("Summary", tab1);
        tabs.addTab("Details", tab2);
        tabs.addTab("Notes",   tab3);
        tabs.setPreferredSize(new Dimension(700, 120));
        root.add(tabs);
        root.add(vgap(12));

        // ---- Coloured panels ----
        root.add(section("Coloured JPanel grid"));
        JPanel swatches = new JPanel(new GridLayout(2, 4, 4, 4));
        swatches.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        Color[] palette = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                           Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK};
        for (Color c : palette) {
            JPanel s = new JPanel(); s.setBackground(c); s.setOpaque(true);
            s.setPreferredSize(new Dimension(80, 28));
            swatches.add(s);
        }
        root.add(swatches);
        root.add(vgap(16));

        return root;
    }

    // -----------------------------------------------------------------------
    // Builder helpers
    // -----------------------------------------------------------------------

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
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

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JLabel italic(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.ITALIC));
        return l;
    }

    private static JLabel colored(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setForeground(c);
        return l;
    }

    private static JPanel row(Component... comps) {
        JPanel p = flow(comps);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JPanel flow(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : comps) p.add(c);
        return p;
    }

    private static JPanel grid(JPanel... rows) {
        JPanel p = new JPanel(new GridLayout(rows.length, 1, 0, 2));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JPanel r : rows) p.add(r);
        return p;
    }

    private static JPanel labeled(String labelText, Component comp) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel l = new JLabel(labelText);
        l.setPreferredSize(new Dimension(160, 26));
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(l, BorderLayout.WEST);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }

    private static <T extends AbstractButton> T disabled(T btn) {
        btn.setEnabled(false);
        return btn;
    }
}
