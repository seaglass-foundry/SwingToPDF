package com.seaglassfoundry.swingtopdf.demo;

import java.awt.Adjustable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ImageIcon;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.VectorComponentHandler;

/**
 * Interactive demo frame with 14 tabs, each showcasing different Swing components.
 * Every tab has a "Generate PDF" button that exports the tab's content panel to a
 * PDF file in the user's home directory.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.ComprehensiveDemoFrame \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class ComprehensiveDemoFrame {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("swingtopdf — Comprehensive Demo");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(900, 700);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Basic Form",       tabWrapper("basic-form",       buildBasicForm()));
            tabs.addTab("Long Form",        tabWrapper("long-form",        buildLongForm()));
            tabs.addTab("Data Table",       tabWrapper("data-table",       buildDataTable()));
            tabs.addTab("Long List",        tabWrapper("long-list",        buildLongList()));
            tabs.addTab("Tree View",        tabWrapper("tree-view",        buildTreeView()));
            tabs.addTab("Rich Text",        tabWrapper("rich-text",        buildRichText()));
            tabs.addTab("HTML Labels",      tabWrapper("html-labels",      buildHtmlLabels()));
            tabs.addTab("Value Widgets",    tabWrapper("value-widgets",    buildValueWidgets()));
            tabs.addTab("Borders",          tabWrapper("borders",          buildBordersGallery()));
            tabs.addTab("Buttons",          tabWrapper("buttons",          buildButtonsPanel()));
            tabs.addTab("Split Pane",       tabWrapper("split-pane",       buildSplitPane()));
            tabs.addTab("Internal Frames",  tabWrapper("internal-frames",  buildInternalFrames()));
            tabs.addTab("Mixed UI",         tabWrapper("mixed-ui",         buildMixedUi()));
            tabs.addTab("Headers & Footers", hfTabWrapper("headers-footers", buildHeaderFooterTable()));
            tabs.addTab("AcroForm",         acroTabWrapper("acroform",     buildAcroForm()));
            tabs.addTab("Vector Handlers", vectorTabWrapper("vector-handlers", buildVectorHandlers()));

            frame.getContentPane().add(tabs);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -----------------------------------------------------------------------
    // Tab wrapper: scrollable view + Generate PDF button
    // -----------------------------------------------------------------------

    /**
     * Wraps a content panel in a BorderLayout panel that shows it in a JScrollPane
     * (for comfortable viewing) and adds a "Generate PDF" button in the SOUTH.
     *
     * @param name    file-name stem used for the output PDF
     * @param content the panel to export
     */
    private static JPanel tabWrapper(String name, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x3A7D44));
        JButton btn = new JButton("Generate PDF");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        btn.addActionListener(generateAction(name, content, status));
        south.add(status);
        south.add(btn);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    private static ActionListener generateAction(String name, JPanel content, JLabel status) {
        return e -> {
            Path out = Paths.get(System.getProperty("user.home"), name + ".pdf");
            status.setText("Exporting…");
            // Size the panel to its preferred size so pagination works correctly
            Dimension pref = content.getPreferredSize();
            content.setSize(pref.width > 0 ? pref.width : 800, pref.height > 0 ? pref.height : 600);
            content.validate();
            try {
                SwingPdfExporter.from(content)
                        .pageSize(PageSize.A4)
                        .exportMode(ExportMode.DATA_REPORT)
                        .margins(36, 36, 36, 36)
                        .export(out);
                status.setText("Saved: " + out);
                // Try to open the file
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(out.toFile());
                }
            } catch (Exception ex) {
                status.setForeground(Color.RED);
                status.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        };
    }

    // -----------------------------------------------------------------------
    // Tab 1 — Basic Form
    // -----------------------------------------------------------------------

    private static JPanel buildBasicForm() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(16));

        p.add(heading("Employee Information Form"));
        p.add(vgap(12));
        p.add(section("Personal Details"));
        p.add(formRow("First Name:",    new JTextField("Jane", 20)));
        p.add(formRow("Last Name:",     new JTextField("Doe", 20)));
        p.add(formRow("Date of Birth:", new JTextField("1985-06-15", 20)));
        p.add(formRow("Gender:",        combo("Female", "Male", "Other", "Prefer not to say")));
        p.add(formRow("Nationality:",   new JTextField("American", 20)));
        p.add(vgap(12));

        p.add(section("Contact Details"));
        p.add(formRow("Email:",         new JTextField("jane.doe@example.com", 28)));
        p.add(formRow("Phone:",         new JTextField("+1 555-0100", 20)));
        p.add(formRow("Address Line 1:", new JTextField("123 Maple Street", 28)));
        p.add(formRow("Address Line 2:", new JTextField("Apt 4B", 28)));
        p.add(formRow("City:",          new JTextField("Springfield", 20)));
        p.add(formRow("State:",         combo("Illinois", "California", "New York", "Texas", "Florida")));
        p.add(formRow("ZIP Code:",      new JTextField("62701", 10)));
        p.add(vgap(12));

        p.add(section("Employment"));
        p.add(formRow("Department:",    combo("Engineering", "Marketing", "Finance", "HR", "Operations")));
        p.add(formRow("Job Title:",     new JTextField("Senior Software Engineer", 28)));
        p.add(formRow("Start Date:",    new JTextField("2018-03-01", 20)));
        p.add(formRow("Salary (USD):",  new JTextField("125,000", 20)));
        p.add(vgap(12));

        p.add(section("Preferences"));
        JCheckBox newsletter = new JCheckBox("Subscribe to company newsletter", true);
        JCheckBox remote     = new JCheckBox("Eligible for remote work", true);
        JCheckBox travel     = new JCheckBox("Willing to travel", false);
        p.add(checkRow(newsletter, remote, travel));
        p.add(vgap(12));

        p.add(section("Notes"));
        JTextArea notes = new JTextArea(
                "Jane joined from Acme Corp and has led three major product launches. "
                + "She is currently mentoring two junior engineers.", 4, 40);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        p.add(scrollOf(notes, 700, 80));
        p.add(vgap(16));

        JPanel btns = flow(new JButton("Save"), new JButton("Reset"), disabled(new JButton("Archive")));
        p.add(btns);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 2 — Long Form (many sections — pagination test)
    // -----------------------------------------------------------------------

    private static JPanel buildLongForm() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(16));

        p.add(heading("Annual Performance Review — Full Questionnaire"));
        p.add(vgap(8));

        String[] sectionNames = {
            "Section 1: Goals and Objectives",
            "Section 2: Core Competencies",
            "Section 3: Leadership & Teamwork",
            "Section 4: Technical Skills",
            "Section 5: Communication",
            "Section 6: Customer Focus",
            "Section 7: Innovation",
            "Section 8: Professional Development",
            "Section 9: Self-Evaluation",
            "Section 10: Manager Evaluation",
            "Section 11: Peer Feedback",
            "Section 12: Action Plan"
        };

        String[] questions = {
            "Rate your performance this year (1-5):",
            "Describe key achievements:",
            "Areas requiring improvement:",
            "Goals achieved vs. set targets:",
            "Training completed this period:",
            "Comments / additional notes:"
        };

        for (String sec : sectionNames) {
            p.add(section(sec));
            for (String q : questions) {
                if (q.startsWith("Rate")) {
                    p.add(formRow(q, combo("1 - Unsatisfactory", "2 - Needs Improvement",
                            "3 - Meets Expectations", "4 - Exceeds Expectations", "5 - Outstanding")));
                } else {
                    JTextArea ta = new JTextArea(3, 40);
                    ta.setLineWrap(true);
                    ta.setWrapStyleWord(true);
                    p.add(formRow(q, scrollOf(ta, 500, 60)));
                }
            }
            p.add(vgap(10));
        }

        p.add(section("Signatures"));
        p.add(formRow("Employee Signature:", new JTextField("Jane Doe", 30)));
        p.add(formRow("Manager Signature:",  new JTextField("John Smith", 30)));
        p.add(formRow("HR Representative:",  new JTextField("Emily Johnson", 30)));
        p.add(formRow("Date:",               new JTextField("2026-03-13", 20)));
        p.add(vgap(16));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 3 — Data Table (100 rows)
    // -----------------------------------------------------------------------

    private static JPanel buildDataTable() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Sales Data — Q1 2026 (100 rows)"));
        p.add(vgap(8));

        String[] cols = {"#", "Sales Rep", "Region", "Product", "Units", "Revenue", "Status"};
        String[] reps = {"Alice N.", "Bob M.", "Carol W.", "David K.", "Eva R.", "Frank C.", "Grace P.", "Henry O."};
        String[] regions = {"North", "South", "East", "West", "Central"};
        String[] products = {"Widget Pro", "Gadget Plus", "Device Lite", "Platform X", "Suite 360"};
        String[] statuses = {"Closed Won", "Closed Lost", "In Progress", "Pending", "Qualified"};
        Random rng = new Random(42);
        Object[][] data = new Object[100][7];
        for (int i = 0; i < 100; i++) {
            int units   = 10 + rng.nextInt(490);
            double rev  = units * (49.99 + rng.nextDouble() * 450.0);
            data[i] = new Object[]{
                i + 1,
                reps[rng.nextInt(reps.length)],
                regions[rng.nextInt(regions.length)],
                products[rng.nextInt(products.length)],
                units,
                String.format("$%,.0f", rev),
                statuses[rng.nextInt(statuses.length)]
            };
        }
        JTable table = new JTable(new DefaultTableModel(data, cols) {
            private static final long serialVersionUID = 8269613896873106237L;

			@Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setRowHeight(20);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);

        // Table expands to show all rows for DATA_REPORT mode
        JScrollPane sp = new JScrollPane(table);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(860, 2060));
        p.add(sp);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 4 — Long List (200 items)
    // -----------------------------------------------------------------------

    private static JPanel buildLongList() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Product Catalogue — 200 Items"));
        p.add(vgap(8));

        String[] categories = {"Electronics", "Clothing", "Books", "Home & Garden", "Sports", "Toys", "Food", "Beauty"};
        String[] adjectives = {"Pro", "Lite", "Max", "Ultra", "Basic", "Advanced", "Compact", "Deluxe"};
        String[] nouns      = {"Widget", "Gadget", "Device", "Module", "Unit", "System", "Kit", "Pack"};
        Random rng = new Random(7);
        String[] items = new String[200];
        for (int i = 0; i < 200; i++) {
            String cat  = categories[rng.nextInt(categories.length)];
            String adj  = adjectives[rng.nextInt(adjectives.length)];
            String noun = nouns[rng.nextInt(nouns.length)];
            items[i] = String.format("%03d  %-12s  %s %s  —  $%.2f", i + 1, cat, adj, noun,
                    9.99 + rng.nextDouble() * 990.0);
        }
        JList<String> list = new JList<>(items);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        list.setFixedCellHeight(18);
        list.setSelectionBackground(new Color(0xB3D4FF));

        JScrollPane sp = new JScrollPane(list);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(840, 3700));
        p.add(sp);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 5 — Tree View
    // -----------------------------------------------------------------------

    private static JPanel buildTreeView() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Organisation Chart — Full Hierarchy"));
        p.add(vgap(8));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Acme Corporation");
        String[][] divisions = {
            {"Engineering",    "Backend Team", "Frontend Team", "DevOps", "QA", "Security"},
            {"Marketing",      "Brand", "Digital", "Events", "Research"},
            {"Finance",        "Accounting", "Payroll", "Audit", "Investments"},
            {"Operations",     "Logistics", "Facilities", "Procurement", "Supply Chain"},
            {"Human Resources","Recruiting", "Training", "Benefits", "Compliance"},
            {"Legal",          "Corporate", "IP & Patents", "Contracts", "Litigation"},
            {"Sales",          "EMEA", "Americas", "APAC", "Partner Channel"},
            {"Product",        "Strategy", "Design", "Research", "Analytics"}
        };
        String[] memberTpl = {"Alice %s", "Bob %s", "Carol %s", "David %s", "Eve %s"};
        Random rng = new Random(3);

        for (String[] div : divisions) {
            DefaultMutableTreeNode divNode = new DefaultMutableTreeNode(div[0]);
            root.add(divNode);
            for (int t = 1; t < div.length; t++) {
                DefaultMutableTreeNode team = new DefaultMutableTreeNode(div[t]);
                divNode.add(team);
                int members = 3 + rng.nextInt(4);
                for (int m = 0; m < members; m++) {
                    String name = String.format(memberTpl[m % memberTpl.length], div[t].split(" ")[0]);
                    team.add(new DefaultMutableTreeNode(name));
                }
            }
        }

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRowHeight(18);
        // Expand all nodes
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        tree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JScrollPane sp = new JScrollPane(tree);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(840, 1000));
        p.add(sp);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 6 — Rich Text (JTextPane)
    // -----------------------------------------------------------------------

    private static JPanel buildRichText() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Styled Document — JTextPane"));
        p.add(vgap(8));

        JTextPane pane = new JTextPane();
        pane.setBackground(Color.WHITE);
        StyledDocument doc = pane.getStyledDocument();

        Style def   = doc.getStyle(StyleContext.DEFAULT_STYLE);
        Style body  = addStyle(doc, "body",   def,  "SansSerif", 13, false, false, false, false, Color.BLACK);
        Style h1    = addStyle(doc, "h1",     body, "SansSerif", 20, true,  false, false, false, new Color(0x1A237E));
        Style h2    = addStyle(doc, "h2",     body, "SansSerif", 15, true,  false, false, false, new Color(0x283593));
        Style bold  = addStyle(doc, "bold",   body, null,        -1, true,  false, false, false, null);
        Style ital  = addStyle(doc, "italic", body, null,        -1, false, true,  false, false, null);
        Style under = addStyle(doc, "under",  body, null,        -1, false, false, true,  false, null);
        Style strike= addStyle(doc, "strike", body, null,        -1, false, false, false, true,  null);
        Style red   = addStyle(doc, "red",    body, null,        -1, false, false, false, false, Color.RED);
        Style green = addStyle(doc, "green",  body, null,        -1, false, false, false, false, new Color(0x2E7D32));
        Style code  = addStyle(doc, "code",   body, "Monospaced", 12, false, false, false, false, new Color(0x7B1FA2));

        try {
            doc.insertString(doc.getLength(), "Comprehensive Typography Test\n\n", h1);
            doc.insertString(doc.getLength(), "1. Font Weights and Styles\n", h2);
            doc.insertString(doc.getLength(), "This is ", body);
            doc.insertString(doc.getLength(), "bold text", bold);
            doc.insertString(doc.getLength(), ", this is ", body);
            doc.insertString(doc.getLength(), "italic text", ital);
            doc.insertString(doc.getLength(), ", this is ", body);
            doc.insertString(doc.getLength(), "underlined text", under);
            doc.insertString(doc.getLength(), ", and this is ", body);
            doc.insertString(doc.getLength(), "strikethrough text", strike);
            doc.insertString(doc.getLength(), ".\n\n", body);

            doc.insertString(doc.getLength(), "2. Colour Variations\n", h2);
            doc.insertString(doc.getLength(), "Normal text followed by ", body);
            doc.insertString(doc.getLength(), "red text", red);
            doc.insertString(doc.getLength(), " and then ", body);
            doc.insertString(doc.getLength(), "green text", green);
            doc.insertString(doc.getLength(), " back to normal.\n\n", body);

            doc.insertString(doc.getLength(), "3. Inline Code\n", h2);
            doc.insertString(doc.getLength(), "Call the method ", body);
            doc.insertString(doc.getLength(), "SwingPdfExporter.from(panel).export(path)", code);
            doc.insertString(doc.getLength(), " to render a PDF.\n\n", body);

            doc.insertString(doc.getLength(), "4. Lorem Ipsum (wrapping test)\n", h2);
            String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
                    + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
                    + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in "
                    + "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
                    + "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
                    + "culpa qui officia deserunt mollit anim id est laborum.\n\n";
            doc.insertString(doc.getLength(), lorem, body);

            doc.insertString(doc.getLength(), "5. Mixed Styles in a Sentence\n", h2);
            doc.insertString(doc.getLength(), "The project is ", body);
            doc.insertString(doc.getLength(), "critically important", bold);
            doc.insertString(doc.getLength(), " and must be delivered ", body);
            doc.insertString(doc.getLength(), "on time", under);
            doc.insertString(doc.getLength(), "; any ", body);
            doc.insertString(doc.getLength(), "delays", strike);
            doc.insertString(doc.getLength(), " rescheduling will incur ", body);
            doc.insertString(doc.getLength(), "significant penalties", red);
            doc.insertString(doc.getLength(), ".\n\n", body);

            doc.insertString(doc.getLength(), "6. Large Block of Text\n", h2);
            for (int i = 1; i <= 20; i++) {
                doc.insertString(doc.getLength(),
                        "Paragraph " + i + ": The quick brown fox jumps over the lazy dog. "
                        + "Pack my box with five dozen liquor jugs. "
                        + "How vexingly quick daft zebras jump!\n", body);
            }
        } catch (BadLocationException ignored) {}

        pane.setPreferredSize(new Dimension(820, 1200));
        JScrollPane sp = new JScrollPane(pane);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(840, 500));
        p.add(sp);
        p.add(vgap(8));
        return p;
    }

    private static Style addStyle(StyledDocument doc, String name, Style parent,
                                   String family, int size, boolean bold, boolean italic,
                                   boolean under, boolean strike, Color fg) {
        Style s = doc.addStyle(name, parent);
        if (family != null) StyleConstants.setFontFamily(s, family);
        if (size > 0)       StyleConstants.setFontSize(s, size);
        StyleConstants.setBold(s, bold);
        StyleConstants.setItalic(s, italic);
        StyleConstants.setUnderline(s, under);
        StyleConstants.setStrikeThrough(s, strike);
        if (fg != null)     StyleConstants.setForeground(s, fg);
        return s;
    }

    // -----------------------------------------------------------------------
    // Tab 7 — HTML Labels
    // -----------------------------------------------------------------------

    private static JPanel buildHtmlLabels() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("HTML-Formatted JLabel Showcase"));
        p.add(vgap(8));
        p.add(section("Basic Formatting"));

        String[] htmlSamples = {
            "<html><b>Bold</b>, <i>Italic</i>, <u>Underline</u>, <s>Strike</s></html>",
            "<html><font color='#C62828'>Red text</font> and <font color='#1565C0'>blue text</font></html>",
            "<html><font size='+2'>Large</font> and <font size='-1'>small</font> font sizes</html>",
            "<html>Line one<br>Line two<br>Line three (multi-line label)</html>",
            "<html><b>Name:</b> Jane Doe &nbsp;&nbsp; <b>Role:</b> Engineer &nbsp;&nbsp; <b>Level:</b> Senior</html>",
            "<html><ul><li>Feature A</li><li>Feature B</li><li>Feature C</li></ul></html>",
            "<html><table border='0'><tr><td><b>Q1</b></td><td>$1.2M</td></tr>"
                + "<tr><td><b>Q2</b></td><td>$1.5M</td></tr></table></html>",
            "<html><center><b>Centred heading</b><br><i>Subtitle text here</i></center></html>",
        };

        for (String html : htmlSamples) {
            JLabel lbl = new JLabel(html);
            lbl.setBackground(new Color(0xFAFAFA));
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xDDDDDD), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(lbl);
            p.add(vgap(6));
        }

        p.add(vgap(8));
        p.add(section("JEditorPane with HTML"));
        JEditorPane editor = new JEditorPane();
        editor.setEditorKit(new HTMLEditorKit());
        editor.setEditable(false);
        editor.setText("<html><body style='font-family:sans-serif;padding:8px'>"
                + "<h2 style='color:#1A237E'>Project Status Report</h2>"
                + "<p>This report summarises the current state of all active projects.</p>"
                + "<table border='1' cellpadding='4' cellspacing='0' width='100%'>"
                + "<tr style='background:#E3F2FD'><th>Project</th><th>Status</th><th>Owner</th><th>Due Date</th></tr>"
                + "<tr><td>Alpha</td><td><font color='green'>On Track</font></td><td>Alice</td><td>2026-04-01</td></tr>"
                + "<tr><td>Beta</td><td><font color='orange'>At Risk</font></td><td>Bob</td><td>2026-05-15</td></tr>"
                + "<tr><td>Gamma</td><td><font color='red'>Delayed</font></td><td>Carol</td><td>2026-03-01</td></tr>"
                + "<tr><td>Delta</td><td><font color='green'>Complete</font></td><td>David</td><td>2026-02-28</td></tr>"
                + "</table>"
                + "<p><i>Last updated: 2026-03-13</i></p>"
                + "</body></html>");
        editor.setBackground(Color.WHITE);
        editor.setPreferredSize(new Dimension(820, 260));
        JScrollPane sp = new JScrollPane(editor);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(840, 280));
        p.add(sp);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 8 — Value Widgets (progress, slider, spinner, scrollbar)
    // -----------------------------------------------------------------------

    private static JPanel buildValueWidgets() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Value Widgets"));
        p.add(vgap(8));

        // --- JProgressBar ---
        p.add(section("JProgressBar"));
        int[] vals = {0, 15, 42, 68, 90, 100};
        for (int v : vals) {
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setValue(v);
            pb.setStringPainted(true);
            pb.setString(v + "%");
            pb.setPreferredSize(new Dimension(500, 22));
            p.add(formRow(v + "%:", pb));
        }
        JProgressBar indeterminate = new JProgressBar();
        indeterminate.setIndeterminate(true);
        indeterminate.setStringPainted(true);
        indeterminate.setString("Loading…");
        indeterminate.setPreferredSize(new Dimension(500, 22));
        p.add(formRow("Indeterminate:", indeterminate));
        p.add(vgap(12));

        // --- JSlider ---
        p.add(section("JSlider"));
        JSlider slH1 = new JSlider(0, 100, 35);
        slH1.setPaintTicks(true); slH1.setPaintLabels(true);
        slH1.setMajorTickSpacing(25); slH1.setMinorTickSpacing(5);
        p.add(formRow("Horizontal 35%:", slH1));

        JSlider slH2 = new JSlider(0, 100, 75);
        slH2.setPaintTicks(true);
        p.add(formRow("No labels 75%:", slH2));

        JSlider slSnap = new JSlider(0, 10, 7);
        slSnap.setSnapToTicks(true); slSnap.setPaintTicks(true); slSnap.setPaintLabels(true);
        slSnap.setMajorTickSpacing(1);
        p.add(formRow("Snap-to-tick 7:", slSnap));
        p.add(vgap(12));

        // --- JSpinner ---
        p.add(section("JSpinner"));
        p.add(formRow("Integer (42):",    new JSpinner(new SpinnerNumberModel(42, 0, 1000, 1))));
        p.add(formRow("Double (3.14):",   new JSpinner(new SpinnerNumberModel(3.14, 0.0, 100.0, 0.01))));
        p.add(formRow("List:",            new JSpinner(new SpinnerListModel(
                new String[]{"Alpha", "Beta", "Gamma", "Delta"}))));
        p.add(formRow("Date:",            new JSpinner(new SpinnerDateModel())));
        p.add(vgap(12));

        // --- JScrollBar ---
        p.add(section("JScrollBar"));
        JScrollBar sbH = new JScrollBar(Adjustable.HORIZONTAL, 30, 20, 0, 100);
        sbH.setPreferredSize(new Dimension(400, 20));
        p.add(formRow("Horizontal:", sbH));

        JPanel vbars = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        vbars.setOpaque(false);
        vbars.setAlignmentX(Component.LEFT_ALIGNMENT);
        int[] thumbVals = {0, 25, 50, 75, 100};
        for (int v : thumbVals) {
            JScrollBar sb = new JScrollBar(Adjustable.VERTICAL, v, 15, 0, 115);
            sb.setPreferredSize(new Dimension(18, 120));
            vbars.add(sb);
        }
        p.add(vbars);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 9 — Borders Gallery
    // -----------------------------------------------------------------------

    private static JPanel buildBordersGallery() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Border Types — Showcase"));
        p.add(vgap(8));

        // Each row: label + a panel using that border
        Object[][] borders = {
            {"LineBorder (1px black)",     BorderFactory.createLineBorder(Color.BLACK, 1)},
            {"LineBorder (3px blue)",       BorderFactory.createLineBorder(Color.BLUE, 3)},
            {"EtchedBorder (LOWERED)",      BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)},
            {"EtchedBorder (RAISED)",       BorderFactory.createEtchedBorder(EtchedBorder.RAISED)},
            {"BevelBorder (LOWERED)",       BorderFactory.createLoweredBevelBorder()},
            {"BevelBorder (RAISED)",        BorderFactory.createRaisedBevelBorder()},
            {"MatteBorder (8/4/8/4 red)",   BorderFactory.createMatteBorder(8, 4, 8, 4, Color.RED)},
            {"MatteBorder (2/2/2/2 green)", BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(0x388E3C))},
            {"TitledBorder (default inner)",BorderFactory.createTitledBorder("Section Title")},
            {"TitledBorder (line border)",  BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY, 2), "Titled Line")},
            {"TitledBorder (etched inner)", BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(), "Titled Etched")},
            {"CompoundBorder",              BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                    BorderFactory.createEmptyBorder(6, 6, 6, 6))},
            {"EmptyBorder (8px)",           BorderFactory.createEmptyBorder(8, 8, 8, 8)},
        };

        for (Object[] row : borders) {
            String labelText = (String) row[0];
            Border border    = (Border) row[1];
            JPanel box = new JPanel(new FlowLayout(FlowLayout.LEFT));
            box.setBackground(new Color(0xFAFAFA));
            box.setOpaque(true);
            box.setBorder(border);
            box.add(new JLabel(labelText));
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(box);
            p.add(vgap(6));
        }
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 10 — Buttons & Controls
    // -----------------------------------------------------------------------

    private static JPanel buildButtonsPanel() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Buttons & Controls"));
        p.add(vgap(8));

        p.add(section("JButton variants"));
        p.add(flow(
                new JButton("Default"),
                makeBtn("Primary", new Color(0x1565C0), Color.WHITE),
                makeBtn("Danger",  new Color(0xC62828), Color.WHITE),
                makeBtn("Success", new Color(0x2E7D32), Color.WHITE),
                disabled(new JButton("Disabled"))
        ));
        p.add(vgap(8));

        p.add(section("JToggleButton"));
        p.add(flow(
                new JToggleButton("Off", false),
                new JToggleButton("On",  true),
                new JToggleButton("Mixed")
        ));
        p.add(vgap(8));

        p.add(section("JCheckBox"));
        p.add(flow(
                new JCheckBox("Unchecked", false),
                new JCheckBox("Checked",   true),
                disabled(new JCheckBox("Disabled unchecked", false)),
                disabled(new JCheckBox("Disabled checked",   true))
        ));
        p.add(vgap(8));

        p.add(section("JRadioButton groups"));
        ButtonGroup bg1 = new ButtonGroup();
        JRadioButton r1a = new JRadioButton("Option A", true);
        JRadioButton r1b = new JRadioButton("Option B");
        JRadioButton r1c = new JRadioButton("Option C");
        bg1.add(r1a); bg1.add(r1b); bg1.add(r1c);
        p.add(flow(r1a, r1b, r1c));

        ButtonGroup bg2 = new ButtonGroup();
        JRadioButton r2a = new JRadioButton("Small");
        JRadioButton r2b = new JRadioButton("Medium", true);
        JRadioButton r2c = new JRadioButton("Large");
        JRadioButton r2d = disabled(new JRadioButton("Custom"));
        bg2.add(r2a); bg2.add(r2b); bg2.add(r2c); bg2.add(r2d);
        p.add(flow(r2a, r2b, r2c, r2d));
        p.add(vgap(8));

        p.add(section("JComboBox"));
        JComboBox<String> editable = new JComboBox<>(new String[]{"Java", "Kotlin", "Scala", "Groovy"});
        editable.setSelectedItem("Kotlin");
        editable.setEditable(true);
        JComboBox<String> readOnly = new JComboBox<>(new String[]{"PDF", "Word", "Excel", "HTML"});
        readOnly.setSelectedIndex(0);
        p.add(flow(
                labeled2("Editable:", editable),
                labeled2("Read-only:", readOnly)
        ));
        p.add(vgap(8));

        p.add(section("JTextField variants"));
        JTextField normal   = new JTextField("Normal text field", 22);
        JTextField disabled = new JTextField("Disabled field", 22); disabled.setEnabled(false);
        JTextField readonlyTf = new JTextField("Read-only field", 22); readonlyTf.setEditable(false);
        JPasswordField pwd  = new JPasswordField("s3cr3t!", 22);
        p.add(flow(normal, disabled, readonlyTf));
        p.add(formRow("Password:", pwd));
        p.add(vgap(8));

        p.add(section("JTextArea"));
        JTextArea ta = new JTextArea(
                "Multi-line text area.\nLine two of content.\nLine three.", 4, 40);
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        p.add(scrollOf(ta, 600, 80));
        p.add(vgap(8));

        p.add(section("JLabel alignment matrix"));
        JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] haligns = {"LEFT", "CENTER", "RIGHT"};
        String[] valigns = {"TOP", "CENTER", "BOTTOM"};
        int[] hconsts = {SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT};
        int[] vconsts = {SwingConstants.TOP,  SwingConstants.CENTER, SwingConstants.BOTTOM};
        for (int v = 0; v < 3; v++) {
            for (int h = 0; h < 3; h++) {
                JLabel lbl = new JLabel(haligns[h] + "/" + valigns[v]);
                lbl.setHorizontalAlignment(hconsts[h]);
                lbl.setVerticalAlignment(vconsts[v]);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                lbl.setPreferredSize(new Dimension(160, 40));
                grid.add(lbl);
            }
        }
        p.add(grid);
        p.add(vgap(8));
        return p;
    }

    private static JButton makeBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        return b;
    }

    // -----------------------------------------------------------------------
    // Tab 11 — Split Pane
    // -----------------------------------------------------------------------

    private static JPanel buildSplitPane() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("JSplitPane Layouts"));
        p.add(vgap(8));

        p.add(section("Horizontal Split — Navigator + Detail"));
        JSplitPane hSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildNavPanel(), buildDetailPanel());
        hSplit.setDividerLocation(200);
        hSplit.setPreferredSize(new Dimension(820, 280));
        hSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(hSplit);
        p.add(vgap(16));

        p.add(section("Vertical Split — Header + Content"));
        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildHeaderPanel(), buildContentAreaPanel());
        vSplit.setDividerLocation(80);
        vSplit.setPreferredSize(new Dimension(820, 300));
        vSplit.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(vSplit);
        p.add(vgap(16));

        p.add(section("Nested Split Panes"));
        JPanel leftContent  = coloured("Left",   new Color(0xE3F2FD));
        JPanel midContent   = coloured("Middle", new Color(0xE8F5E9));
        JPanel rightContent = coloured("Right",  new Color(0xFFF3E0));
        JSplitPane inner = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, midContent, rightContent);
        inner.setDividerLocation(270);
        JSplitPane outer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContent, inner);
        outer.setDividerLocation(200);
        outer.setPreferredSize(new Dimension(820, 180));
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(outer);
        p.add(vgap(8));
        return p;
    }

    private static JPanel buildNavPanel() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(new Color(0xF5F5F5));
        nav.setBorder(BorderFactory.createTitledBorder("Navigator"));
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Projects");
        root.add(new DefaultMutableTreeNode("Alpha"));
        root.add(new DefaultMutableTreeNode("Beta"));
        root.add(new DefaultMutableTreeNode("Gamma"));
        JTree tree = new JTree(root);
        tree.setRowHeight(18);
        nav.add(new JScrollPane(tree), BorderLayout.CENTER);
        return nav;
    }

    private static JPanel buildDetailPanel() {
        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBackground(Color.WHITE);
        detail.setBorder(BorderFactory.createTitledBorder("Details"));
        detail.add(new JLabel("Project: Alpha"));
        detail.add(vgap(4));
        detail.add(new JLabel("Status: In Progress"));
        detail.add(vgap(4));
        detail.add(new JLabel("Owner: Alice"));
        JTextArea desc = new JTextArea("Project Alpha is the flagship initiative for 2026.", 3, 30);
        desc.setLineWrap(true); desc.setWrapStyleWord(true); desc.setEditable(false);
        detail.add(new JScrollPane(desc));
        return detail;
    }

    private static JPanel buildHeaderPanel() {
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        hdr.setBackground(new Color(0x1A237E));
        hdr.setOpaque(true);
        JLabel title = new JLabel("swing2pdf Content Viewer");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        hdr.add(title);
        hdr.add(new JButton("File"));
        hdr.add(new JButton("Edit"));
        hdr.add(new JButton("View"));
        return hdr;
    }

    private static JPanel buildContentAreaPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        JTextArea area = new JTextArea(
                "Main content area.\nThis panel sits in the lower half of the vertical split.\n"
                + "It can contain any Swing component.");
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        area.setEditable(false);
        content.add(new JScrollPane(area), BorderLayout.CENTER);
        return content;
    }

    private static JPanel coloured(String text, Color bg) {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(bg);
        panel.setOpaque(true);
        panel.add(new JLabel(text));
        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 12 — Internal Frames
    // -----------------------------------------------------------------------

    private static JPanel buildInternalFrames() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("JInternalFrame / JDesktopPane"));
        p.add(vgap(8));

        JDesktopPane desktop = new JDesktopPane();
        desktop.setBackground(new Color(0x607D8B));
        desktop.setOpaque(true);
        desktop.setAlignmentX(Component.LEFT_ALIGNMENT);
        desktop.setPreferredSize(new Dimension(840, 520));
        desktop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        // Frame 1 — form
        JInternalFrame f1 = new JInternalFrame("Customer Details", true, true, true, true);
        f1.setSize(350, 220);
        f1.setLocation(20, 20);
        JPanel f1Content = new JPanel();
        f1Content.setLayout(new BoxLayout(f1Content, BoxLayout.Y_AXIS));
        f1Content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        f1Content.add(formRow("Name:",  new JTextField("Jane Doe", 18)));
        f1Content.add(formRow("Email:", new JTextField("jane@example.com", 18)));
        f1Content.add(formRow("Plan:",  combo("Basic", "Pro", "Enterprise")));
        f1.getContentPane().add(f1Content);
        f1.setVisible(true);
        desktop.add(f1);

        // Frame 2 — list
        JInternalFrame f2 = new JInternalFrame("Recent Orders", true, false, false, true);
        f2.setSize(300, 180);
        f2.setLocation(390, 20);
        JList<String> orders = new JList<>(new String[]{
            "#1001 — Widget Pro × 3", "#1002 — Gadget Lite × 1",
            "#1003 — Device Max × 2", "#1004 — Suite 360 × 1",
            "#1005 — Platform X × 5"
        });
        orders.setFixedCellHeight(20);
        f2.getContentPane().add(new JScrollPane(orders));
        f2.setVisible(true);
        desktop.add(f2);

        // Frame 3 — chart placeholder
        JInternalFrame f3 = new JInternalFrame("Revenue Chart", true, true, false, true);
        f3.setSize(380, 220);
        f3.setLocation(20, 270);
        JPanel chart = new JPanel() {
            private static final long serialVersionUID = -4405885276899711988L;

			@Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int[] months = {120, 145, 132, 178, 165, 190};
                int barW = 40, gap = 10, baseY = getHeight() - 30;
                g2.setColor(new Color(0x1565C0));
                for (int i = 0; i < months.length; i++) {
                    int bh = months[i];
                    g2.fillRect(20 + i * (barW + gap), baseY - bh, barW, bh);
                }
                g2.setColor(Color.BLACK);
                g2.drawLine(10, baseY, getWidth() - 10, baseY);
            }
        };
        chart.setBackground(Color.WHITE);
        f3.getContentPane().add(chart);
        f3.setVisible(true);
        desktop.add(f3);

        // Frame 4 — notes (no title bar controls)
        JInternalFrame f4 = new JInternalFrame("Notes", false, false, false, false);
        f4.setSize(200, 200);
        f4.setLocation(420, 270);
        JTextArea notes = new JTextArea("Quick note:\nMeeting at 3pm.\nBring the report.");
        notes.setLineWrap(true);
        f4.getContentPane().add(new JScrollPane(notes));
        f4.setVisible(true);
        desktop.add(f4);

        p.add(desktop);
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 13 — Mixed / Complex UI
    // -----------------------------------------------------------------------

    private static JPanel buildMixedUi() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(12));

        p.add(heading("Mixed Component Showcase"));
        p.add(vgap(8));

        // Dashboard-style metric cards
        p.add(section("Dashboard Metrics"));
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[][] metrics = {{"Revenue", "$2.4M", "+12%"}, {"Users", "18,432", "+5%"},
                               {"Orders", "3,201", "+8%"},  {"Issues", "14", "-3%"}};
        for (String[] m : metrics) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setOpaque(true);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xBBBBBB), 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            JLabel title = new JLabel(m[0]);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 11f));
            title.setForeground(new Color(0x757575));
            JLabel value = new JLabel(m[1]);
            value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
            JLabel change = new JLabel(m[2]);
            change.setFont(change.getFont().deriveFont(Font.PLAIN, 11f));
            change.setForeground(m[2].startsWith("+") ? new Color(0x2E7D32) : new Color(0xC62828));
            card.add(title);
            card.add(value);
            card.add(change);
            cards.add(card);
        }
        p.add(cards);
        p.add(vgap(12));

        // Status table with coloured cells
        p.add(section("System Status Table"));
        String[] cols = {"Service", "Status", "Uptime", "Last Check", "Owner"};
        Object[][] rows = {
            {"Auth Service",    "HEALTHY",    "99.98%", "2026-03-13 08:00", "Platform Team"},
            {"Payment Gateway", "DEGRADED",   "98.10%", "2026-03-13 07:55", "Payments Team"},
            {"Email Worker",    "HEALTHY",    "100%",   "2026-03-13 08:00", "Infra Team"},
            {"Report Builder",  "OUTAGE",     "87.50%", "2026-03-13 07:30", "Analytics Team"},
            {"API Gateway",     "HEALTHY",    "99.99%", "2026-03-13 08:00", "Platform Team"},
            {"CDN",             "HEALTHY",    "100%",   "2026-03-13 08:00", "Infra Team"},
            {"DB Primary",      "HEALTHY",    "99.95%", "2026-03-13 08:00", "Database Team"},
            {"DB Replica",      "DEGRADED",   "97.30%", "2026-03-13 07:45", "Database Team"},
        };
        JTable statusTable = new JTable(new DefaultTableModel(rows, cols) {
            private static final long serialVersionUID = 2149894322307021818L;

			@Override public boolean isCellEditable(int r, int c) { return false; }
        }) {
            private static final long serialVersionUID = -6795933314721935985L;

			@Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (col == 1) {
                    String v = (String) getValueAt(row, col);
                    c.setForeground("HEALTHY".equals(v) ? new Color(0x2E7D32)
                            : "DEGRADED".equals(v) ? new Color(0xE65100) : Color.RED);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        };
        statusTable.setRowHeight(22);
        JScrollPane tblSp = new JScrollPane(statusTable);
        tblSp.setAlignmentX(Component.LEFT_ALIGNMENT);
        tblSp.setPreferredSize(new Dimension(820, 196));
        p.add(tblSp);
        p.add(vgap(12));

        // Mixed-layout search + filter row
        p.add(section("Filter & Search Bar"));
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBar.add(new JLabel("Filter:"));
        filterBar.add(new JTextField("search term", 18));
        filterBar.add(combo("All Regions", "North", "South", "East", "West"));
        filterBar.add(combo("All Status", "Active", "Inactive", "Pending"));
        filterBar.add(new JButton("Search"));
        filterBar.add(new JButton("Clear"));
        p.add(filterBar);
        p.add(vgap(12));

        // Bordered sections side-by-side
        p.add(section("Side-by-Side Bordered Panels"));
        JPanel sideBySide = new JPanel(new GridLayout(1, 2, 12, 0));
        sideBySide.setOpaque(false);
        sideBySide.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Action Items"));
        leftPanel.setBackground(Color.WHITE);
        String[] actions = {"Review Q1 budget", "Update roadmap", "Hire backend engineer",
                "Schedule retro", "Write release notes", "Fix critical bug #4201"};
        for (String a : actions) {
            JCheckBox cb = new JCheckBox(a, a.startsWith("Review") || a.startsWith("Fix"));
            leftPanel.add(cb);
        }

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x1565C0), 2), "Key Decisions"));
        rightPanel.setBackground(Color.WHITE);
        String[] decisions = {
            "Migrate to Java 21 LTS",
            "Adopt trunk-based dev",
            "Sunset legacy API v1",
            "Enable 2FA for all staff",
            "Move CI to GitHub Actions"
        };
        for (String d : decisions) {
            JLabel lbl = new JLabel("• " + d);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
            rightPanel.add(lbl);
            rightPanel.add(vgap(4));
        }

        sideBySide.add(leftPanel);
        sideBySide.add(rightPanel);
        p.add(sideBySide);
        p.add(vgap(12));

        // Progress overview
        p.add(section("Project Progress Overview"));
        String[][] projects = {
            {"Alpha — Backend API",      "82"},
            {"Beta — Mobile App",        "47"},
            {"Gamma — Data Pipeline",    "100"},
            {"Delta — Admin Portal",     "30"},
            {"Epsilon — Auth Rewrite",   "65"},
        };
        for (String[] proj : projects) {
            int pct = Integer.parseInt(proj[1]);
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setValue(pct);
            pb.setStringPainted(true);
            pb.setString(proj[0] + "  " + pct + "%");
            pb.setPreferredSize(new Dimension(700, 22));
            pb.setForeground(pct == 100 ? new Color(0x2E7D32) : pct < 40 ? new Color(0xC62828) : new Color(0x1565C0));
            p.add(formRow("", pb));
        }
        p.add(vgap(8));
        return p;
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private static JPanel white() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setOpaque(true);
        return p;
    }

    private static Border pad(int px) {
        return BorderFactory.createEmptyBorder(px, px, px, px);
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(new Color(0x1A237E));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel section(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBackground(new Color(0xE8EEF8));
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setForeground(new Color(0x2B4C7E));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static JPanel formRow(String labelText, Component comp) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        if (!labelText.isEmpty()) {
            JLabel l = new JLabel(labelText);
            l.setPreferredSize(new Dimension(160, 26));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(l, BorderLayout.WEST);
        }
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private static JPanel checkRow(JCheckBox... boxes) {
        JPanel row = flow(boxes);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel flow(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : comps) p.add(c);
        return p;
    }

    private static JComboBox<String> combo(String... items) {
        return new JComboBox<>(items);
    }

    private static JScrollPane scrollOf(JComponent c, int w, int h) {
        JScrollPane sp = new JScrollPane(c);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(w, h));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return sp;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }

    private static <T extends AbstractButton> T disabled(T btn) {
        btn.setEnabled(false);
        return btn;
    }

    /** Compact label+component pair for inline use in flow layouts. */
    private static JPanel labeled2(String labelText, Component comp) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.add(new JLabel(labelText));
        row.add(comp);
        return row;
    }

    // -----------------------------------------------------------------------
    // Tab — Headers & Footers (multi-page table with colored background)
    // -----------------------------------------------------------------------

    /**
     * Header/Footer showcase tab. A preset dropdown drives which
     * {@link HeaderFooter} configuration is used on export; the code snippet
     * panel shows the equivalent Java so users can copy it.
     *
     * <p>Each preset exercises a different capability: plain text, text
     * wrapping, HTML inline styling, HTML multi-run formatting, HTML wrapping,
     * JLabel-with-HTML, JLabel-with-icon, composite JPanel footer, right
     * alignment, and tall explicit-height bands.
     */
    private static JPanel hfTabWrapper(String name, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // --- Preset selector & code preview (NORTH) --------------------------
        String[] presetKeys = {
                "plain-text", "plain-text-wrap", "colored-text", "background-fill",
                "html-inline", "html-multi-run", "html-wrap",
                "jlabel-html", "jlabel-icon", "jpanel-footer",
                "right-aligned", "tall-header",
                "demonstration-banner", "demonstration-banner-html",
                "demonstration-banner-per-page"
        };
        String[] presetLabels = {
                "1. Plain text (baseline)",
                "2. Plain text + wrap",
                "3. Colored text",
                "4. Background fill",
                "5. HTML inline styling",
                "6. HTML multi-run formatting",
                "7. HTML wrapping",
                "8. JLabel with HTML",
                "9. JLabel with icon",
                "10. JPanel composite footer",
                "11. Right-aligned JLabel",
                "12. Tall header (explicit height)",
                "13. Demonstration banner (green strip + wrapped sentence)",
                "14. Demonstration banner (HTML-only approximation)",
                "15. Demonstration banner per-page (full on cover, strip on rest)"
        };

        JComboBox<String> presetCombo = new JComboBox<>(presetLabels);
        presetCombo.setSelectedIndex(0);

        JTextArea snippet = new JTextArea(hfSnippetFor(presetKeys[0]));
        snippet.setEditable(false);
        snippet.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        snippet.setBackground(new Color(0xF7F7F7));
        snippet.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCCCCCC)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        presetCombo.addActionListener(e ->
                snippet.setText(hfSnippetFor(presetKeys[presetCombo.getSelectedIndex()])));

        JPanel north = new JPanel(new BorderLayout(6, 4));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row1.add(new JLabel("Preset:"));
        row1.add(presetCombo);
        north.add(row1, BorderLayout.NORTH);
        JScrollPane snippetScroll = new JScrollPane(snippet);
        snippetScroll.setPreferredSize(new Dimension(860, 110));
        north.add(snippetScroll, BorderLayout.CENTER);
        wrapper.add(north, BorderLayout.NORTH);

        // --- Content panel (CENTER) ------------------------------------------
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        wrapper.add(scroll, BorderLayout.CENTER);

        // --- Export controls (SOUTH) -----------------------------------------
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x3A7D44));
        JButton btn = new JButton("Generate PDF");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        btn.addActionListener(e -> {
            String key = presetKeys[presetCombo.getSelectedIndex()];
            Path out = Paths.get(System.getProperty("user.home"), name + "-" + key + ".pdf");
            status.setText("Exporting…");
            status.setForeground(new Color(0x3A7D44));
            Dimension pref = content.getPreferredSize();
            content.setSize(pref.width > 0 ? pref.width : 800, pref.height > 0 ? pref.height : 600);
            content.validate();
            try {
                SwingPdfExporter exporter = SwingPdfExporter.from(content)
                        .pageSize(PageSize.A4)
                        .exportMode(ExportMode.DATA_REPORT)
                        .margins(72, 54, 72, 54);
                if ("demonstration-banner-per-page".equals(key)) {
                    // Page 1: green DEMONSTRATION strip on top, wrapped sentence
                    // below (the same composition as preset 13).
                    JPanel coverPanel = new JPanel(new BorderLayout(0, 0));
                    coverPanel.setOpaque(false);

                    JLabel coverStrip = new JLabel("DEMONSTRATION", SwingConstants.CENTER);
                    coverStrip.setOpaque(true);
                    coverStrip.setBackground(new Color(0x2E8B57));
                    coverStrip.setForeground(Color.WHITE);
                    coverStrip.setFont(coverStrip.getFont().deriveFont(Font.BOLD, 12f));
                    coverStrip.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

                    JLabel blurb = new JLabel(
                            "<html><div style='text-align:center'>" +
                            "The quick brown fox jumps over the lazy dog while the ambitious " +
                            "platypus saunters past a sleepy marmoset carrying a bundle of twigs<br>" +
                            "to demonstrate that a single long sentence can span two lines of " +
                            "centered body copy at the full printable width of the page." +
                            "</div></html>");
                    blurb.setHorizontalAlignment(SwingConstants.CENTER);
                    blurb.setFont(blurb.getFont().deriveFont(9f));
                    blurb.setForeground(new Color(0x333333));
                    blurb.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));

                    coverPanel.add(coverStrip, BorderLayout.NORTH);
                    coverPanel.add(blurb,      BorderLayout.CENTER);

                    // Page 2+: just the green strip on its own (separate JLabel
                    // instance because a Swing component can only have one parent).
                    JLabel runningStrip = new JLabel("DEMONSTRATION", SwingConstants.CENTER);
                    runningStrip.setOpaque(true);
                    runningStrip.setBackground(new Color(0x2E8B57));
                    runningStrip.setForeground(Color.WHITE);
                    runningStrip.setFont(runningStrip.getFont().deriveFont(Font.BOLD, 12f));
                    runningStrip.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

                    HeaderFooter coverBand = HeaderFooter.of(coverPanel).height(56f);
                    HeaderFooter stripBand = HeaderFooter.of(runningStrip).height(22f);

                    exporter = exporter
                            .header((page, pages) -> page == 1 ? coverBand : stripBand);
                    // No footer (matches preset 13).
                } else {
                    HeaderFooter[] hf = hfPreset(key);
                    if (hf[0] != null) exporter = exporter.header(hf[0]);
                    if (hf[1] != null) exporter = exporter.footer(hf[1]);
                }
                exporter.export(out);
                status.setText("Saved: " + out);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(out.toFile());
                }
            } catch (Exception ex) {
                status.setForeground(Color.RED);
                status.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        south.add(status);
        south.add(btn);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    /**
     * Builds the {@link HeaderFooter} pair (header, footer) for a given preset
     * key. Each preset exercises one styling axis or mode. Returns an array of
     * length 2; either entry may be {@code null} to omit that band.
     */
    private static HeaderFooter[] hfPreset(String key) {
        return switch (key) {

            // 1. Plain text baseline -- original behaviour, regression guard
            case "plain-text" -> new HeaderFooter[]{
                    HeaderFooter.of("Warehouse Inventory Report"),
                    HeaderFooter.of("Page {page} of {pages}")
            };

            // 2. Plain text + wrap -- opt-in TEXT-mode word wrap
            case "plain-text-wrap" -> new HeaderFooter[]{
                    HeaderFooter.of(
                            "Warehouse Inventory Report -- this deliberately long header " +
                            "demonstrates the new .wrap(true) option breaking text across lines " +
                            "when it exceeds the printable width")
                            .wrap(true)
                            .height(42f)
                            .fontSize(10f)
                            .color(new Color(0x333333)),
                    HeaderFooter.of("Page {page} of {pages}")
            };

            // 3. Colored text
            case "colored-text" -> new HeaderFooter[]{
                    HeaderFooter.of("WAREHOUSE INVENTORY")
                            .color(new Color(0xB22222))
                            .fontSize(12f),
                    HeaderFooter.of("DRAFT -- Page {page} of {pages}")
                            .color(new Color(0x444444))
                            .fontSize(9f)
            };

            // 4. Background fill
            case "background-fill" -> new HeaderFooter[]{
                    HeaderFooter.of("Warehouse Inventory Report")
                            .color(Color.WHITE)
                            .fontSize(11f)
                            .backgroundColor(new Color(0x2B4C7E))
                            .height(26f),
                    HeaderFooter.of("Page {page} of {pages}")
                            .color(new Color(0x2B4C7E))
                            .fontSize(9f)
                            .backgroundColor(new Color(0xE8EEF7))
                            .height(20f)
            };

            // 5. HTML inline styling
            case "html-inline" -> new HeaderFooter[]{
                    HeaderFooter.html(
                            "<b>Warehouse Inventory</b> &mdash; " +
                            "<span style='color:#888888'>Q2 {page}/{pages}</span>")
                            .fontSize(11f)
                            .height(24f),
                    HeaderFooter.html(
                            "<i>Generated for demonstration</i> &middot; " +
                            "<b>Page {page} of {pages}</b>")
                            .fontSize(9f)
            };

            // 6. HTML multi-run formatting
            case "html-multi-run" -> new HeaderFooter[]{
                    HeaderFooter.html(
                            "<font size='+2' color='#B22222'><b>ACME</b></font> " +
                            "<font color='#333333'>Warehouse Inventory</font> " +
                            "<i>(Internal)</i>")
                            .fontSize(10f)
                            .height(32f),
                    HeaderFooter.html(
                            "<u>Page {page}</u> of <b>{pages}</b> " +
                            "<span style='color:#888'>&bull; printed {page}/{pages}</span>")
                            .fontSize(9f)
            };

            // 7. HTML wrapping
            case "html-wrap" -> new HeaderFooter[]{
                    HeaderFooter.html(
                            "<b style='color:#2B4C7E'>Warehouse Inventory Report</b> &mdash; " +
                            "<i>this HTML header is long enough to wrap to a second line, " +
                            "and wrapping preserves the inline styling of each run</i>")
                            .fontSize(10f)
                            .height(48f),
                    HeaderFooter.html("<b>Page {page}</b> of <b>{pages}</b>").fontSize(9f)
            };

            // 8. JLabel with HTML
            case "jlabel-html" -> {
                JLabel h = new JLabel(
                        "<html><b style='color:#2B4C7E;font-size:13pt'>Warehouse Inventory</b> " +
                        "<span style='color:#999999'>&mdash; Draft</span></html>");
                h.setOpaque(true);
                h.setBackground(new Color(0xEFEFEF));
                h.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                h.setHorizontalAlignment(SwingConstants.LEFT);

                JLabel f = new JLabel("<html><span style='color:#666'>Page <b>{page}</b> of <b>{pages}</b></span></html>");
                f.setHorizontalAlignment(SwingConstants.CENTER);

                yield new HeaderFooter[]{
                        HeaderFooter.of(h).height(28f),
                        HeaderFooter.of(f).height(18f)
                };
            }

            // 9. JLabel with icon
            case "jlabel-icon" -> {
                JLabel h = new JLabel("Warehouse Inventory Report");
                h.setIcon(new ImageIcon(hfSampleIcon(18, new Color(0x2B4C7E))));
                h.setIconTextGap(8);
                h.setFont(h.getFont().deriveFont(Font.BOLD, 12f));
                h.setForeground(new Color(0x2B4C7E));

                JLabel f = new JLabel("Page {page} of {pages}");
                f.setHorizontalAlignment(SwingConstants.CENTER);
                f.setForeground(new Color(0x666666));
                yield new HeaderFooter[]{
                        HeaderFooter.of(h).height(28f).align(HeaderFooter.Alignment.LEFT),
                        HeaderFooter.of(f)
                };
            }

            // 10. JPanel composite footer (company left, page right, top border)
            case "jpanel-footer" -> {
                JLabel h = new JLabel("Warehouse Inventory Report");
                h.setFont(h.getFont().deriveFont(Font.BOLD, 12f));

                JPanel footer = new JPanel(new BorderLayout());
                footer.setOpaque(false);
                footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCCCCCC)));
                JLabel left = new JLabel("Seaglass Foundry");
                left.setFont(left.getFont().deriveFont(Font.ITALIC, 9f));
                left.setForeground(new Color(0x555555));
                left.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
                JLabel right = new JLabel("Page {page} of {pages}");
                right.setFont(right.getFont().deriveFont(9f));
                right.setForeground(new Color(0x555555));
                right.setHorizontalAlignment(SwingConstants.RIGHT);
                right.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 4));
                footer.add(left,  BorderLayout.WEST);
                footer.add(right, BorderLayout.EAST);

                yield new HeaderFooter[]{
                        HeaderFooter.of(h).align(HeaderFooter.Alignment.LEFT),
                        HeaderFooter.of(footer).height(24f)
                };
            }

            // 11. Right-aligned JLabel
            case "right-aligned" -> {
                JLabel h = new JLabel("DRAFT");
                h.setFont(h.getFont().deriveFont(Font.BOLD, 11f));
                h.setForeground(new Color(0xB22222));
                yield new HeaderFooter[]{
                        HeaderFooter.of(h).align(HeaderFooter.Alignment.RIGHT),
                        HeaderFooter.of("Page {page} of {pages}").align(HeaderFooter.Alignment.RIGHT)
                };
            }

            // 12. Tall header: 2-row JPanel with explicit height
            case "tall-header" -> {
                JPanel hp = new JPanel(new GridLayout(2, 1, 0, 2));
                hp.setOpaque(false);
                JLabel top = new JLabel("Warehouse Inventory Report");
                top.setFont(top.getFont().deriveFont(Font.BOLD, 14f));
                top.setForeground(new Color(0x2B4C7E));
                JLabel subtitle = new JLabel("Full stock listing -- Q2 2026");
                subtitle.setFont(subtitle.getFont().deriveFont(Font.ITALIC, 10f));
                subtitle.setForeground(new Color(0x777777));
                hp.add(top);
                hp.add(subtitle);
                yield new HeaderFooter[]{
                        HeaderFooter.of(hp).height(52f).align(HeaderFooter.Alignment.LEFT),
                        HeaderFooter.of("Page {page} of {pages}")
                };
            }

            // 13. Demonstration banner: green strip with white centered text
            // on top, followed by a long sentence that wraps to two lines.
            // No footer. Uses the standard 120-row warehouse table so the
            // export spans at least 3 pages.
            case "demonstration-banner" -> {
                JPanel hp = new JPanel(new BorderLayout(0, 0));
                hp.setOpaque(false);

                JLabel banner = new JLabel("DEMONSTRATION", SwingConstants.CENTER);
                banner.setOpaque(true);
                banner.setBackground(new Color(0x2E8B57));
                banner.setForeground(Color.WHITE);
                banner.setFont(banner.getFont().deriveFont(Font.BOLD, 12f));
                banner.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

                // NOTE: JLabel HTML in component-mode headers breaks only on
                // explicit block tags (<br>, <p>, <div>); width-based auto-wrap
                // is not supported in that path. Use <br> to force two lines.
                JLabel blurb = new JLabel(
                        "<html><div style='text-align:center'>" +
                        "The quick brown fox jumps over the lazy dog while the ambitious " +
                        "platypus saunters past a sleepy marmoset carrying a bundle of twigs<br>" +
                        "to demonstrate that a single long sentence can span two lines of " +
                        "centered body copy at the full printable width of the page." +
                        "</div></html>");
                blurb.setHorizontalAlignment(SwingConstants.CENTER);
                blurb.setFont(blurb.getFont().deriveFont(9f));
                blurb.setForeground(new Color(0x333333));
                blurb.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));

                hp.add(banner, BorderLayout.NORTH);
                hp.add(blurb,  BorderLayout.CENTER);

                yield new HeaderFooter[]{
                        HeaderFooter.of(hp).height(56f),
                        null // no footer
                };
            }

            // 14. HTML-only approximation of preset 13. HtmlStyledTextRenderer
            // doesn't support per-block background colours, so the closest
            // single-call HTML equivalent paints the entire band green and
            // renders all text in white. Visually similar in spirit but the
            // "strip + body" split of preset 13 isn't reproducible in pure
            // HTML without extending the parser to recognise CSS backgrounds.
            case "demonstration-banner-html" -> new HeaderFooter[]{
                    HeaderFooter.html(
                            "<b><font size='+1'>DEMONSTRATION</font></b><br>" +
                            "The quick brown fox jumps over the lazy dog while the " +
                            "ambitious platypus saunters past a sleepy marmoset " +
                            "carrying a bundle of twigs to demonstrate that a single " +
                            "long sentence can span two lines of centered body copy " +
                            "at the full printable width of the page.")
                            .align(HeaderFooter.Alignment.CENTER)
                            .color(Color.WHITE)
                            .backgroundColor(new Color(0x2E8B57))
                            .fontSize(9f)
                            .height(56f),
                    null // no footer
            };

            default -> new HeaderFooter[]{
                    HeaderFooter.of("Warehouse Inventory"),
                    HeaderFooter.of("Page {page} of {pages}")
            };
        };
    }

    /** Source-style snippet shown in the preview panel for each preset. */
    private static String hfSnippetFor(String key) {
        return switch (key) {
            case "plain-text" -> """
                    HeaderFooter.of("Warehouse Inventory Report")
                    HeaderFooter.of("Page {page} of {pages}")""";
            case "plain-text-wrap" -> """
                    HeaderFooter.of("... deliberately long header ...")
                        .wrap(true)
                        .height(42f)
                        .fontSize(10f)""";
            case "colored-text" -> """
                    HeaderFooter.of("WAREHOUSE INVENTORY")
                        .color(new Color(0xB22222))
                        .fontSize(12f)""";
            case "background-fill" -> """
                    HeaderFooter.of("Warehouse Inventory Report")
                        .color(Color.WHITE)
                        .backgroundColor(new Color(0x2B4C7E))
                        .height(26f)""";
            case "html-inline" -> """
                    HeaderFooter.html("<b>Warehouse Inventory</b> &mdash; " +
                                      "<span style='color:#888888'>Q2 {page}/{pages}</span>")""";
            case "html-multi-run" -> """
                    HeaderFooter.html("<font size='+2' color='#B22222'><b>ACME</b></font> " +
                                      "<font color='#333'>Warehouse Inventory</font> <i>(Internal)</i>")""";
            case "html-wrap" -> """
                    HeaderFooter.html("<b style='color:#2B4C7E'>Warehouse Inventory Report</b> " +
                                      "&mdash; <i>long enough to wrap to a second line</i>")
                        .height(48f)""";
            case "jlabel-html" -> """
                    JLabel h = new JLabel("<html><b>Warehouse Inventory</b> " +
                                          "<span style='color:#999'>&mdash; Draft</span></html>");
                    h.setOpaque(true);
                    h.setBackground(new Color(0xEFEFEF));
                    h.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                    HeaderFooter.of(h).height(28f);""";
            case "jlabel-icon" -> """
                    JLabel h = new JLabel("Warehouse Inventory Report");
                    h.setIcon(new ImageIcon(iconImage));
                    h.setFont(h.getFont().deriveFont(Font.BOLD, 12f));
                    HeaderFooter.of(h).align(Alignment.LEFT).height(28f);""";
            case "jpanel-footer" -> """
                    JPanel footer = new JPanel(new BorderLayout());
                    footer.add(new JLabel("Seaglass Foundry"), BorderLayout.WEST);
                    footer.add(new JLabel("Page {page} of {pages}"), BorderLayout.EAST);
                    footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ...));
                    HeaderFooter.of(footer).height(24f);""";
            case "right-aligned" -> """
                    JLabel h = new JLabel("DRAFT");
                    h.setFont(h.getFont().deriveFont(Font.BOLD, 11f));
                    h.setForeground(new Color(0xB22222));
                    HeaderFooter.of(h).align(Alignment.RIGHT);""";
            case "tall-header" -> """
                    JPanel hp = new JPanel(new GridLayout(2, 1));
                    hp.add(new JLabel("Warehouse Inventory Report"));   // bold 14pt
                    hp.add(new JLabel("Full stock listing -- Q2 2026")); // italic 10pt
                    HeaderFooter.of(hp).height(52f).align(Alignment.LEFT);""";
            case "demonstration-banner" -> """
                    JPanel hp = new JPanel(new BorderLayout());
                    JLabel banner = new JLabel("DEMONSTRATION", SwingConstants.CENTER);
                    banner.setOpaque(true);
                    banner.setBackground(new Color(0x2E8B57));  // green
                    banner.setForeground(Color.WHITE);
                    banner.setFont(banner.getFont().deriveFont(Font.BOLD, 12f));
                    JLabel blurb = new JLabel(
                        "<html><div style='text-align:center'>" +
                        "The quick brown fox jumps over the lazy dog while the ambitious " +
                        "platypus saunters past a sleepy marmoset carrying a bundle of " +
                        "twigs<br>to demonstrate that a single long sentence can span two " +
                        "lines of centered body copy at the full printable width of the page." +
                        "</div></html>");
                    hp.add(banner, BorderLayout.NORTH);
                    hp.add(blurb,  BorderLayout.CENTER);
                    HeaderFooter.of(hp).height(56f);   // no footer""";
            case "demonstration-banner-html" -> """
                    // HTML-only approximation: entire band is green (block
                    // backgrounds aren't supported in the HTML path, so we
                    // can't isolate the green strip to just the first line).
                    HeaderFooter.html(
                        "<b><font size='+1'>DEMONSTRATION</font></b><br>" +
                        "The quick brown fox jumps over the lazy dog while the " +
                        "ambitious platypus saunters past a sleepy marmoset carrying " +
                        "a bundle of twigs to demonstrate that a single long sentence " +
                        "can span two lines of centered body copy at the full " +
                        "printable width of the page.")
                        .align(Alignment.CENTER)
                        .color(Color.WHITE)
                        .backgroundColor(new Color(0x2E8B57))
                        .fontSize(9f)
                        .height(56f);  // no footer""";
            case "demonstration-banner-per-page" -> """
                    // Per-page provider: full demonstration banner (green strip
                    // + wrapped sentence) on page 1, just the green strip on
                    // every subsequent page.
                    JPanel coverPanel = new JPanel(new BorderLayout());
                    coverPanel.add(greenStrip(),    BorderLayout.NORTH);   // styled as preset 13
                    coverPanel.add(wrappedSentence, BorderLayout.CENTER);

                    HeaderFooter coverBand = HeaderFooter.of(coverPanel).height(56f);
                    HeaderFooter stripBand = HeaderFooter.of(greenStrip()).height(22f);

                    exporter.header((page, pages) -> page == 1 ? coverBand : stripBand);
                    // No footer.""";
            default -> "";
        };
    }

    /** Build a small diamond icon for the icon-header preset. */
    private static BufferedImage hfSampleIcon(int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        Path2D diamond = new Path2D.Double();
        diamond.moveTo(size / 2.0, 1);
        diamond.lineTo(size - 1,   size / 2.0);
        diamond.lineTo(size / 2.0, size - 1);
        diamond.lineTo(1,          size / 2.0);
        diamond.closePath();
        g.fill(diamond);
        g.dispose();
        return img;
    }

    private static JPanel buildHeaderFooterTable() {
        Color bgColor = new Color(0xF0F4FA);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bgColor);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        p.add(heading("Warehouse Inventory Report"));
        p.add(vgap(4));
        JLabel sub = new JLabel("Full stock listing as of 25 March 2026 — spans multiple pages");
        sub.setFont(sub.getFont().deriveFont(Font.ITALIC, 11f));
        sub.setForeground(new Color(0x555555));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(sub);
        p.add(vgap(12));

        // Build a large table that will span several pages
        String[] cols = {"SKU", "Product Name", "Category", "Warehouse", "Qty", "Unit Price", "Total Value", "Status"};
        String[] categories = {"Electronics", "Furniture", "Office", "Safety", "Lighting", "Plumbing", "Hardware"};
        String[] warehouses = {"WH-North", "WH-South", "WH-East", "WH-West", "WH-Central"};
        String[] statuses   = {"In Stock", "Low Stock", "Reorder", "Backordered", "Discontinued"};
        String[] adjectives = {"Heavy-Duty", "Premium", "Standard", "Compact", "Industrial", "Eco", "Pro"};
        String[] nouns      = {"Bracket", "Panel", "Sensor", "Cable", "Valve", "Lamp", "Switch", "Shelf",
                               "Mount", "Clip", "Relay", "Gauge", "Fitting", "Hinge", "Bolt"};

        int rowCount = 120;
        Random rng = new Random(99);
        Object[][] data = new Object[rowCount][8];
        for (int i = 0; i < rowCount; i++) {
            int qty   = 5 + rng.nextInt(995);
            double price = 2.50 + rng.nextDouble() * 497.50;
            data[i] = new Object[]{
                String.format("SKU-%05d", 10000 + i),
                adjectives[rng.nextInt(adjectives.length)] + " " + nouns[rng.nextInt(nouns.length)],
                categories[rng.nextInt(categories.length)],
                warehouses[rng.nextInt(warehouses.length)],
                qty,
                String.format("$%.2f", price),
                String.format("$%,.2f", qty * price),
                statuses[rng.nextInt(statuses.length)]
            };
        }

        JTable table = new JTable(new DefaultTableModel(data, cols) {
            private static final long serialVersionUID = 7461029384756102938L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(0xCCD6E6));
        table.getTableHeader().setBackground(new Color(0x2B4C7E));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(table.getFont().deriveFont(Font.BOLD));

        JScrollPane sp = new JScrollPane(table);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(860, rowCount * 22 + 30));
        sp.getViewport().setBackground(bgColor);
        p.add(sp);

        p.add(vgap(12));
        JLabel footer = new JLabel(rowCount + " items listed  |  Report generated for demonstration purposes");
        footer.setFont(footer.getFont().deriveFont(Font.ITALIC, 10f));
        footer.setForeground(new Color(0x777777));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(footer);

        return p;
    }

    // -----------------------------------------------------------------------
    // Tab 14 — AcroForm (fillable PDF)
    // -----------------------------------------------------------------------

    /**
     * Like {@link #tabWrapper} but exports with {@code enableAcroForm()} so the
     * resulting PDF contains interactive form fields rather than vector ink.
     */
    private static JPanel acroTabWrapper(String name, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x3A7D44));
        JButton btn = new JButton("Generate Fillable PDF");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        btn.addActionListener(e -> {
            Path out = Paths.get(System.getProperty("user.home"), name + ".pdf");
            status.setText("Exporting…");
            Dimension pref = content.getPreferredSize();
            content.setSize(pref.width > 0 ? pref.width : 560, pref.height > 0 ? pref.height : 660);
            content.validate();
            try {
                SwingPdfExporter.from(content)
                        .pageSize(PageSize.A4)
                        .enableAcroForm()
                        .export(out);
                status.setText("Saved: " + out);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(out.toFile());
                }
            } catch (Exception ex) {
                status.setForeground(Color.RED);
                status.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        south.add(status);
        south.add(btn);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    private static JPanel buildAcroForm() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ---- Text fields ---------------------------------------------------
        JPanel textSection = titledSection2("Text fields");
        textSection.setLayout(new GridBagLayout());

        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(3, 4, 3, 4);

        int tRow = 0;
        for (Object[] row : new Object[][]{
                {"First name:",  named2(new JTextField("Jane", 20),                    "firstName")},
                {"Last name:",   named2(new JTextField("Smith", 20),                   "lastName")},
                {"Email:",       named2(new JTextField("jane.smith@example.com", 20),  "email")},
                {"Password:",    named2(new JPasswordField("hunter2", 20),             "password")},
        }) {
            tc.gridy = tRow; tc.gridx = 0; tc.weightx = 0; tc.fill = GridBagConstraints.NONE;
            tc.anchor = GridBagConstraints.EAST; tc.weighty = 0;
            textSection.add(label2((String) row[0]), tc);
            tc.gridx = 1; tc.weightx = 1.0; tc.fill = GridBagConstraints.HORIZONTAL;
            tc.anchor = GridBagConstraints.WEST;
            textSection.add((Component) row[1], tc);
            tRow++;
        }

        // Notes: JTextArea in a fixed-height scroll pane so the user can type
        // freely without reflowing peer rows in the GridBagLayout.
        JTextArea notes = named2(new JTextArea("Multi-line notes\ngo here.", 5, 20), "notes");
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setPreferredSize(new Dimension(200, 88));
        tc.gridy = tRow; tc.gridx = 0; tc.weightx = 0; tc.fill = GridBagConstraints.NONE;
        tc.anchor = GridBagConstraints.NORTHEAST; tc.weighty = 0;
        textSection.add(label2("Notes:"), tc);
        tc.gridx = 1; tc.weightx = 1.0; tc.fill = GridBagConstraints.HORIZONTAL;
        tc.anchor = GridBagConstraints.WEST;
        textSection.add(notesScroll, tc);

        // ---- Checkboxes ----------------------------------------------------
        JPanel checkSection = titledSection2("Checkboxes");
        checkSection.setLayout(new GridLayout(0, 2, 8, 4));

        checkSection.add(named2(new JCheckBox("Subscribe to newsletter", true),  "newsletter"));
        checkSection.add(named2(new JCheckBox("I agree to the terms",     true),  "termsAgreed"));
        checkSection.add(named2(new JCheckBox("Email notifications",      false), "notifications"));
        checkSection.add(named2(new JCheckBox("Dark mode",                false), "darkMode"));

        // ---- Radio buttons — two independent groups ------------------------
        JPanel radioSection = titledSection2("Radio buttons");
        radioSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 16);

        gbc.gridx = 0; gbc.gridy = 0;
        radioSection.add(label2("Shirt size:"), gbc);
        ButtonGroup sizeGroup = new ButtonGroup();
        String[] sizes = {"Small", "Medium", "Large", "XL"};
        for (int i = 0; i < sizes.length; i++) {
            JRadioButton rb = named2(new JRadioButton(sizes[i], i == 1), "size_" + sizes[i].toLowerCase());
            sizeGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        gbc.gridx = 0; gbc.gridy = 1;
        radioSection.add(label2("Payment:"), gbc);
        ButtonGroup payGroup = new ButtonGroup();
        String[] payments = {"Credit card", "PayPal", "Bank transfer"};
        for (int i = 0; i < payments.length; i++) {
            JRadioButton rb = named2(new JRadioButton(payments[i], i == 0), "payment_" + i);
            payGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        // ---- Combo boxes ---------------------------------------------------
        JPanel comboSection = titledSection2("Combo boxes");
        comboSection.setLayout(new GridLayout(0, 2, 8, 6));

        comboSection.add(label2("Country:"));
        JComboBox<String> country = named2(
                new JComboBox<>(new String[]{"United States", "Canada", "United Kingdom", "Australia", "Other"}),
                "country");
        country.setSelectedItem("Canada");
        comboSection.add(country);

        comboSection.add(label2("Priority:"));
        JComboBox<String> priority = named2(
                new JComboBox<>(new String[]{"Low", "Medium", "High", "Critical"}),
                "priority");
        priority.setSelectedItem("High");
        comboSection.add(priority);

        comboSection.add(label2("Custom (editable):"));
        JComboBox<String> custom = named2(
                new JComboBox<>(new String[]{"Option A", "Option B", "Option C"}),
                "customOption");
        custom.setEditable(true);
        custom.setSelectedItem("My custom value");
        comboSection.add(custom);

        // Add all sections with GridBagLayout: fill horizontally, fixed preferred height
        GridBagConstraints rootGbc = new GridBagConstraints();
        rootGbc.gridx = 0;
        rootGbc.fill = GridBagConstraints.HORIZONTAL;
        rootGbc.weightx = 1.0;
        rootGbc.weighty = 0.0;
        rootGbc.insets = new Insets(0, 0, 12, 0);
        rootGbc.gridy = 0; root.add(textSection, rootGbc);
        rootGbc.gridy = 1; root.add(checkSection, rootGbc);
        rootGbc.gridy = 2; root.add(radioSection, rootGbc);
        rootGbc.gridy = 3; rootGbc.insets = new Insets(0, 0, 0, 0);
        root.add(comboSection, rootGbc);

        // Filler row absorbs extra vertical space so sections keep their preferred heights
        rootGbc.gridy = 4;
        rootGbc.weighty = 1.0;
        rootGbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        root.add(filler, rootGbc);

        root.setPreferredSize(new Dimension(560, 660));
        return root;
    }

    // -----------------------------------------------------------------------
    // Tab — Vector Handlers (custom Graphics2D rendering to vector PDF)
    // -----------------------------------------------------------------------

    /**
     * Like {@link #tabWrapper} but exports using {@code registerHandler()} for
     * custom-painted components so their output is vector instead of rasterised.
     */
    private static JPanel vectorTabWrapper(String name, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JLabel status = new JLabel(" ");
        status.setForeground(new Color(0x3A7D44));
        JButton btn = new JButton("Generate PDF");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        btn.addActionListener(e -> {
            Path out = Paths.get(System.getProperty("user.home"), name + ".pdf");
            status.setText("Exporting\u2026");
            Dimension pref = content.getPreferredSize();
            content.setSize(pref.width > 0 ? pref.width : 800, pref.height > 0 ? pref.height : 600);
            content.validate();
            try {
                SwingPdfExporter.from(content)
                        .pageSize(PageSize.A4)
                        .exportMode(ExportMode.DATA_REPORT)
                        .margins(36, 36, 36, 36)
                        // Basic case: simple shapes and text
                        .registerHandler(SimpleChartPanel.class, (comp, g2, bounds) -> {
                            ((SimpleChartPanel) comp).drawVector(g2, bounds);
                        })
                        // Complex case: multi-series chart with gradients, curves, and annotations
                        .registerHandler(ComplexChartPanel.class, (comp, g2, bounds) -> {
                            ((ComplexChartPanel) comp).drawVector(g2, bounds);
                        })
                        .export(out);
                status.setText("Saved: " + out);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(out.toFile());
                }
            } catch (Exception ex) {
                status.setForeground(Color.RED);
                status.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        south.add(status);
        south.add(btn);
        wrapper.add(south, BorderLayout.SOUTH);
        return wrapper;
    }

    private static JPanel buildVectorHandlers() {
        JPanel p = white();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(pad(16));

        p.add(heading("Vector Component Handlers"));
        p.add(vgap(4));
        JLabel desc = new JLabel(
                "<html>Components below use custom <code>paintComponent()</code> drawing. "
                + "With <code>registerHandler()</code>, their output is vector PDF "
                + "(selectable text, resolution-independent shapes) instead of rasterised bitmaps.</html>");
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(desc);
        p.add(vgap(16));

        // ---- Basic case: simple bar chart with labels ----
        p.add(section("Basic: Bar Chart (shapes + vector text)"));
        p.add(vgap(4));
        SimpleChartPanel simpleChart = new SimpleChartPanel();
        simpleChart.setPreferredSize(new Dimension(700, 280));
        simpleChart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        simpleChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(simpleChart);
        p.add(vgap(16));

        // ---- Complex case: multi-series line chart with gradients and annotations ----
        p.add(section("Complex: Multi-Series Line Chart (gradients, curves, annotations)"));
        p.add(vgap(4));
        ComplexChartPanel complexChart = new ComplexChartPanel();
        complexChart.setPreferredSize(new Dimension(700, 360));
        complexChart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        complexChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(complexChart);
        p.add(vgap(16));

        // ---- Side-by-side comparison note ----
        JLabel note = new JLabel(
                "<html><b>Tip:</b> Zoom into the PDF output to verify that text is selectable "
                + "and shapes remain sharp at any zoom level. Compare with a rasterised export "
                + "(without registerHandler) to see the difference.</html>");
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setForeground(new Color(0x555555));
        p.add(note);
        p.add(vgap(8));

        return p;
    }

    // -----------------------------------------------------------------------
    // Custom-painted components for vector handler demo
    // -----------------------------------------------------------------------

    /**
     * Basic case: a bar chart with solid-colour bars, axis lines, and text labels.
     * Overrides paintComponent so it would normally be rasterised.
     */
    static class SimpleChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final String[] LABELS = {"Q1", "Q2", "Q3", "Q4"};
        private static final double[] VALUES = {42, 78, 55, 91};
        private static final Color[] BAR_COLORS = {
                new Color(0x4285F4), new Color(0xEA4335),
                new Color(0xFBBC05), new Color(0x34A853)
        };

        SimpleChartPanel() {
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawVector((Graphics2D) g, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        }

        void drawVector(Graphics2D g2, Rectangle2D bounds) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double x0 = bounds.getX() + 60;
            double y0 = bounds.getY() + 30;
            double chartW = bounds.getWidth() - 90;
            double chartH = bounds.getHeight() - 80;

            // Title
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.setColor(new Color(0x333333));
            g2.drawString("Quarterly Revenue ($M)", (float) (x0 + chartW / 2 - 100), (float) (y0 - 10));

            // Y-axis gridlines and labels
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            double maxVal = 100;
            for (int tick = 0; tick <= 100; tick += 20) {
                double ty = y0 + chartH - (tick / maxVal) * chartH;
                g2.setColor(new Color(0xDDDDDD));
                g2.draw(new Line2D.Double(x0, ty, x0 + chartW, ty));
                g2.setColor(new Color(0x666666));
                g2.drawString(String.valueOf(tick), (float) (x0 - 30), (float) (ty + 4));
            }

            // Axis lines
            g2.setColor(new Color(0x333333));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Line2D.Double(x0, y0, x0, y0 + chartH));
            g2.draw(new Line2D.Double(x0, y0 + chartH, x0 + chartW, y0 + chartH));

            // Bars
            double barW = chartW / VALUES.length * 0.65;
            double gap = chartW / VALUES.length;
            for (int i = 0; i < VALUES.length; i++) {
                double bx = x0 + i * gap + (gap - barW) / 2;
                double bh = (VALUES[i] / maxVal) * chartH;
                double by = y0 + chartH - bh;

                g2.setColor(BAR_COLORS[i]);
                g2.fill(new Rectangle2D.Double(bx, by, barW, bh));

                // Bar outline
                g2.setColor(BAR_COLORS[i].darker());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Rectangle2D.Double(bx, by, barW, bh));

                // Value label above bar
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(new Color(0x333333));
                String val = "$" + (int) VALUES[i] + "M";
                g2.drawString(val, (float) (bx + barW / 2 - 16), (float) (by - 6));

                // X-axis label
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(new Color(0x666666));
                g2.drawString(LABELS[i], (float) (bx + barW / 2 - 8), (float) (y0 + chartH + 18));
            }
        }
    }

    /**
     * Complex case: multi-series line chart with gradient fills, cubic curves,
     * data point markers, a legend, and annotations.
     */
    static class ComplexChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        private static final double[] SERIES_A = {12, 19, 28, 35, 42, 55, 62, 58, 49, 40, 30, 22};
        private static final double[] SERIES_B = {8,  14, 22, 30, 38, 45, 50, 52, 48, 38, 25, 15};
        private static final double[] SERIES_C = {5,  10, 15, 18, 25, 30, 35, 40, 42, 35, 28, 18};

        ComplexChartPanel() {
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawVector((Graphics2D) g, new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
        }

        void drawVector(Graphics2D g2, Rectangle2D bounds) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double x0 = bounds.getX() + 60;
            double y0 = bounds.getY() + 40;
            double chartW = bounds.getWidth() - 100;
            double chartH = bounds.getHeight() - 110;
            double maxVal = 70;

            // Title
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.setColor(new Color(0x222222));
            g2.drawString("Monthly Active Users (thousands)", (float) (x0 + chartW / 2 - 140), (float) (y0 - 16));

            // Grid
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setStroke(new BasicStroke(0.5f));
            for (int tick = 0; tick <= 70; tick += 10) {
                double ty = y0 + chartH - (tick / maxVal) * chartH;
                g2.setColor(new Color(0xE8E8E8));
                g2.draw(new Line2D.Double(x0, ty, x0 + chartW, ty));
                g2.setColor(new Color(0x888888));
                g2.drawString(String.valueOf(tick) + "k", (float) (x0 - 32), (float) (ty + 4));
            }

            // X-axis month labels
            double stepX = chartW / (MONTHS.length - 1);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i < MONTHS.length; i++) {
                double mx = x0 + i * stepX;
                g2.setColor(new Color(0xE0E0E0));
                g2.draw(new Line2D.Double(mx, y0, mx, y0 + chartH));
                g2.setColor(new Color(0x666666));
                g2.drawString(MONTHS[i], (float) (mx - 10), (float) (y0 + chartH + 16));
            }

            // Axes
            g2.setColor(new Color(0x333333));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Line2D.Double(x0, y0, x0, y0 + chartH));
            g2.draw(new Line2D.Double(x0, y0 + chartH, x0 + chartW, y0 + chartH));

            // Series with gradient fill under curves
            Color colorA = new Color(0x4285F4);
            Color colorB = new Color(0xEA4335);
            Color colorC = new Color(0x34A853);
            drawSeriesWithFill(g2, SERIES_C, colorC, x0, y0, chartW, chartH, maxVal, stepX, 0.15f);
            drawSeriesWithFill(g2, SERIES_B, colorB, x0, y0, chartW, chartH, maxVal, stepX, 0.15f);
            drawSeriesWithFill(g2, SERIES_A, colorA, x0, y0, chartW, chartH, maxVal, stepX, 0.15f);

            // Lines and data points on top
            drawSeriesLine(g2, SERIES_C, colorC, x0, y0, chartW, chartH, maxVal, stepX);
            drawSeriesLine(g2, SERIES_B, colorB, x0, y0, chartW, chartH, maxVal, stepX);
            drawSeriesLine(g2, SERIES_A, colorA, x0, y0, chartW, chartH, maxVal, stepX);

            // Annotation: peak value on series A
            int peakIdx = 6; // July
            double peakX = x0 + peakIdx * stepX;
            double peakY = y0 + chartH - (SERIES_A[peakIdx] / maxVal) * chartH;
            g2.setColor(colorA.darker());
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g2.draw(new Line2D.Double(peakX, peakY - 8, peakX, peakY - 35));
            g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("Peak: 62k", (float) (peakX - 22), (float) (peakY - 40));

            // Legend
            double legendX = x0 + chartW - 180;
            double legendY = y0 + chartH + 30;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            drawLegendItem(g2, "Product A", colorA, legendX, legendY);
            drawLegendItem(g2, "Product B", colorB, legendX + 80, legendY);
            drawLegendItem(g2, "Product C", colorC, legendX + 160, legendY);
        }

        private static void drawSeriesWithFill(Graphics2D g2, double[] data, Color color,
                double x0, double y0, double chartW, double chartH, double maxVal,
                double stepX, float alpha) {
            Path2D.Double fillPath = new Path2D.Double();
            double firstX = x0;
            double firstY = y0 + chartH - (data[0] / maxVal) * chartH;
            fillPath.moveTo(firstX, y0 + chartH); // baseline
            fillPath.lineTo(firstX, firstY);
            for (int i = 1; i < data.length; i++) {
                double px = x0 + i * stepX;
                double py = y0 + chartH - (data[i] / maxVal) * chartH;
                fillPath.lineTo(px, py);
            }
            fillPath.lineTo(x0 + (data.length - 1) * stepX, y0 + chartH); // back to baseline
            fillPath.closePath();

            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (255 * alpha));
            g2.setColor(fillColor);
            g2.fill(fillPath);
        }

        private static void drawSeriesLine(Graphics2D g2, double[] data, Color color,
                double x0, double y0, double chartW, double chartH, double maxVal, double stepX) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < data.length - 1; i++) {
                double px1 = x0 + i * stepX;
                double py1 = y0 + chartH - (data[i] / maxVal) * chartH;
                double px2 = x0 + (i + 1) * stepX;
                double py2 = y0 + chartH - (data[i + 1] / maxVal) * chartH;
                g2.draw(new Line2D.Double(px1, py1, px2, py2));
            }

            // Data point markers
            for (int i = 0; i < data.length; i++) {
                double px = x0 + i * stepX;
                double py = y0 + chartH - (data[i] / maxVal) * chartH;
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
            }
        }

        private static void drawLegendItem(Graphics2D g2, String label, Color color,
                double x, double y) {
            g2.setColor(color);
            g2.fill(new Rectangle2D.Double(x, y - 8, 12, 12));
            g2.setColor(new Color(0x333333));
            g2.drawString(label, (float) (x + 16), (float) (y + 2));
        }
    }

    // -----------------------------------------------------------------------
    // AcroForm helpers
    // -----------------------------------------------------------------------

    private static JPanel titledSection2(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP));
        return p;
    }

    private static JLabel label2(String text) {
        JLabel l = new JLabel(text);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private static <T extends JComponent> T named2(T comp, String name) {
        comp.setName(name);
        return comp;
    }
}
