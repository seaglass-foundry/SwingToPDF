package com.seaglassfoundry.swingtopdf.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.Orientation;
import com.seaglassfoundry.swingtopdf.api.PageSize;

/**
 * Interactive demo application for the swingtopdf library.
 *
 * <p>Launch via {@link #main(String[])} to open a frame containing a rich
 * cross-section of Swing components. Use <b>File → Export to PDF…</b> (or
 * press Ctrl+E) to render the visible content panel to a PDF file of your
 * choosing.
 *
 * <p>The "Export Options" toolbar lets you toggle full-content scroll rendering,
 * pagination mode, and annotation export before each export run.
 */
public class SwingVecPdfDemo extends JFrame {

    private static final long serialVersionUID = 1L;

    // -----------------------------------------------------------------------
    // Main content panel — this is what gets exported
    // -----------------------------------------------------------------------

    private final JPanel contentPanel;

    // Export-option controls (toolbar)
    private JComboBox<ExportMode> cboExportMode;
    private JLabel    statusLabel;

    // -----------------------------------------------------------------------

    public SwingVecPdfDemo() {
        super("SwingVecPDF Demo");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 4));

        contentPanel = buildContentPanel();

        add(buildMenuBar_asComponent(), BorderLayout.NORTH);  // toolbar row
        add(new JScrollPane(contentPanel), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        setJMenuBar(buildMenuBar());
        pack();
        setLocationRelativeTo(null);
    }

    // -----------------------------------------------------------------------
    // Menu bar
    // -----------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        // ---- File ----
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem exportItem = new JMenuItem("Export to PDF\u2026");
        exportItem.setMnemonic('E');
        exportItem.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        exportItem.addActionListener(this::onExport);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('x');
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // ---- Help ----
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "SwingVecPDF Demo\n\nUse File → Export to PDF… to export the content panel.\n" +
                "Adjust export options in the toolbar.",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(helpMenu);
        return bar;
    }

    /** Toolbar with export-option controls rendered above the content. */
    private Component buildMenuBar_asComponent() {
        JToolBar tb = new JToolBar("Export Options");
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        cboExportMode = new JComboBox<>(ExportMode.values());
        cboExportMode.setSelectedItem(ExportMode.DATA_REPORT);
        cboExportMode.setMaximumSize(cboExportMode.getPreferredSize());
        cboExportMode.setToolTipText("Export mode");

        JButton exportBtn = new JButton("Export to PDF\u2026");
        exportBtn.addActionListener(this::onExport);

        tb.add(new JLabel("Mode: "));
        tb.add(cboExportMode);
        tb.addSeparator();
        tb.add(exportBtn);
        return tb;
    }

    private Component buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statusLabel = new JLabel("Ready — use File → Export to PDF… or Ctrl+E");
        bar.add(statusLabel);
        return bar;
    }

    // -----------------------------------------------------------------------
    // Export action
    // -----------------------------------------------------------------------

    private void onExport(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Content Panel to PDF");
        fc.setSelectedFile(new File("swingtopdf-demo.pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = fc.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getParentFile(), target.getName() + ".pdf");
        }
        Path outputPath = target.toPath();

        // Capture toolbar state on the EDT before handing off to the worker
        ExportMode mode = (ExportMode) cboExportMode.getSelectedItem();

        statusLabel.setText("Exporting…");
        final File finalTarget = target;

        // Run export off the EDT to keep UI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                SwingPdfExporter.from(contentPanel)
                        .pageSize(PageSize.A4)
                        .orientation(Orientation.PORTRAIT)
                        .margins(36, 36, 36, 36)
                        .exportMode(mode)
                        .export(outputPath);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // rethrow any exception wrapped in ExecutionException
                    statusLabel.setText("Exported: " + finalTarget.getAbsolutePath());
                    int open = JOptionPane.showConfirmDialog(SwingVecPdfDemo.this,
                            "PDF saved to:\n" + finalTarget.getAbsolutePath() +
                            "\n\nOpen the containing folder?",
                            "Export Complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (open == JOptionPane.YES_OPTION) {
                        try { Desktop.getDesktop().open(finalTarget.getParentFile()); }
                        catch (Exception ex) { /* ignore */ }
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String detail = cause.getClass().getSimpleName() +
                                    (cause.getMessage() != null ? ": " + cause.getMessage() : "");
                    statusLabel.setText("Export failed — " + detail);
                    JOptionPane.showMessageDialog(SwingVecPdfDemo.this,
                            "Export failed.\n\n" + detail +
                            "\n\nCheck the console/log for the full stack trace.",
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    cause.printStackTrace(); // surface in IDE console
                }
            }
        };
        worker.execute();
    }

    // -----------------------------------------------------------------------
    // Content panel — the component tree that gets exported to PDF
    // -----------------------------------------------------------------------

    private JPanel buildContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        // Header banner
        JLabel title = new JLabel("SwingVecPDF Demo — Component Showcase", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(title, BorderLayout.NORTH);

        // Tabbed pane with one tab per component category
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Basic Controls",   buildBasicControlsTab());
        tabs.addTab("Table & List",     buildTableAndListTab());
        tabs.addTab("Text Components",  buildTextComponentsTab());
        tabs.addTab("Gauges",           buildGaugesTab());
        tabs.addTab("Layout Panels",    buildLayoutPanelsTab());

        panel.add(tabs, BorderLayout.CENTER);

        // Footer note
        JLabel footer = new JLabel(
                "<html><i>Tip: DATA_REPORT renders all tabs and rows; UI_SNAPSHOT captures only " +
                "what is currently visible.</i></html>", SwingConstants.CENTER);
        footer.setForeground(Color.GRAY);
        footer.setFont(footer.getFont().deriveFont(11f));
        panel.add(footer, BorderLayout.SOUTH);

        // Size the panel so it is comfortably exportable
        panel.setPreferredSize(new Dimension(780, 600));
        return panel;
    }

    // ---- Tab 1: Basic controls ----

    private JPanel buildBasicControlsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        // Label
        p.add(label("JLabel:"), lc);
        p.add(new JLabel("Plain label text"), fc);
        nextRow(lc, fc);

        // JButton
        p.add(label("JButton:"), lc);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setOpaque(false);
        btns.add(new JButton("OK"));
        JButton disabled = new JButton("Disabled");
        disabled.setEnabled(false);
        btns.add(disabled);
        JToggleButton toggle = new JToggleButton("Toggle", true);
        btns.add(toggle);
        p.add(btns, fc);
        nextRow(lc, fc);

        // JCheckBox
        p.add(label("JCheckBox:"), lc);
        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setOpaque(false);
        checks.add(new JCheckBox("Checked", true));
        checks.add(new JCheckBox("Unchecked", false));
        JCheckBox triState = new JCheckBox("Disabled");
        triState.setEnabled(false);
        checks.add(triState);
        p.add(checks, fc);
        nextRow(lc, fc);

        // JRadioButton
        p.add(label("JRadioButton:"), lc);
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        radios.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        JRadioButton r1 = new JRadioButton("Option A", true);
        JRadioButton r2 = new JRadioButton("Option B");
        JRadioButton r3 = new JRadioButton("Option C");
        bg.add(r1); bg.add(r2); bg.add(r3);
        radios.add(r1); radios.add(r2); radios.add(r3);
        p.add(radios, fc);
        nextRow(lc, fc);

        // JComboBox
        p.add(label("JComboBox:"), lc);
        JPanel combos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        combos.setOpaque(false);
        combos.add(new JComboBox<>(new String[]{"Alpha", "Beta", "Gamma", "Delta"}));
        JComboBox<String> editableCbo = new JComboBox<>(new String[]{"One", "Two", "Three"});
        editableCbo.setEditable(true);
        editableCbo.setSelectedItem("Editable combo");
        combos.add(editableCbo);
        p.add(combos, fc);
        nextRow(lc, fc);

        // JSpinner
        p.add(label("JSpinner:"), lc);
        JPanel spinners = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        spinners.setOpaque(false);
        spinners.add(new JSpinner(new SpinnerNumberModel(42, 0, 100, 1)));
        spinners.add(new JSpinner(new SpinnerListModel(new String[]{"Jan","Feb","Mar","Apr","May","Jun"})));
        p.add(spinners, fc);
        nextRow(lc, fc);

        // Filler
        GridBagConstraints fill = new GridBagConstraints();
        fill.gridy = lc.gridy; fill.weighty = 1.0; fill.fill = GridBagConstraints.VERTICAL;
        p.add(Box.createVerticalGlue(), fill);

        return p;
    }

    // ---- Tab 2: Table & List ----

    private JPanel buildTableAndListTab() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // JTable
        String[] cols = {"ID", "Name", "Department", "Salary", "Status"};
        Object[][] rows = {
            {1,  "Alice Nguyen",    "Engineering",  "$120,000", "Active"},
            {2,  "Bob Martinez",    "Marketing",     "$85,000", "Active"},
            {3,  "Carol Williams",  "Finance",       "$95,000", "On Leave"},
            {4,  "David Kim",       "Engineering",  "$115,000", "Active"},
            {5,  "Eva Rossi",       "HR",            "$78,000", "Active"},
            {6,  "Frank Chen",      "Engineering",  "$130,000", "Active"},
            {7,  "Grace Patel",     "Marketing",     "$88,000", "Inactive"},
            {8,  "Henry Okafor",    "Finance",      "$102,000", "Active"},
            {9,  "Isla Schmidt",    "Engineering",   "$98,000", "Active"},
            {10, "James Larsson",   "Operations",    "$72,000", "Active"},
        };
        @SuppressWarnings("serial")
        DefaultTableModel dtm = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(dtm);
        table.setToolTipText("Employee data table");
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new TitledBorder("JTable — Employees"));

        // JList
        String[] items = {
            "Apple", "Apricot", "Banana", "Blueberry", "Cherry",
            "Coconut", "Date", "Fig", "Grape", "Guava",
            "Kiwi", "Lemon", "Lime", "Mango", "Melon",
            "Orange", "Papaya", "Peach", "Pear", "Pineapple",
        };
        JList<String> list = new JList<>(items);
        list.setSelectedIndices(new int[]{0, 2, 4});
        list.setToolTipText("Fruit selection list");
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(new TitledBorder("JList — Fruits"));

        p.add(tableScroll);
        p.add(listScroll);
        return p;
    }

    // ---- Tab 3: Text components ----

    private JPanel buildTextComponentsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        // JTextField
        p.add(label("JTextField:"), lc);
        JTextField tf = new JTextField("Hello, SwingVecPDF!", 24);
        tf.setToolTipText("A plain text field");
        p.add(tf, fc);
        nextRow(lc, fc);

        // JPasswordField
        p.add(label("JPasswordField:"), lc);
        JPasswordField pf = new JPasswordField("secret", 24);
        p.add(pf, fc);
        nextRow(lc, fc);

        // JFormattedTextField
        p.add(label("JFormattedTextField:"), lc);
        JFormattedTextField ftf = new JFormattedTextField(java.text.NumberFormat.getCurrencyInstance());
        ftf.setValue(1234.56);
        ftf.setColumns(24);
        p.add(ftf, fc);
        nextRow(lc, fc);

        // JTextArea
        p.add(label("JTextArea:"), lc);
        JTextArea ta = new JTextArea(
            "Line 1: The quick brown fox\nLine 2: jumps over the lazy dog.\nLine 3: SwingVecPDF rocks!", 4, 24);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        p.add(new JScrollPane(ta), fc);
        nextRow(lc, fc);

        // JEditorPane (HTML with hyperlinks)
        p.add(label("JEditorPane (HTML):"), lc);
        JEditorPane ep = new JEditorPane("text/html",
            "<html><body style='font-family:sans-serif;font-size:13px;'>" +
            "Visit <a href='https://pdfbox.apache.org'>Apache PDFBox</a> or " +
            "<a href='https://github.com/rototor/pdfbox-graphics2d'>pdfbox-graphics2d</a>." +
            "</body></html>");
        ep.setEditable(false);
        ep.setPreferredSize(new Dimension(400, 60));
        p.add(new JScrollPane(ep), fc);
        nextRow(lc, fc);

        // Filler
        GridBagConstraints fill = new GridBagConstraints();
        fill.gridy = lc.gridy; fill.weighty = 1.0; fill.fill = GridBagConstraints.VERTICAL;
        p.add(Box.createVerticalGlue(), fill);

        return p;
    }

    // ---- Tab 4: Gauges ----

    private JPanel buildGaugesTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints lc = labelConstraints();
        GridBagConstraints fc = fieldConstraints();

        // JProgressBar — indeterminate
        p.add(label("JProgressBar (string):"), lc);
        JProgressBar pb1 = new JProgressBar(0, 100);
        pb1.setValue(65);
        pb1.setStringPainted(true);
        pb1.setToolTipText("Task progress: 65%");
        p.add(pb1, fc);
        nextRow(lc, fc);

        // JProgressBar — no string
        p.add(label("JProgressBar (no string):"), lc);
        JProgressBar pb2 = new JProgressBar(0, 100);
        pb2.setValue(30);
        p.add(pb2, fc);
        nextRow(lc, fc);

        // JSlider — horizontal with ticks
        p.add(label("JSlider (h, ticks):"), lc);
        JSlider sl1 = new JSlider(0, 100, 45);
        sl1.setPaintTicks(true);
        sl1.setPaintLabels(true);
        sl1.setMajorTickSpacing(25);
        sl1.setMinorTickSpacing(5);
        sl1.setToolTipText("Horizontal slider");
        p.add(sl1, fc);
        nextRow(lc, fc);

        // JSlider — vertical
        p.add(label("JSlider (vertical):"), lc);
        JSlider sl2 = new JSlider(SwingConstants.VERTICAL, 0, 100, 70);
        sl2.setPaintTicks(true);
        sl2.setMajorTickSpacing(20);
        sl2.setPreferredSize(new Dimension(60, 100));
        p.add(sl2, fc);
        nextRow(lc, fc);

        // JSeparator
        p.add(label("JSeparator:"), lc);
        JSeparator sep = new JSeparator();
        sep.setPreferredSize(new Dimension(200, 4));
        p.add(sep, fc);
        nextRow(lc, fc);

        // Filler
        GridBagConstraints fill = new GridBagConstraints();
        fill.gridy = lc.gridy; fill.weighty = 1.0; fill.fill = GridBagConstraints.VERTICAL;
        p.add(Box.createVerticalGlue(), fill);

        return p;
    }

    // ---- Tab 5: Layout panels ----

    private JPanel buildLayoutPanelsTab() {
        JPanel p = new JPanel(new GridLayout(2, 2, 12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Nested JSplitPane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                colorPanel("Left pane", new Color(0xD0E8FF)),
                colorPanel("Right pane", new Color(0xD0FFD8)));
        split.setDividerLocation(120);
        JPanel splitWrap = titled(split, "JSplitPane");
        p.add(splitWrap);

        // Color swatches (opaque panels)
        JPanel swatches = new JPanel(new GridLayout(2, 4, 4, 4));
        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                          Color.CYAN, Color.BLUE, Color.MAGENTA, Color.PINK};
        for (Color c : colors) {
            JPanel swatch = new JPanel();
            swatch.setBackground(c);
            swatch.setOpaque(true);
            swatch.setToolTipText(String.format("RGB(%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue()));
            swatches.add(swatch);
        }
        p.add(titled(swatches, "Color Swatches (opaque JPanel)"));

        // JScrollPane with many labels (tests full-content rendering)
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        for (int i = 1; i <= 30; i++) {
            JLabel row = new JLabel("  Row " + i + ": the quick brown fox jumps over the lazy dog  ");
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            scrollContent.add(row);
        }
        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setPreferredSize(new Dimension(300, 120));
        p.add(titled(scrollPane, "JScrollPane (30 rows — enable Full Content)"));

        // Gradient custom-painted panel
        @SuppressWarnings("serial")
        JPanel gradPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x6A5ACD),
                        getWidth(), getHeight(), new Color(0xFF7F50));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                FontMetrics fm = g2.getFontMetrics();
                String txt = "Custom gradient fill";
                g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2,
                        (getHeight() + fm.getAscent()) / 2);
                g2.dispose();
            }
        };
        p.add(titled(gradPanel, "Custom-painted gradient JPanel"));

        return p;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(5, 4, 5, 8);
        return c;
    }

    private static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1; c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 0, 5, 4);
        return c;
    }

    private static void nextRow(GridBagConstraints lc, GridBagConstraints fc) {
        lc.gridy++;
        fc.gridy++;
    }

    private static JPanel colorPanel(String text, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setOpaque(true);
        p.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    }

    private static JPanel titled(Component inner, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(title));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * Launch the demo frame on the Event Dispatch Thread.
     *
     * <p>To run from Maven:
     * <pre>
     *   mvn test-compile exec:java \
     *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.SwingVecPdfDemo \
     *     -Dexec.classpathScope=test \
     *     -Dexec.jvmArgs="--add-opens java.desktop/javax.swing=com.seaglassfoundry.swingtopdf
     *                      --add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
     *                      --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf"
     * </pre>
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SwingVecPdfDemo().setVisible(true);
        });
    }
}
