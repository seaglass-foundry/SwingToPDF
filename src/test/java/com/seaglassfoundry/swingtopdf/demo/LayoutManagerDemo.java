package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates layout-manager edge cases in both export modes.
 *
 * <p>Covers CardLayout, GroupLayout, SpringLayout, deeply nested layouts,
 * and edge cases (zero-size panels, null layout with overlapping components,
 * empty containers).
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.LayoutManagerDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class LayoutManagerDemo {

    public static void main(String[] args) throws Exception {
        Path outSnapshot   = Paths.get("swingtopdf-layouts-snapshot.pdf").toAbsolutePath();
        Path outDataReport = Paths.get("swingtopdf-layouts-datareport.pdf").toAbsolutePath();

        JPanel[] holder = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildRoot();
            holder[0].setPreferredSize(new Dimension(750, 1400));
            holder[0].setSize(750, 1400);
            holder[0].validate();
            latch.countDown();
        });
        latch.await();

        HeaderFooter footer = HeaderFooter.of("Page {page} of {pages}");

        System.out.println("Exporting UI_SNAPSHOT -> " + outSnapshot);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.UI_SNAPSHOT)
                .footer(footer)
                .export(outSnapshot);

        System.out.println("Exporting DATA_REPORT -> " + outDataReport);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .footer(footer)
                .export(outDataReport);

        System.out.println("Done.");
        System.out.println("  snapshot:    CardLayout shows only the active card.");
        System.out.println("  data report: CardLayout still shows only the active card (limitation).");

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(outSnapshot.toFile());
            Desktop.getDesktop().open(outDataReport.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JPanel buildRoot() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setOpaque(true);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(heading("Section 1: CardLayout"));
        root.add(vgap(8));
        root.add(buildCardLayoutSection());
        root.add(vgap(4));
        JLabel cardNote = new JLabel(
                "<html><body style='width:600px'><i>UI_SNAPSHOT and DATA_REPORT both show "
                + "only the active card. Use JTabbedPane for multi-card export.</i></body></html>");
        cardNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cardNote);
        root.add(vgap(16));

        root.add(heading("Section 2: GroupLayout (Form)"));
        root.add(vgap(8));
        root.add(buildGroupLayoutForm());
        root.add(vgap(16));

        root.add(heading("Section 3: GroupLayout (Complex)"));
        root.add(vgap(8));
        root.add(buildGroupLayoutComplex());
        root.add(vgap(16));

        root.add(heading("Section 4: SpringLayout"));
        root.add(vgap(8));
        root.add(buildSpringLayoutSection());
        root.add(vgap(16));

        root.add(heading("Section 5: Nested Layout Stress Test"));
        root.add(vgap(8));
        root.add(buildNestedStressTest());
        root.add(vgap(16));

        root.add(heading("Section 6: Edge Cases"));
        root.add(vgap(8));
        root.add(buildEdgeCases());

        return root;
    }

    // -----------------------------------------------------------------------
    // Section 1: CardLayout
    // -----------------------------------------------------------------------

    private static JPanel buildCardLayoutSection() {
        JPanel cards = new JPanel(new CardLayout());
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Card 1 — "Dashboard" (the selected card)
        JPanel dashboard = new JPanel(new GridLayout(1, 3, 8, 0));
        dashboard.setOpaque(false);
        dashboard.add(kpiLabel("Revenue", "$4.8M"));
        dashboard.add(kpiLabel("Users", "12,400"));
        dashboard.add(kpiLabel("Uptime", "99.97%"));
        cards.add(dashboard, "Dashboard");

        // Card 2 — "Settings" (hidden)
        JPanel settings = new JPanel(new GridBagLayout());
        settings.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;
        settings.add(new JCheckBox("Enable notifications", true), gbc);
        gbc.gridy = 1;
        settings.add(new JCheckBox("Dark mode"), gbc);
        cards.add(settings, "Settings");

        // Card 3 — "Log" (hidden)
        JTextArea log = new JTextArea("CardLayout Log Entry: this text should NOT appear in the PDF.");
        log.setRows(3);
        cards.add(new JScrollPane(log), "Log");

        // Show Dashboard
        ((CardLayout) cards.getLayout()).show(cards, "Dashboard");
        return cards;
    }

    // -----------------------------------------------------------------------
    // Section 2: GroupLayout (Form)
    // -----------------------------------------------------------------------

    private static JPanel buildGroupLayoutForm() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel nameLabel = new JLabel("Name:");
        JTextField nameField = new JTextField("Alice GroupLayout");
        JLabel countryLabel = new JLabel("Country:");
        JComboBox<String> countryCombo = new JComboBox<>(new String[]{"USA", "Canada", "UK"});
        JLabel subscribeLabel = new JLabel("Subscribe:");
        JCheckBox subscribeCb = new JCheckBox("Yes, send emails", true);

        gl.linkSize(SwingConstants.HORIZONTAL, nameLabel, countryLabel, subscribeLabel);

        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(nameLabel)
                        .addComponent(countryLabel)
                        .addComponent(subscribeLabel))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(nameField, 200, 200, 300)
                        .addComponent(countryCombo, 200, 200, 300)
                        .addComponent(subscribeCb)));

        gl.setVerticalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(nameLabel).addComponent(nameField))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(countryLabel).addComponent(countryCombo))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(subscribeLabel).addComponent(subscribeCb)));

        return p;
    }

    // -----------------------------------------------------------------------
    // Section 3: GroupLayout (Complex)
    // -----------------------------------------------------------------------

    private static JPanel buildGroupLayoutComplex() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel col1 = new JLabel("Column A");
        JLabel col2 = new JLabel("Column B");
        JLabel col3 = new JLabel("Column C");
        JButton btn1 = new JButton("Action 1");
        JButton btn2 = new JButton("Action 2");

        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(col1)
                        .addComponent(btn1))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(col2))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(col3)
                        .addComponent(btn2)));

        gl.setVerticalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(col1).addComponent(col2).addComponent(col3))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btn1).addComponent(btn2)));

        return p;
    }

    // -----------------------------------------------------------------------
    // Section 4: SpringLayout
    // -----------------------------------------------------------------------

    private static JPanel buildSpringLayoutSection() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        SpringLayout sl = new SpringLayout();
        p.setLayout(sl);
        p.setPreferredSize(new Dimension(400, 70));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField("spring@example.com");
        emailField.setColumns(20);
        JLabel phoneLabel = new JLabel("Phone:");
        JTextField phoneField = new JTextField("+1-555-SPRING");
        phoneField.setColumns(20);

        p.add(emailLabel);
        p.add(emailField);
        p.add(phoneLabel);
        p.add(phoneField);

        // Email row
        sl.putConstraint(SpringLayout.WEST, emailLabel, 8, SpringLayout.WEST, p);
        sl.putConstraint(SpringLayout.NORTH, emailLabel, 8, SpringLayout.NORTH, p);
        sl.putConstraint(SpringLayout.WEST, emailField, 8, SpringLayout.EAST, emailLabel);
        sl.putConstraint(SpringLayout.NORTH, emailField, 5, SpringLayout.NORTH, p);

        // Phone row — relative to email row
        sl.putConstraint(SpringLayout.WEST, phoneLabel, 8, SpringLayout.WEST, p);
        sl.putConstraint(SpringLayout.NORTH, phoneLabel, 8, SpringLayout.SOUTH, emailLabel);
        sl.putConstraint(SpringLayout.WEST, phoneField, 8, SpringLayout.EAST, phoneLabel);
        sl.putConstraint(SpringLayout.NORTH, phoneField, 5, SpringLayout.SOUTH, emailField);

        return p;
    }

    // -----------------------------------------------------------------------
    // Section 5: Nested Layout Stress Test
    // -----------------------------------------------------------------------

    private static JPanel buildNestedStressTest() {
        JPanel outer = new JPanel(new BorderLayout(4, 4));
        outer.setOpaque(false);
        outer.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // NORTH — FlowLayout with buttons
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        north.setOpaque(false);
        north.add(new JButton("Nested-File"));
        north.add(new JButton("Nested-Edit"));
        north.add(new JButton("Nested-View"));
        outer.add(north, BorderLayout.NORTH);

        // CENTER — JSplitPane
        // Left: GridBagLayout form
        JPanel leftForm = new JPanel(new GridBagLayout());
        leftForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;
        leftForm.add(new JLabel("Depth-Left:"), gbc);
        gbc.gridx = 1;
        leftForm.add(new JTextField("nested-value", 10), gbc);

        // Right: BoxLayout labels
        JPanel rightBox = new JPanel();
        rightBox.setLayout(new BoxLayout(rightBox, BoxLayout.Y_AXIS));
        rightBox.setOpaque(false);
        rightBox.add(new JLabel("Box-Label-1"));
        rightBox.add(new JLabel("Box-Label-2"));
        rightBox.add(new JLabel("Box-Label-3"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftForm, rightBox);
        split.setDividerLocation(300);
        outer.add(split, BorderLayout.CENTER);

        // SOUTH — GridLayout with colored panels
        JPanel south = new JPanel(new GridLayout(1, 3, 4, 0));
        south.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        south.setPreferredSize(new Dimension(700, 40));
        JPanel red = coloredPanel(new Color(0xFFCDD2), "Red-Panel");
        JPanel green = coloredPanel(new Color(0xC8E6C9), "Green-Panel");
        JPanel blue = coloredPanel(new Color(0xBBDEFB), "Blue-Panel");
        south.add(red);
        south.add(green);
        south.add(blue);
        outer.add(south, BorderLayout.SOUTH);

        return outer;
    }

    // -----------------------------------------------------------------------
    // Section 6: Edge Cases
    // -----------------------------------------------------------------------

    private static JPanel buildEdgeCases() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Zero-size panel in FlowLayout
        JPanel flowRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        flowRow.setOpaque(false);
        flowRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        flowRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JPanel zeroSize = new JPanel();
        zeroSize.setPreferredSize(new Dimension(0, 0));
        flowRow.add(zeroSize);
        flowRow.add(new JLabel("After-Zero-Size"));
        p.add(flowRow);
        p.add(vgap(8));

        // Null layout with overlapping labels
        JPanel nullLayout = new JPanel(null);
        nullLayout.setOpaque(false);
        nullLayout.setAlignmentX(Component.LEFT_ALIGNMENT);
        nullLayout.setPreferredSize(new Dimension(300, 30));
        nullLayout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel overlap1 = new JLabel("Overlap-A");
        overlap1.setBounds(10, 2, 100, 20);
        JLabel overlap2 = new JLabel("Overlap-B");
        overlap2.setBounds(50, 2, 100, 20);
        nullLayout.add(overlap1);
        nullLayout.add(overlap2);
        p.add(nullLayout);
        p.add(vgap(8));

        // Empty containers
        JPanel emptyFlow = new JPanel(new FlowLayout());
        emptyFlow.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyFlow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        emptyFlow.setPreferredSize(new Dimension(100, 10));
        p.add(emptyFlow);

        JPanel emptyGrid = new JPanel(new GridLayout(1, 1));
        emptyGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        emptyGrid.setPreferredSize(new Dimension(100, 10));
        p.add(emptyGrid);

        return p;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(new Color(0x2B4C7E));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }

    private static JPanel kpiLabel(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0xDFEAFB));
        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(10f));
        t.setForeground(Color.DARK_GRAY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 16f));
        v.setForeground(new Color(0x2B4C7E));
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(t);
        card.add(v);
        return card;
    }

    private static JPanel coloredPanel(Color bg, String labelText) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setOpaque(true);
        p.add(new JLabel(labelText), BorderLayout.CENTER);
        return p;
    }
}
