package com.seaglassfoundry.swingtopdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests that the rendering pipeline handles all standard JDK layout managers
 * correctly, including edge cases like zero-size children, null layout with
 * overlapping components, and deeply nested mixed layouts.
 */
class LayoutManagerTest {

    @Test
    void cardLayout_uiSnapshot_onlyShowsVisibleCard(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel cards = buildCardLayout();
        JPanel wrapper = wrap(cards, 400, 100);

        Path pdf = tmp.resolve("card-snapshot.pdf");
        SwingPdfExporter.from(wrapper)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.UI_SNAPSHOT)
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Active-Card-Text");
            assertThat(text).doesNotContain("Hidden-Card-Alpha");
            assertThat(text).doesNotContain("Hidden-Card-Beta");
        }
    }

    @Test
    void cardLayout_dataReport_onlyShowsVisibleCard(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel cards = buildCardLayout();
        JPanel wrapper = wrap(cards, 400, 100);

        Path pdf = tmp.resolve("card-datareport.pdf");
        SwingPdfExporter.from(wrapper)
                .pageSize(PageSize.A4)
                .exportMode(ExportMode.DATA_REPORT)
                .export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            // DATA_REPORT also only shows the visible card — this is a known limitation
            assertThat(text).contains("Active-Card-Text");
            assertThat(text).doesNotContain("Hidden-Card-Alpha");
            assertThat(text).doesNotContain("Hidden-Card-Beta");
        }
    }

    @Test
    void groupLayout_allComponentsRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel form = new JPanel();
        GroupLayout gl = new GroupLayout(form);
        form.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel nameLabel = new JLabel("GL-Name:");
        JTextField nameField = new JTextField("GL-Alice");
        JLabel cityLabel = new JLabel("GL-City:");
        JTextField cityField = new JTextField("GL-Portland");
        JLabel roleLabel = new JLabel("GL-Role:");
        JTextField roleField = new JTextField("GL-Engineer");

        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(nameLabel).addComponent(cityLabel).addComponent(roleLabel))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(nameField, 150, 150, 250)
                        .addComponent(cityField, 150, 150, 250)
                        .addComponent(roleField, 150, 150, 250)));

        gl.setVerticalGroup(gl.createSequentialGroup()
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(nameLabel).addComponent(nameField))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(cityLabel).addComponent(cityField))
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(roleLabel).addComponent(roleField)));

        JPanel wrapper = wrap(form, 400, 120);

        Path pdf = tmp.resolve("grouplayout.pdf");
        SwingPdfExporter.from(wrapper).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("GL-Name:");
            assertThat(text).contains("GL-Alice");
            assertThat(text).contains("GL-City:");
            assertThat(text).contains("GL-Portland");
            assertThat(text).contains("GL-Role:");
            assertThat(text).contains("GL-Engineer");
        }
    }

    @Test
    void springLayout_allComponentsRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel p = new JPanel();
        SpringLayout sl = new SpringLayout();
        p.setLayout(sl);

        JLabel emailLabel = new JLabel("SL-Email:");
        JTextField emailField = new JTextField("SL-test@example.com");
        emailField.setColumns(15);
        JLabel phoneLabel = new JLabel("SL-Phone:");
        JTextField phoneField = new JTextField("SL-555-1234");
        phoneField.setColumns(15);

        p.add(emailLabel);
        p.add(emailField);
        p.add(phoneLabel);
        p.add(phoneField);

        sl.putConstraint(SpringLayout.WEST, emailLabel, 8, SpringLayout.WEST, p);
        sl.putConstraint(SpringLayout.NORTH, emailLabel, 8, SpringLayout.NORTH, p);
        sl.putConstraint(SpringLayout.WEST, emailField, 8, SpringLayout.EAST, emailLabel);
        sl.putConstraint(SpringLayout.NORTH, emailField, 5, SpringLayout.NORTH, p);

        sl.putConstraint(SpringLayout.WEST, phoneLabel, 8, SpringLayout.WEST, p);
        sl.putConstraint(SpringLayout.NORTH, phoneLabel, 8, SpringLayout.SOUTH, emailLabel);
        sl.putConstraint(SpringLayout.WEST, phoneField, 8, SpringLayout.EAST, phoneLabel);
        sl.putConstraint(SpringLayout.NORTH, phoneField, 5, SpringLayout.SOUTH, emailField);

        JPanel wrapper = wrap(p, 400, 80);

        Path pdf = tmp.resolve("springlayout.pdf");
        SwingPdfExporter.from(wrapper).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("SL-Email:");
            assertThat(text).contains("SL-test@example.com");
            assertThat(text).contains("SL-Phone:");
            assertThat(text).contains("SL-555-1234");
        }
    }

    @Test
    void zeroSizeChild_doesNotCrash(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        flow.setBackground(Color.WHITE);
        flow.setOpaque(true);
        JPanel zeroSize = new JPanel();
        zeroSize.setPreferredSize(new Dimension(0, 0));
        flow.add(zeroSize);
        flow.add(new JLabel("ZeroSizeNeighbor"));

        JPanel wrapper = wrap(flow, 300, 50);

        Path pdf = tmp.resolve("zerosize.pdf");
        SwingPdfExporter.from(wrapper).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void overlappingComponents_allTextRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        JPanel nullLayout = new JPanel(null);
        nullLayout.setBackground(Color.WHITE);
        nullLayout.setOpaque(true);
        JLabel a = new JLabel("OverlapAlpha");
        a.setBounds(10, 5, 120, 20);
        JLabel b = new JLabel("OverlapBeta");
        b.setBounds(60, 5, 120, 20);
        nullLayout.add(a);
        nullLayout.add(b);

        JPanel wrapper = wrap(nullLayout, 300, 40);

        Path pdf = tmp.resolve("overlap.pdf");
        SwingPdfExporter.from(wrapper).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("OverlapAlpha");
            assertThat(text).contains("OverlapBeta");
        }
    }

    @Test
    void deeplyNestedMixedLayouts_allTextRendered(@TempDir Path tmp) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Skipped in headless environment");

        // BorderLayout root
        JPanel root = new JPanel(new BorderLayout(4, 4));
        root.setBackground(Color.WHITE);
        root.setOpaque(true);

        // NORTH — FlowLayout
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(new JLabel("DeepNorth"));
        root.add(north, BorderLayout.NORTH);

        // CENTER — GridBagLayout
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(2, 4, 2, 4);
        center.add(new JLabel("DeepCenter"), gbc);
        root.add(center, BorderLayout.CENTER);

        // SOUTH — BoxLayout inside GridLayout
        JPanel south = new JPanel(new GridLayout(1, 2));
        JPanel box1 = new JPanel();
        box1.setLayout(new BoxLayout(box1, BoxLayout.Y_AXIS));
        box1.setOpaque(false);
        box1.add(new JLabel("DeepSouth-Box1"));
        JPanel box2 = new JPanel();
        box2.setLayout(new BoxLayout(box2, BoxLayout.Y_AXIS));
        box2.setOpaque(false);
        box2.add(new JLabel("DeepSouth-Box2"));
        south.add(box1);
        south.add(box2);
        root.add(south, BorderLayout.SOUTH);

        JPanel wrapper = wrap(root, 500, 200);

        Path pdf = tmp.resolve("deepnested.pdf");
        SwingPdfExporter.from(wrapper).pageSize(PageSize.A4).export(pdf);

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("DeepNorth");
            assertThat(text).contains("DeepCenter");
            assertThat(text).contains("DeepSouth-Box1");
            assertThat(text).contains("DeepSouth-Box2");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JPanel buildCardLayout() {
        JPanel cards = new JPanel(new CardLayout());

        JPanel active = new JPanel(new BorderLayout());
        active.add(new JLabel("Active-Card-Text"), BorderLayout.CENTER);
        cards.add(active, "Active");

        JPanel hidden1 = new JPanel(new BorderLayout());
        hidden1.add(new JLabel("Hidden-Card-Alpha"), BorderLayout.CENTER);
        cards.add(hidden1, "Hidden1");

        JPanel hidden2 = new JPanel(new BorderLayout());
        hidden2.add(new JLabel("Hidden-Card-Beta"), BorderLayout.CENTER);
        cards.add(hidden2, "Hidden2");

        ((CardLayout) cards.getLayout()).show(cards, "Active");
        return cards;
    }

    private static JPanel wrap(JComponent content, int w, int h) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setOpaque(true);
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(w, h));
        wrapper.setSize(w, h);
        wrapper.doLayout();
        wrapper.validate();
        return wrapper;
    }
}
