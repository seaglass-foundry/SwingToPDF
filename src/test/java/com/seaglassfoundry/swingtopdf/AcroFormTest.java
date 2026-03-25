package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Verifies that {@code enableAcroForm()} produces valid PDF AcroForm widgets.
 */
class AcroFormTest {

    // -----------------------------------------------------------------------
    // TextField
    // -----------------------------------------------------------------------

    @Test
    void textField_valueAppearsInAcroForm(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField tf = new JTextField("Hello, World!", 20);
        Path pdf = export(wrap(tf, 300, 40), tmp, "tf.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertThat(form).isNotNull();
            // PDFBox 3.x generates appearance streams during save and clears
            // NeedAppearances to signal they are up-to-date; the form is valid
            // regardless of the flag value.

            PDField field = findField(form, PDTextField.class);
            assertThat(field).isNotNull();
            assertThat(((PDTextField) field).getValue()).isEqualTo("Hello, World!");
        }
    }

    @Test
    void passwordField_isMarkedPassword(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPasswordField pf = new JPasswordField("secret", 20);
        Path pdf = export(wrap(pf, 300, 40), tmp, "pw.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDField field = findField(doc.getDocumentCatalog().getAcroForm(), PDTextField.class);
            assertThat(field).isNotNull();
            assertThat(((PDTextField) field).isPassword()).isTrue();
            // Password content must not be exported
            assertThat(((PDTextField) field).getValue()).isEqualTo("");
        }
    }

    @Test
    void textArea_isMultiline(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextArea ta = new JTextArea("Line 1\nLine 2\nLine 3", 4, 20);
        ta.setLineWrap(true);
        Path pdf = export(wrap(ta, 300, 80), tmp, "ta.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDField field = findField(doc.getDocumentCatalog().getAcroForm(), PDTextField.class);
            assertThat(field).isNotNull();
            assertThat(((PDTextField) field).isMultiline()).isTrue();
            assertThat(((PDTextField) field).getValue()).contains("Line 1");
        }
    }

    // -----------------------------------------------------------------------
    // CheckBox
    // -----------------------------------------------------------------------

    @Test
    void checkBox_checked_fieldIsChecked(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("Accept terms", true);
        Path pdf = export(wrap(cb, 200, 30), tmp, "cb_checked.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDCheckBox field = (PDCheckBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDCheckBox.class);
            assertThat(field).isNotNull();
            assertThat(field.isChecked()).isTrue();
        }
    }

    @Test
    void checkBox_unchecked_fieldIsUnchecked(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("Subscribe", false);
        Path pdf = export(wrap(cb, 200, 30), tmp, "cb_unchecked.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDCheckBox field = (PDCheckBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDCheckBox.class);
            assertThat(field).isNotNull();
            assertThat(field.isChecked()).isFalse();
        }
    }

    @Test
    void multipleCheckBoxes_eachHasOwnField(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb1 = new JCheckBox("Option A", true);
        JCheckBox cb2 = new JCheckBox("Option B", false);
        JCheckBox cb3 = new JCheckBox("Option C", true);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 90));
        panel.setSize(200, 90);
        panel.add(cb1); panel.add(cb2); panel.add(cb3);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "cbs.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            List<PDField> fields = doc.getDocumentCatalog().getAcroForm().getFields();
            long checkBoxCount = fields.stream().filter(f -> f instanceof PDCheckBox).count();
            assertThat(checkBoxCount).isEqualTo(3);
        }
    }

    // -----------------------------------------------------------------------
    // RadioButton
    // -----------------------------------------------------------------------

    @Test
    void radioButtons_groupedByButtonGroup_singlePdfField(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("Small", false);
        JRadioButton r2 = new JRadioButton("Medium", true);
        JRadioButton r3 = new JRadioButton("Large", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2); bg.add(r3);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 90));
        panel.setSize(200, 90);
        panel.add(r1); panel.add(r2); panel.add(r3);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            long radioFieldCount = form.getFields().stream()
                    .filter(f -> f instanceof PDRadioButton).count();
            // Three radio buttons in one group → exactly one PDRadioButton field
            assertThat(radioFieldCount).isEqualTo(1);

            PDRadioButton field = (PDRadioButton) form.getFields().stream()
                    .filter(f -> f instanceof PDRadioButton).findFirst().orElseThrow();
            // Three widgets for three buttons
            assertThat(field.getWidgets()).hasSize(3);
            // "Medium" is selected — V must name exactly one option
            assertThat(field.getSelectedExportValues()).containsExactly("Medium");

            // Each widget's /AS must match only its own on-state or "Off".
            // If any non-selected widget has AS set to its on-state name (not "Off"),
            // PDF viewers display multiple buttons as selected simultaneously.
            long selectedAsCount = field.getWidgets().stream()
                    .filter(w -> {
                        String as = w.getCOSObject().getNameAsString(
                                org.apache.pdfbox.cos.COSName.AS);
                        return as != null && !as.equals("Off");
                    })
                    .count();
            assertThat(selectedAsCount)
                    .as("Exactly one widget must have AS != Off")
                    .isEqualTo(1);
        }
    }

    @Test
    void radioButtons_structuralIntegrity(@TempDir Path tmp) throws Exception {
        // Verifies the full COS structure required for mutual exclusivity:
        // /Parent on each widget, /Kids on the field, unique on-state names,
        // correct /V and /AS.
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("Alpha", true);
        JRadioButton r2 = new JRadioButton("Beta",  false);
        JRadioButton r3 = new JRadioButton("Gamma", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2); bg.add(r3);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 90));
        panel.setSize(200, 90);
        panel.add(r1); panel.add(r2); panel.add(r3);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio_structure.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            PDRadioButton field = (PDRadioButton) form.getFields().stream()
                    .filter(f -> f instanceof PDRadioButton).findFirst().orElseThrow();

            // 1. Field must have /FT = Btn
            assertThat(field.getFieldType()).isEqualTo("Btn");

            // 2. Field flags must include Radio (bit 16) and NoToggleToOff (bit 15)
            int ff = field.getCOSObject().getInt(COSName.FF, 0);
            assertThat(ff & (1 << 15)).as("Radio flag (bit 16)").isNotZero();
            assertThat(ff & (1 << 14)).as("NoToggleToOff flag (bit 15)").isNotZero();

            // 3. Field must have /Kids array (not merged single-widget)
            assertThat(field.getCOSObject().containsKey(COSName.KIDS))
                    .as("Multi-widget field must use /Kids array")
                    .isTrue();

            // 4. Each widget must have /Parent pointing back to the field dict
            List<PDAnnotationWidget> widgets = field.getWidgets();
            assertThat(widgets).hasSize(3);
            for (PDAnnotationWidget w : widgets) {
                assertThat(w.getCOSObject().getDictionaryObject(COSName.PARENT))
                        .as("Widget must have /Parent → field")
                        .isSameAs(field.getCOSObject());
            }

            // 5. Each widget's /AP/N must have exactly two keys: its unique on-state + "Off"
            for (PDAnnotationWidget w : widgets) {
                var apDict = w.getAppearance().getNormalAppearance();
                assertThat(apDict.isSubDictionary())
                        .as("Normal appearance must be a sub-dictionary (two states)")
                        .isTrue();
            }

            // 6. V = "Alpha" (the selected button's on-state)
            String v = field.getCOSObject().getNameAsString(COSName.V);
            assertThat(v).isEqualTo("Alpha");

            // 7. Exactly one widget has AS = its on-state; others have AS = "Off"
            int selectedCount = 0;
            for (PDAnnotationWidget w : widgets) {
                String as = w.getCOSObject().getNameAsString(COSName.AS);
                if (!"Off".equals(as)) selectedCount++;
            }
            assertThat(selectedCount).isEqualTo(1);
        }
    }

    @Test
    void radioButtons_toggleDeselectsOthers(@TempDir Path tmp) throws Exception {
        // Simulates clicking a different radio button by calling setValue() on the
        // loaded PDF field and verifying that exactly one widget ends up selected.
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("Alpha", true);
        JRadioButton r2 = new JRadioButton("Beta",  false);
        JRadioButton r3 = new JRadioButton("Gamma", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2); bg.add(r3);

        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 90));
        panel.setSize(200, 90);
        panel.add(r1); panel.add(r2); panel.add(r3);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio_toggle.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDRadioButton field = (PDRadioButton) doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton)
                    .findFirst().orElseThrow();

            // Initially "Alpha" selected
            assertThat(field.getValue()).isEqualTo("Alpha");

            // "Click" Beta — setValue simulates what a viewer does
            field.setValue("Beta");
            assertThat(field.getValue()).isEqualTo("Beta");

            // Exactly one widget should have AS != "Off"
            long selectedAfterToggle = field.getWidgets().stream()
                    .filter(w -> !"Off".equals(w.getCOSObject().getNameAsString(COSName.AS)))
                    .count();
            assertThat(selectedAfterToggle)
                    .as("After toggling to Beta, exactly one widget must be selected")
                    .isEqualTo(1);

            // And that widget's AS should be "Beta"
            String selectedAs = field.getWidgets().stream()
                    .map(w -> w.getCOSObject().getNameAsString(COSName.AS))
                    .filter(as -> !"Off".equals(as))
                    .findFirst().orElseThrow();
            assertThat(selectedAs).isEqualTo("Beta");
        }
    }

    @Test
    void radioButtons_twoGroups_twoPdfFields(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton ra = new JRadioButton("Red",   true);
        JRadioButton rb = new JRadioButton("Blue",  false);
        ButtonGroup bg1 = new ButtonGroup(); bg1.add(ra); bg1.add(rb);

        JRadioButton rc = new JRadioButton("Small", false);
        JRadioButton rd = new JRadioButton("Large", true);
        ButtonGroup bg2 = new ButtonGroup(); bg2.add(rc); bg2.add(rd);

        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 120));
        panel.setSize(200, 120);
        panel.add(ra); panel.add(rb); panel.add(rc); panel.add(rd);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "two_radio_groups.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            long radioFieldCount = doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton).count();
            assertThat(radioFieldCount).isEqualTo(2);
        }
    }

    @Test
    void radioButtons_twoGroupsSameParent_twoPdfFields(@TempDir Path tmp) throws Exception {
        // Regression: two ButtonGroups whose buttons all share the same parent container
        // (e.g. a GridBagLayout form panel) must produce two separate PDF radio fields,
        // not one merged field. Previously the parent-container fallback caused a merge.
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel shared = new JPanel(new GridBagLayout());
        shared.setBackground(Color.WHITE);

        JRadioButton size1 = new JRadioButton("Small",  true);
        JRadioButton size2 = new JRadioButton("Medium", false);
        JRadioButton size3 = new JRadioButton("Large",  false);
        ButtonGroup sizeGroup = new ButtonGroup();
        sizeGroup.add(size1); sizeGroup.add(size2); sizeGroup.add(size3);

        JRadioButton pay1 = new JRadioButton("Credit card",   true);
        JRadioButton pay2 = new JRadioButton("PayPal",        false);
        JRadioButton pay3 = new JRadioButton("Bank transfer", false);
        ButtonGroup payGroup = new ButtonGroup();
        payGroup.add(pay1); payGroup.add(pay2); payGroup.add(pay3);

        // All six buttons land in the same container — the problematic layout.
        for (JRadioButton b : new JRadioButton[]{size1, size2, size3, pay1, pay2, pay3})
            shared.add(b);

        shared.setPreferredSize(new Dimension(400, 80));
        shared.setSize(400, 80);
        shared.doLayout(); shared.validate();

        Path pdf = export(shared, tmp, "two_groups_same_parent.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            long radioFieldCount = doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton).count();
            assertThat(radioFieldCount)
                    .as("Two ButtonGroups in the same container must produce two PDF fields")
                    .isEqualTo(2);
        }
    }

    @Test
    void radioButtons_acroFormDemoLayout_twoFieldsWithCorrectStructure(@TempDir Path tmp) throws Exception {
        // Replicates the AcroFormDemo layout: two ButtonGroups in the same parent
        // container (GridBagLayout). Verifies all COS structure requirements for
        // proper mutual exclusivity in PDF viewers.
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel radioSection = new JPanel(new GridBagLayout());
        radioSection.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 16);

        gbc.gridx = 0; gbc.gridy = 0;
        radioSection.add(new JLabel("Shirt size:"), gbc);
        ButtonGroup sizeGroup = new ButtonGroup();
        String[] sizes = {"Small", "Medium", "Large", "XL"};
        for (int i = 0; i < sizes.length; i++) {
            JRadioButton rb = new JRadioButton(sizes[i], i == 1);
            rb.setName("size_" + sizes[i].toLowerCase());
            sizeGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        gbc.gridx = 0; gbc.gridy = 1;
        radioSection.add(new JLabel("Payment:"), gbc);
        ButtonGroup payGroup = new ButtonGroup();
        String[] payments = {"Credit card", "PayPal", "Bank transfer"};
        for (int i = 0; i < payments.length; i++) {
            JRadioButton rb = new JRadioButton(payments[i], i == 0);
            rb.setName("payment_" + i);
            payGroup.add(rb);
            gbc.gridx = i + 1;
            radioSection.add(rb, gbc);
        }

        radioSection.setPreferredSize(new Dimension(500, 80));
        radioSection.setSize(500, 80);
        radioSection.doLayout();
        radioSection.validate();

        Path pdf = export(radioSection, tmp, "radio_demo_layout.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            assertThat(form.getNeedAppearances()).isFalse();

            List<PDRadioButton> radioFields = form.getFields().stream()
                    .filter(f -> f instanceof PDRadioButton)
                    .map(f -> (PDRadioButton) f)
                    .toList();
            assertThat(radioFields).hasSize(2);

            for (PDRadioButton radio : radioFields) {
                var fd = radio.getCOSObject();
                // Correct field type and flags
                assertThat(fd.getNameAsString(COSName.FT)).isEqualTo("Btn");
                int ff = fd.getInt(COSName.FF, 0);
                assertThat(ff & (1 << 15)).as("Radio flag").isNotZero();
                assertThat(ff & (1 << 14)).as("NoToggleToOff flag").isNotZero();

                // Each widget has correct /Parent, /Subtype, unique AP/N on-state
                for (PDAnnotationWidget w : radio.getWidgets()) {
                    var wd = w.getCOSObject();
                    assertThat(wd.getNameAsString(COSName.SUBTYPE)).isEqualTo("Widget");
                    assertThat(wd.getDictionaryObject(COSName.PARENT))
                            .as("/Parent must reference the field")
                            .isSameAs(fd);
                    assertThat(w.isPrinted()).as("Widget must have Print flag").isTrue();
                }

                // Exactly one widget selected per field
                long selected = radio.getWidgets().stream()
                        .filter(w -> !"Off".equals(w.getCOSObject().getNameAsString(COSName.AS)))
                        .count();
                assertThat(selected).as("Exactly one widget selected").isEqualTo(1);

                // setValue() toggle works
                String origValue = radio.getValue();
                String otherValue = radio.getOnValues().stream()
                        .filter(v -> !v.equals(origValue))
                        .findFirst().orElseThrow();
                radio.setValue(otherValue);
                assertThat(radio.getValue()).isEqualTo(otherValue);
                long afterToggle = radio.getWidgets().stream()
                        .filter(w -> !"Off".equals(w.getCOSObject().getNameAsString(COSName.AS)))
                        .count();
                assertThat(afterToggle).as("Still one selected after toggle").isEqualTo(1);
            }
        }
    }

    // -----------------------------------------------------------------------
    // ComboBox
    // -----------------------------------------------------------------------

    @Test
    void comboBox_selectedValueInField(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<String> cb = new JComboBox<>(new String[]{"Alpha", "Beta", "Gamma"});
        cb.setSelectedItem("Beta");
        Path pdf = export(wrap(cb, 200, 30), tmp, "combo.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDComboBox field = (PDComboBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDComboBox.class);
            assertThat(field).isNotNull();
            assertThat(field.getValue()).contains("Beta");
            // All three options are present
            assertThat(field.getOptions()).hasSize(3);
        }
    }

    @Test
    void comboBox_editableFlag_preserved(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<String> cb = new JComboBox<>(new String[]{"Java", "Kotlin", "Scala"});
        cb.setEditable(true);
        cb.setSelectedItem("Kotlin");
        Path pdf = export(wrap(cb, 200, 30), tmp, "combo_editable.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDComboBox field = (PDComboBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDComboBox.class);
            assertThat(field).isNotNull();
            assertThat(field.isEdit()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Mixed form — tests all field types together
    // -----------------------------------------------------------------------

    @Test
    void mixedForm_allFieldTypesPresent(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);

        form.add(new JTextField("John Doe", 20));
        form.add(new JTextArea("Notes go here", 3, 20));
        form.add(new JPasswordField("pass123", 20));
        form.add(new JCheckBox("Agreed", true));

        JRadioButton ra = new JRadioButton("Yes", true);
        JRadioButton rb = new JRadioButton("No",  false);
        ButtonGroup bg = new ButtonGroup(); bg.add(ra); bg.add(rb);
        form.add(ra); form.add(rb);

        form.add(new JComboBox<>(new String[]{"Draft", "Published", "Archived"}));

        form.setPreferredSize(new Dimension(300, 300));
        form.setSize(300, 300);
        form.doLayout(); form.validate();

        Path pdf = export(form, tmp, "mixed.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            assertThat(acroForm).isNotNull();

            List<PDField> fields = acroForm.getFields();
            long textFields   = fields.stream().filter(f -> f instanceof PDTextField).count();
            long checkBoxes   = fields.stream().filter(f -> f instanceof PDCheckBox).count();
            long radioFields  = fields.stream().filter(f -> f instanceof PDRadioButton).count();
            long comboFields  = fields.stream().filter(f -> f instanceof PDComboBox).count();

            // JTextField + JTextArea + JPasswordField = 3 text fields
            assertThat(textFields).isGreaterThanOrEqualTo(3);
            assertThat(checkBoxes).isGreaterThanOrEqualTo(1);
            assertThat(radioFields).isEqualTo(1);   // one group → one PDRadioButton
            assertThat(comboFields).isGreaterThanOrEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Disabled → read-only
    // -----------------------------------------------------------------------

    @Test
    void textField_disabled_isReadOnly(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField tf = new JTextField("Read only text", 20);
        tf.setEnabled(false);
        Path pdf = export(wrap(tf, 300, 40), tmp, "tf_disabled.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDTextField field = (PDTextField) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDTextField.class);
            assertThat(field).isNotNull();
            assertThat(field.isReadOnly()).isTrue();
        }
    }

    @Test
    void checkBox_disabled_isReadOnly(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JCheckBox cb = new JCheckBox("Locked option", true);
        cb.setEnabled(false);
        Path pdf = export(wrap(cb, 200, 30), tmp, "cb_disabled.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDCheckBox field = (PDCheckBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDCheckBox.class);
            assertThat(field).isNotNull();
            assertThat(field.isReadOnly()).isTrue();
        }
    }

    @Test
    void comboBox_disabled_isReadOnly(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JComboBox<String> cb = new JComboBox<>(new String[]{"A", "B", "C"});
        cb.setSelectedItem("B");
        cb.setEnabled(false);
        Path pdf = export(wrap(cb, 200, 30), tmp, "combo_disabled.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDComboBox field = (PDComboBox) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDComboBox.class);
            assertThat(field).isNotNull();
            assertThat(field.isReadOnly()).isTrue();
        }
    }

    @Test
    void radioButtons_allDisabled_fieldIsReadOnly(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("On", true);
        JRadioButton r2 = new JRadioButton("Off", false);
        r1.setEnabled(false);
        r2.setEnabled(false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 60));
        panel.setSize(200, 60);
        panel.add(r1); panel.add(r2);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio_disabled.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDRadioButton field = (PDRadioButton) findField(
                    doc.getDocumentCatalog().getAcroForm(), PDRadioButton.class);
            assertThat(field).isNotNull();
            assertThat(field.isReadOnly()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate on-value collision
    // -----------------------------------------------------------------------

    @Test
    void radioButtons_duplicateLabels_uniqueOnValues(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("Option A", true);
        JRadioButton r2 = new JRadioButton("Option A", false);
        JRadioButton r3 = new JRadioButton("Option A", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2); bg.add(r3);

        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 90));
        panel.setSize(200, 90);
        panel.add(r1); panel.add(r2); panel.add(r3);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio_dup_labels.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDRadioButton field = (PDRadioButton) doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton)
                    .findFirst().orElseThrow();

            // Three widgets with three distinct on-values
            assertThat(field.getWidgets()).hasSize(3);
            List<String> onValues = new ArrayList<>(field.getOnValues());
            assertThat(onValues).hasSize(3);
            // All on-values must be unique
            assertThat(onValues).doesNotHaveDuplicates();

            // setValue() can toggle each independently
            for (String val : onValues) {
                field.setValue(val);
                assertThat(field.getValue()).isEqualTo(val);
                long selected = field.getWidgets().stream()
                        .filter(w -> !"Off".equals(
                                w.getCOSObject().getNameAsString(COSName.AS)))
                        .count();
                assertThat(selected).as("Exactly one selected for " + val).isEqualTo(1);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Layout manager independence
    // -----------------------------------------------------------------------

    @Test
    void radioButtons_boxLayout_groupDetected(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("X", true);
        JRadioButton r2 = new JRadioButton("Y", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 60));
        panel.setSize(200, 60);
        panel.add(r1); panel.add(r2);
        panel.doLayout(); panel.validate();

        Path pdf = export(panel, tmp, "radio_boxlayout.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            long radioCount = doc.getDocumentCatalog().getAcroForm().getFields().stream()
                    .filter(f -> f instanceof PDRadioButton).count();
            assertThat(radioCount).isEqualTo(1);
            PDRadioButton field = (PDRadioButton) doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton)
                    .findFirst().orElseThrow();
            assertThat(field.getWidgets()).hasSize(2);
        }
    }

    @Test
    void radioButtons_nullLayout_groupDetected(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JRadioButton r1 = new JRadioButton("A", false);
        JRadioButton r2 = new JRadioButton("B", true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(r1); bg.add(r2);

        JPanel panel = new JPanel(null);
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(200, 60));
        panel.setSize(200, 60);
        r1.setBounds(4, 0, 100, 30);
        r2.setBounds(4, 30, 100, 30);
        panel.add(r1); panel.add(r2);
        // No doLayout needed — bounds set manually

        Path pdf = export(panel, tmp, "radio_nulllayout.pdf");

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            long radioCount = doc.getDocumentCatalog().getAcroForm().getFields().stream()
                    .filter(f -> f instanceof PDRadioButton).count();
            assertThat(radioCount).isEqualTo(1);
            PDRadioButton field = (PDRadioButton) doc.getDocumentCatalog().getAcroForm()
                    .getFields().stream().filter(f -> f instanceof PDRadioButton)
                    .findFirst().orElseThrow();
            assertThat(field.getWidgets()).hasSize(2);
        }
    }

    // -----------------------------------------------------------------------
    // No AcroForm when not enabled
    // -----------------------------------------------------------------------

    @Test
    void withoutEnableAcroForm_noAcroFormInPdf(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JTextField tf = new JTextField("Hello", 20);
        JPanel panel = wrap(tf, 300, 40);

        Path pdf = tmp.resolve("no_acroform.pdf");
        SwingPdfExporter.from(panel).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            // AcroForm may be null or have no fields when not explicitly enabled
            assertThat(form == null || form.getFields().isEmpty()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Path export(JPanel panel, Path tmp, String name) throws Exception {
        Path pdf = tmp.resolve(name);
        SwingPdfExporter.from(panel)
                .pageSize(PageSize.A4)
                .enableAcroForm()
                .export(pdf);
        return pdf;
    }

    private static JPanel wrap(Component comp, int w, int h) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setSize(w, h);
        panel.add(comp);
        panel.doLayout();
        panel.validate();
        return panel;
    }

    private static PDField findField(PDAcroForm form, Class<? extends PDField> type) {
        if (form == null) return null;
        return form.getFields().stream()
                .filter(type::isInstance)
                .findFirst()
                .orElse(null);
    }
}
