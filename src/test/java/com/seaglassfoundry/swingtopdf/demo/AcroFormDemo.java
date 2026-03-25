package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.awt.Desktop;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates a fillable PDF form exercising every AcroForm field type and
 * saves it to {@code swingtopdf-acroform-demo.pdf} in the project root.
 *
 * <p>Open the PDF in Adobe Acrobat, Foxit, or any standards-compliant viewer
 * to verify that the fields are interactive and pre-populated with the correct
 * values.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.AcroFormDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class AcroFormDemo {

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("swingtopdf-acroform-demo.pdf");

        JPanel form = buildForm();

        SwingPdfExporter.from(form)
                .pageSize(PageSize.A4)
                .enableAcroForm()
                .export(out);

        System.out.println("Written: " + out.toAbsolutePath());

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(out.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JPanel buildForm() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ---- Text fields ---------------------------------------------------
        JPanel textSection = titledSection("Text fields");
        textSection.setLayout(new GridLayout(0, 2, 8, 6));

        textSection.add(label("First name:"));
        JTextField firstName = named(new JTextField("Jane", 20), "firstName");
        textSection.add(firstName);

        textSection.add(label("Last name:"));
        JTextField lastName = named(new JTextField("Smith", 20), "lastName");
        textSection.add(lastName);

        textSection.add(label("Email:"));
        JTextField email = named(new JTextField("jane.smith@example.com", 20), "email");
        textSection.add(email);

        textSection.add(label("Password:"));
        JPasswordField password = named(new JPasswordField("hunter2", 20), "password");
        textSection.add(password);

        textSection.add(label("Notes:"));
        JTextArea notes = named(new JTextArea("Multi-line notes\ngo here.", 5, 20), "notes");
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        textSection.add(notes); // no JScrollPane — PDF multiline field scrolls natively

        root.add(textSection);
        root.add(Box.createVerticalStrut(12));

        // ---- Checkboxes ----------------------------------------------------
        JPanel checkSection = titledSection("Checkboxes");
        checkSection.setLayout(new GridLayout(0, 2, 8, 4));

        JCheckBox newsletter   = named(new JCheckBox("Subscribe to newsletter", true),  "newsletter");
        JCheckBox termsAgreed  = named(new JCheckBox("I agree to the terms",     true),  "termsAgreed");
        JCheckBox notifications = named(new JCheckBox("Email notifications",      false), "notifications");
        JCheckBox darkMode     = named(new JCheckBox("Dark mode",                 false), "darkMode");

        checkSection.add(newsletter);
        checkSection.add(termsAgreed);
        checkSection.add(notifications);
        checkSection.add(darkMode);

        root.add(checkSection);
        root.add(Box.createVerticalStrut(12));

        // ---- Radio buttons — two independent groups ------------------------
        JPanel radioSection = titledSection("Radio buttons");
        radioSection.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 16);

        // Group 1: size
        gbc.gridx = 0; gbc.gridy = 0;
        radioSection.add(label("Shirt size:"), gbc);

        ButtonGroup sizeGroup = new ButtonGroup();
        String[] sizes = {"Small", "Medium", "Large", "XL"};
        for (int i = 0; i < sizes.length; i++) {
            JRadioButton rb = named(new JRadioButton(sizes[i], i == 1), "size_" + sizes[i].toLowerCase());
            sizeGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        // Group 2: payment
        gbc.gridx = 0; gbc.gridy = 1;
        radioSection.add(label("Payment:"), gbc);

        ButtonGroup payGroup = new ButtonGroup();
        String[] payments = {"Credit card", "PayPal", "Bank transfer"};
        for (int i = 0; i < payments.length; i++) {
            JRadioButton rb = named(new JRadioButton(payments[i], i == 0), "payment_" + i);
            payGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        root.add(radioSection);
        root.add(Box.createVerticalStrut(12));

        // ---- Combo boxes ---------------------------------------------------
        JPanel comboSection = titledSection("Combo boxes");
        comboSection.setLayout(new GridLayout(0, 2, 8, 6));

        comboSection.add(label("Country:"));
        JComboBox<String> country = named(
                new JComboBox<>(new String[]{"United States", "Canada", "United Kingdom", "Australia", "Other"}),
                "country");
        country.setSelectedItem("Canada");
        comboSection.add(country);

        comboSection.add(label("Priority:"));
        JComboBox<String> priority = named(
                new JComboBox<>(new String[]{"Low", "Medium", "High", "Critical"}),
                "priority");
        priority.setSelectedItem("High");
        comboSection.add(priority);

        comboSection.add(label("Custom (editable):"));
        JComboBox<String> custom = named(
                new JComboBox<>(new String[]{"Option A", "Option B", "Option C"}),
                "customOption");
        custom.setEditable(true);
        custom.setSelectedItem("My custom value");
        comboSection.add(custom);

        root.add(comboSection);

        // Fix size so layout works without a real screen
        root.setPreferredSize(new Dimension(560, 660));
        root.setSize(560, 660);
        root.doLayout();
        root.validate();

        return root;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static <T extends JComponent> T named(T comp, String name) {
        comp.setName(name);
        return comp;
    }

    private static JPanel titledSection(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP));
        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }
}
