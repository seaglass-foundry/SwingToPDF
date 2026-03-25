package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates PDF document metadata: title, author, subject, and keywords.
 *
 * <p>Open the exported PDF in any PDF viewer, then inspect
 * File → Properties (or Document Properties) to see the embedded metadata.
 * The creator field is always set to {@code swingtopdf} automatically.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.MetadataDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class MetadataDemo {

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("swingtopdf-metadata-demo.pdf").toAbsolutePath();

        JPanel[] holder = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holder[0] = buildPanel();
            holder[0].setSize(700, 600);
            holder[0].validate();
            latch.countDown();
        });
        latch.await();

        System.out.println("Exporting to: " + out);
        SwingPdfExporter.from(holder[0])
                .pageSize(PageSize.A4)
                // ---- PDF document metadata ----
                .title("SwingVecPDF Metadata Demo")
                .author("Demo Author")
                .subject("PDF metadata embedding via swing2pdf")
                .keywords("swing2pdf, metadata, PDF, Swing, Java")
                .export(out);
        System.out.println("Done.");
        System.out.println();
        System.out.println("Open the PDF and check File > Properties to see:");
        System.out.println("  Title:    SwingVecPDF Metadata Demo");
        System.out.println("  Author:   Demo Author");
        System.out.println("  Subject:  PDF metadata embedding via swing2pdf");
        System.out.println("  Keywords: swing2pdf, metadata, PDF, Swing, Java");
        System.out.println("  Creator:  swing2pdf  (always set automatically)");

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(out.toFile());
        }
    }

    // -----------------------------------------------------------------------

    private static JPanel buildPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        root.add(heading("PDF Document Metadata Demo"));
        root.add(vgap(12));

        root.add(intro(
                "This PDF was exported with the following metadata. Open File → Properties " +
                "in your PDF viewer to inspect them."));
        root.add(vgap(16));

        root.add(metaRow("Title",    "SwingVecPDF Metadata Demo"));
        root.add(vgap(6));
        root.add(metaRow("Author",   "Demo Author"));
        root.add(vgap(6));
        root.add(metaRow("Subject",  "PDF metadata embedding via swing2pdf"));
        root.add(vgap(6));
        root.add(metaRow("Keywords", "swing2pdf, metadata, PDF, Swing, Java"));
        root.add(vgap(6));
        root.add(metaRow("Creator",  "swing2pdf  (set automatically — always present)"));
        root.add(vgap(24));

        root.add(codeBox(
                "SwingPdfExporter.from(panel)\n" +
                "    .title(\"SwingVecPDF Metadata Demo\")\n" +
                "    .author(\"Demo Author\")\n" +
                "    .subject(\"PDF metadata embedding via swing2pdf\")\n" +
                "    .keywords(\"swing2pdf, metadata, PDF, Swing, Java\")\n" +
                "    .export(path);"
        ));

        return root;
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

    private static JLabel intro(String text) {
        JLabel l = new JLabel("<html><body style='width:580px'>" + text + "</body></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel metaRow(String key, String value) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel keyLabel = new JLabel(key + ":");
        keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD, 11f));
        keyLabel.setForeground(new Color(0x2B4C7E));
        keyLabel.setPreferredSize(new Dimension(90, 24));
        keyLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(valLabel.getFont().deriveFont(11f));

        p.add(keyLabel, BorderLayout.WEST);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private static JPanel codeBox(String code) {
        JTextArea ta = new JTextArea(code);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        ta.setBackground(new Color(0xF5F5F5));
        ta.setForeground(new Color(0x333333));
        ta.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel p = new JPanel(new BorderLayout());
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        p.setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));
        p.add(ta, BorderLayout.CENTER);
        return p;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }
}
