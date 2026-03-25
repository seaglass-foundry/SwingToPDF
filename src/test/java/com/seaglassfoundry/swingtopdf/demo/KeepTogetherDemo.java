package com.seaglassfoundry.swingtopdf.demo;

import javax.swing.*;

import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates the {@code KEEP_TOGETHER} client property that prevents a component
 * from being split across a page boundary.
 *
 * <p>Two PDFs are generated side-by-side:
 * <ol>
 *   <li><b>keep-together ON</b>  — each coloured card is kept whole on one page.</li>
 *   <li><b>keep-together OFF</b> — cards are cut wherever the page break falls.</li>
 * </ol>
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.KeepTogetherDemo \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class KeepTogetherDemo {

    public static void main(String[] args) throws Exception {
        Path outOn  = Paths.get("swingtopdf-keeptogether-on.pdf").toAbsolutePath();
        Path outOff = Paths.get("swingtopdf-keeptogether-off.pdf").toAbsolutePath();

        JPanel[] holderOn  = new JPanel[1];
        JPanel[] holderOff = new JPanel[1];
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            holderOn[0]  = buildPanel(true);
            holderOff[0] = buildPanel(false);
            holderOn[0].setSize(700, 3000);
            holderOn[0].validate();
            holderOff[0].setSize(700, 3000);
            holderOff[0].validate();
            latch.countDown();
        });
        latch.await();

        HeaderFooter footer = HeaderFooter.of("Page {page} of {pages}");

        System.out.println("Exporting KEEP_TOGETHER=ON  → " + outOn);
        SwingPdfExporter.from(holderOn[0])
                .pageSize(PageSize.A4)
                .footer(footer)
                .export(outOn);

        System.out.println("Exporting KEEP_TOGETHER=OFF → " + outOff);
        SwingPdfExporter.from(holderOff[0])
                .pageSize(PageSize.A4)
                .footer(footer)
                .export(outOff);

        System.out.println("Done.  Compare the two PDFs — cards should never be split in the ON version.");

        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(outOn.toFile());
            java.awt.Desktop.getDesktop().open(outOff.toFile());
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Build a tall panel containing coloured "cards". Each card is optionally
     * marked with {@link SwingPdfExporter#KEEP_TOGETHER}.
     */
    private static JPanel buildPanel(boolean keepTogether) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String mode = keepTogether ? "KEEP_TOGETHER = ON" : "KEEP_TOGETHER = OFF";
        root.add(heading("KEEP_TOGETHER Demo — " + mode));
        root.add(vgap(8));
        root.add(description(keepTogether));
        root.add(vgap(12));

        // Build cards sized so that some straddle natural page breaks
        String[] titles = {
            "Q1 — January to March",
            "Q2 — April to June",
            "Q3 — July to September",
            "Q4 — October to December",
        };
        Color[] colors = {
            new Color(0xDFEAFB),
            new Color(0xD6F0E0),
            new Color(0xFFF3CC),
            new Color(0xFFE0E0),
        };
        String[] summaries = {
            "Revenue $1.38M (+15%). Strong performance driven by new product launches.",
            "Revenue $1.54M (+18%). Record quarter boosted by summer promotional campaign.",
            "Revenue $1.21M (+8%).  Seasonal slowdown offset by enterprise contract wins.",
            "Revenue $1.67M (+22%). Best quarter on record; holiday demand exceeded forecast.",
        };

        for (int i = 0; i < titles.length; i++) {
            root.add(card(titles[i], summaries[i], colors[i], keepTogether));
            root.add(vgap(8));
        }

        // Repeat cards to fill multiple pages
        for (int rep = 0; rep < 3; rep++) {
            for (int i = 0; i < titles.length; i++) {
                root.add(card(titles[i] + " (repeat " + (rep + 1) + ")",
                              summaries[i], colors[i], keepTogether));
                root.add(vgap(8));
            }
        }

        return root;
    }

    /**
     * A coloured card with a title, summary, and several detail rows.
     * If {@code keepTogether=true}, the card panel is tagged with
     * {@link SwingPdfExporter#KEEP_TOGETHER} so the engine snaps the
     * page break to just before the card's top edge.
     */
    private static JPanel card(String title, String summary, Color bg,
                                boolean keepTogether) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bg);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(vgap(4));

        JLabel summaryLabel = new JLabel(summary);
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(11f));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(summaryLabel);
        card.add(vgap(8));

        // Detail rows to give the card some height (~180 px total)
        String[][] details = {
            {"Product A", "$320 000", "1 200 units"},
            {"Product B", "$280 000", "980 units"},
            {"Product C", "$415 000", "1 650 units"},
            {"Other",     "$365 000", "varies"},
        };
        for (String[] d : details) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 1));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(fixedLabel(d[0], 130, Font.PLAIN));
            row.add(fixedLabel(d[1], 100, Font.BOLD));
            row.add(fixedLabel(d[2], 100, Font.PLAIN));
            card.add(row);
        }

        if (keepTogether) {
            card.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);
        }

        return card;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        l.setForeground(new Color(0x2B4C7E));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel description(boolean keepTogether) {
        String text = keepTogether
                ? "Each quarterly card is marked with KEEP_TOGETHER = TRUE. " +
                  "Page breaks snap to just before each card, keeping every card intact."
                : "Cards have no KEEP_TOGETHER flag. Page breaks fall at the natural " +
                  "interval and may cut cards in the middle.";
        JLabel l = new JLabel("<html><body style='width:600px'>" + text + "</body></html>");
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel fixedLabel(String text, int width, int style) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(style, 11f));
        l.setPreferredSize(new Dimension(width, 18));
        return l;
    }

    private static Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }
}
