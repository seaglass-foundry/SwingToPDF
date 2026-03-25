package com.seaglassfoundry.swingtopdf.demo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a professional sales brochure PDF for swingtopdf.
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass=com.seaglassfoundry.swingtopdf.demo.GenerateSalesBrochure \
 *     -Dexec.classpathScope=test
 * </pre>
 */
public class GenerateSalesBrochure {

    // Colors (RGB 0-1)
    private static final float[] DARK_BLUE  = {0.106f, 0.208f, 0.373f};  // #1B3560
    private static final float[] MED_BLUE   = {0.180f, 0.349f, 0.580f};  // #2E5994
    private static final float[] ACCENT     = {0.204f, 0.596f, 0.859f};  // #3498DB
    private static final float[] DARK_GRAY  = {0.200f, 0.200f, 0.200f};
    private static final float[] MED_GRAY   = {0.400f, 0.400f, 0.400f};
    private static final float[] LIGHT_GRAY = {0.920f, 0.920f, 0.920f};
    private static final float[] WHITE      = {1f, 1f, 1f};
    private static final float[] GREEN      = {0.153f, 0.682f, 0.376f};  // #27AE60
    private static final float[] RED_SOFT   = {0.906f, 0.298f, 0.235f};  // #E74C3C

    // Fonts
    private static final PDType1Font HELVETICA       = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font HELVETICA_BOLD  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font HELVETICA_OBL   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    private static final PDType1Font COURIER         = new PDType1Font(Standard14Fonts.FontName.COURIER);

    // Page layout
    private static final float PAGE_W = PDRectangle.LETTER.getWidth();
    private static final float PAGE_H = PDRectangle.LETTER.getHeight();
    private static final float MARGIN = 54f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    public static void main(String[] args) throws Exception {
        Path out = Paths.get("docs/swingtopdf-sales-brochure.pdf").toAbsolutePath();
        System.out.println("Generating brochure: " + out);

        try (PDDocument doc = new PDDocument()) {
            writePage1_Cover(doc);
            writePage2_Problem(doc);
            writePage3_Capabilities(doc);
            writePage4_Pricing(doc);

            doc.save(out.toFile());
        }

        System.out.println("Done: " + out);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(out.toFile());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PAGE 1 — Cover / Hero
    // ─────────────────────────────────────────────────────────────
    private static void writePage1_Cover(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Dark blue header band
            fillRect(cs, 0, PAGE_H - 220, PAGE_W, 220, DARK_BLUE);

            // Product name
            drawText(cs, HELVETICA_BOLD, 36, WHITE, MARGIN, PAGE_H - 90, "swingtopdf");
            // Tagline
            drawText(cs, HELVETICA, 16, WHITE, MARGIN, PAGE_H - 120,
                    "Professional PDF Export for Java Swing Applications");

            // Subtitle line
            drawText(cs, HELVETICA_OBL, 11, new float[]{0.7f, 0.8f, 1f}, MARGIN, PAGE_H - 155,
                    "Searchable text. Vector graphics. Embedded fonts. One line of code.");

            // Accent line under header
            fillRect(cs, 0, PAGE_H - 224, PAGE_W, 4, ACCENT);

            float y = PAGE_H - 280;

            // Hero bullets
            String[] bullets = {
                "Convert any Swing JComponent to a true-vector PDF",
                "Text is searchable, selectable, and zoomable \u2014 not a rasterized screenshot",
                "20+ dedicated component handlers: JTable, JTree, JTextPane, and more",
                "Auto-pagination with headers, footers, and page numbers",
                "Interactive AcroForm fields: text inputs, checkboxes, dropdowns",
                "Font embedding with automatic glyph subsetting",
                "Thread-safe API \u2014 call from any thread"
            };

            for (String bullet : bullets) {
                drawText(cs, HELVETICA_BOLD, 10, ACCENT, MARGIN, y, "\u2022");
                drawText(cs, HELVETICA, 10, DARK_GRAY, MARGIN + 14, y, bullet);
                y -= 22;
            }

            // Code example box
            y -= 20;
            float boxH = 130;
            fillRect(cs, MARGIN, y - boxH, CONTENT_W, boxH, new float[]{0.96f, 0.96f, 0.98f});
            strokeRect(cs, MARGIN, y - boxH, CONTENT_W, boxH, new float[]{0.80f, 0.80f, 0.85f}, 0.5f);

            drawText(cs, HELVETICA_BOLD, 10, DARK_BLUE, MARGIN + 12, y - 18, "Integration in 30 seconds:");

            float codeY = y - 38;
            String[] code = {
                "SwingPdfExporter.from(myPanel)",
                "    .pageSize(PageSize.A4)",
                "    .exportMode(ExportMode.DATA_REPORT)",
                "    .title(\"Q3 Sales Report\")",
                "    .footer(HeaderFooter.of(\"Page {page} of {pages}\"))",
                "    .export(Path.of(\"report.pdf\"));"
            };
            for (String line : code) {
                drawText(cs, COURIER, 9, MED_BLUE, MARGIN + 16, codeY, line);
                codeY -= 14;
            }

            // Bottom tagline
            drawText(cs, HELVETICA_BOLD, 11, DARK_BLUE, MARGIN, 80,
                    "Your Swing UI deserves a PDF that looks as good as the screen.");
            drawText(cs, HELVETICA, 9, MED_GRAY, MARGIN, 62,
                    "swingtopdf.io");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PAGE 2 — The Problem / Competitive Landscape
    // ─────────────────────────────────────────────────────────────
    private static void writePage2_Problem(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Header band
            fillRect(cs, 0, PAGE_H - 50, PAGE_W, 50, DARK_BLUE);
            drawText(cs, HELVETICA_BOLD, 14, WHITE, MARGIN, PAGE_H - 34, "The Problem");
            drawText(cs, HELVETICA, 8, new float[]{0.7f, 0.8f, 1f}, PAGE_W - MARGIN - 60, PAGE_H - 34, "swingtopdf.io");
            fillRect(cs, 0, PAGE_H - 53, PAGE_W, 3, ACCENT);

            float y = PAGE_H - 90;

            // Problem statement
            y = drawWrappedText(cs, HELVETICA, 10.5f, DARK_GRAY, MARGIN, y, CONTENT_W,
                    "Most Java teams solving the \"export to PDF\" problem reach for the same workaround: " +
                    "capture a screenshot, embed it as a raster image, call it done. The result is a blurry, " +
                    "non-searchable, bloated PDF that reflects poorly on an otherwise professional application.");
            y -= 12;
            y = drawWrappedText(cs, HELVETICA_BOLD, 10.5f, DARK_BLUE, MARGIN, y, CONTENT_W,
                    "For a financial statement, a clinical report, or a government document, that is not acceptable.");

            y -= 28;
            drawText(cs, HELVETICA_BOLD, 13, DARK_BLUE, MARGIN, y, "Every Alternative Falls Short");
            y -= 22;

            // Comparison table
            String[][] alts = {
                {"Screenshot / Robot.createScreenCapture()", "Raster output. Blurry. Text not searchable."},
                {"Java Print API to PDF printer", "Raster output. No pagination control."},
                {"JasperReports / BIRT", "Requires rebuilding UI in a report format."},
                {"Raw Apache PDFBox", "No component awareness. No pagination. Build it yourself."},
                {"Build it in-house", "$15K\u201350K+ in engineering. Ongoing maintenance."},
            };

            for (String[] row : alts) {
                fillRect(cs, MARGIN, y - 14, CONTENT_W, 28, LIGHT_GRAY);
                drawText(cs, HELVETICA_BOLD, 8.5f, DARK_GRAY, MARGIN + 6, y - 2, row[0]);
                drawText(cs, HELVETICA, 8.5f, MED_GRAY, MARGIN + 6, y - 14, row[1]);
                y -= 32;
            }

            y -= 20;
            drawText(cs, HELVETICA_BOLD, 13, DARK_BLUE, MARGIN, y, "Competitive Landscape");
            y -= 22;

            // Competitor table header
            float[] colX = {MARGIN, MARGIN + 140, MARGIN + 240, MARGIN + 310};
            fillRect(cs, MARGIN, y - 14, CONTENT_W, 18, DARK_BLUE);
            drawText(cs, HELVETICA_BOLD, 8, WHITE, colX[0] + 6, y - 8, "Library");
            drawText(cs, HELVETICA_BOLD, 8, WHITE, colX[1] + 6, y - 8, "Price");
            drawText(cs, HELVETICA_BOLD, 8, WHITE, colX[2] + 6, y - 8, "License");
            drawText(cs, HELVETICA_BOLD, 8, WHITE, colX[3] + 6, y - 8, "Key Limitation");
            y -= 18;

            String[][] competitors = {
                {"iText 7", "~$45K/yr avg", "AGPL / Comm.", "AGPL repels enterprise; very expensive"},
                {"IronPDF", "$749\u2013$2,999", "Commercial", "Primarily .NET; Java is a wrapper"},
                {"Aspose.PDF", "$1,199+", "Commercial", "Heavy API; no Swing-specific support"},
                {"PDFreactor", "$2,950+", "Commercial", "HTML-to-PDF only; no Swing rendering"},
                {"Apache PDFBox", "Free", "Apache 2.0", "Low-level; no Swing rendering layer"},
                {"VectorGraphics2D", "Free", "LGPL", "Missing font embedding; stale"},
                {"FreeHEP", "Free", "LGPL", "Abandoned; built for Java 1.4"},
            };

            boolean alt = false;
            for (String[] row : competitors) {
                if (alt) fillRect(cs, MARGIN, y - 12, CONTENT_W, 17, LIGHT_GRAY);
                drawText(cs, HELVETICA_BOLD, 7.5f, DARK_GRAY, colX[0] + 6, y - 6, row[0]);
                drawText(cs, HELVETICA, 7.5f, DARK_GRAY, colX[1] + 6, y - 6, row[1]);
                drawText(cs, HELVETICA, 7.5f, DARK_GRAY, colX[2] + 6, y - 6, row[2]);
                drawText(cs, HELVETICA, 7.5f, DARK_GRAY, colX[3] + 6, y - 6, row[3]);
                y -= 17;
                alt = !alt;
            }

            y -= 20;
            fillRect(cs, MARGIN, y - 36, CONTENT_W, 40, new float[]{0.93f, 0.96f, 1f});
            drawText(cs, HELVETICA_BOLD, 10, DARK_BLUE, MARGIN + 12, y - 10,
                    "swingtopdf is the only library that converts a live Swing component tree");
            drawText(cs, HELVETICA_BOLD, 10, DARK_BLUE, MARGIN + 12, y - 24,
                    "to a fully vector PDF with searchable text and embedded fonts.");

            // Footer
            drawText(cs, HELVETICA, 8, MED_GRAY, MARGIN, 40, "swingtopdf \u2014 Professional PDF Export for Java Swing Applications");
            drawText(cs, HELVETICA, 8, MED_GRAY, PAGE_W - MARGIN - 30, 40, "Page 2");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PAGE 3 — Capabilities
    // ─────────────────────────────────────────────────────────────
    private static void writePage3_Capabilities(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Header band
            fillRect(cs, 0, PAGE_H - 50, PAGE_W, 50, DARK_BLUE);
            drawText(cs, HELVETICA_BOLD, 14, WHITE, MARGIN, PAGE_H - 34, "Capabilities");
            drawText(cs, HELVETICA, 8, new float[]{0.7f, 0.8f, 1f}, PAGE_W - MARGIN - 60, PAGE_H - 34, "swingtopdf.io");
            fillRect(cs, 0, PAGE_H - 53, PAGE_W, 3, ACCENT);

            float y = PAGE_H - 82;

            // Component coverage section
            drawText(cs, HELVETICA_BOLD, 12, DARK_BLUE, MARGIN, y, "Component Coverage");
            y -= 8;
            drawText(cs, HELVETICA, 9, MED_GRAY, MARGIN, y,
                    "Dedicated vector renderers for every major Swing component. Not a generic paint \u2014 each type is structure-aware.");
            y -= 20;

            String[][] components = {
                {"JLabel", "Icon + text; multi-line HTML; disabled state"},
                {"JTextField, JTextArea, JPasswordField", "Full text with alignment and word-wrap"},
                {"JTextPane", "Bold, italic, underline, strikethrough, color, highlight, icons"},
                {"JEditorPane (HTML)", "CSS fonts/color, block backgrounds, table borders"},
                {"JTable", "All rows, all columns \u2014 not just the visible viewport"},
                {"JList, JTree", "Full data model with icons; expand/collapse state"},
                {"JTabbedPane", "Tab bar with icons; all tabs stacked vertically"},
                {"JCheckBox, JRadioButton", "Vector indicators; correct checked/selected state"},
                {"JComboBox, JSpinner", "Selected value with styling; chevron arrows"},
                {"JProgressBar, JSlider, JScrollBar", "Vector track, thumb, tick labels"},
                {"JSplitPane, JInternalFrame", "Vector chrome; full content recursion"},
                {"JPanel, JScrollPane, JToolBar, ...", "Background, border, full child recursion"},
            };

            for (String[] row : components) {
                drawText(cs, HELVETICA_BOLD, 8, DARK_BLUE, MARGIN + 4, y, row[0]);
                drawText(cs, HELVETICA, 8, MED_GRAY, MARGIN + 230, y, row[1]);
                y -= 14;
            }

            y -= 8;
            drawText(cs, HELVETICA_OBL, 8, MED_GRAY, MARGIN + 4, y,
                    "Components without a dedicated handler are rasterized automatically \u2014 always visually correct.");

            // Feature sections
            y -= 28;
            y = drawFeatureSection(cs, y, "Data Report Mode",
                    "Renders the complete data behind your UI \u2014 not just what is visible on screen.",
                    new String[]{
                        "A JTable scrolled to row 50 of 500 renders all 500 rows",
                        "A JTabbedPane on tab 2 of 5 renders all 5 tabs",
                        "A JScrollPane showing a partial view renders the complete content"
                    });

            y -= 12;
            y = drawFeatureSection(cs, y, "Auto-Pagination & Headers/Footers",
                    "Long content is automatically paginated. Professional page decoration on every page.",
                    new String[]{
                        "Smart page breaks at table row boundaries",
                        "KEEP_TOGETHER client property prevents splitting components",
                        "Headers and footers with {page} and {pages} tokens",
                        "Full control over alignment, font size, and color"
                    });

            y -= 12;
            y = drawFeatureSection(cs, y, "Interactive AcroForm Fields",
                    "Export fillable PDF forms from live Swing components. No separate form authoring tool required.",
                    new String[]{
                        "JTextField, JTextArea, JPasswordField -> text input fields",
                        "JCheckBox -> checkboxes; JRadioButton -> grouped radio buttons",
                        "JComboBox -> dropdown selections"
                    });

            y -= 12;
            y = drawFeatureSection(cs, y, "Font Embedding",
                    "Fonts are automatically located, subsetted, and embedded. Three resolution strategies:",
                    new String[]{
                        "JVM-internal reflection (fastest; handles most fonts)",
                        "OS font directory scan (catches system-installed fonts)",
                        "User-supplied FontResolver hook (custom font locations)"
                    });

            // Footer
            drawText(cs, HELVETICA, 8, MED_GRAY, MARGIN, 40, "swingtopdf \u2014 Professional PDF Export for Java Swing Applications");
            drawText(cs, HELVETICA, 8, MED_GRAY, PAGE_W - MARGIN - 30, 40, "Page 3");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PAGE 4 — Pricing & Contact
    // ─────────────────────────────────────────────────────────────
    private static void writePage4_Pricing(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Header band
            fillRect(cs, 0, PAGE_H - 50, PAGE_W, 50, DARK_BLUE);
            drawText(cs, HELVETICA_BOLD, 14, WHITE, MARGIN, PAGE_H - 34, "Licensing & Pricing");
            drawText(cs, HELVETICA, 8, new float[]{0.7f, 0.8f, 1f}, PAGE_W - MARGIN - 60, PAGE_H - 34, "swingtopdf.io");
            fillRect(cs, 0, PAGE_H - 53, PAGE_W, 3, ACCENT);

            float y = PAGE_H - 84;

            drawWrappedText(cs, HELVETICA, 10, DARK_GRAY, MARGIN, y, CONTENT_W,
                    "swingtopdf is source-available. The source code is publicly visible on GitHub for security " +
                    "review. All tiers include every feature \u2014 tiers differ by seat count, support level, " +
                    "and deployment rights.");
            y -= 40;

            // Pricing cards
            float cardW = (CONTENT_W - 18) / 4f;

            drawPricingCard(cs, MARGIN, y, cardW, "Developer",
                    "$599", "/year",
                    new String[]{"1 developer seat", "All features", "Email support"},
                    false);

            drawPricingCard(cs, MARGIN + cardW + 6, y, cardW, "Team",
                    "$1,999", "/year",
                    new String[]{"Up to 5 developers", "All features", "Priority support"},
                    true);  // highlighted

            drawPricingCard(cs, MARGIN + 2 * (cardW + 6), y, cardW, "Server",
                    "$3,999", "/year",
                    new String[]{"Unlimited developers", "Per production server", "SLA included"},
                    false);

            drawPricingCard(cs, MARGIN + 3 * (cardW + 6), y, cardW, "OEM / ISV",
                    "$15K+", "/year",
                    new String[]{"Redistribution rights", "Embed in your product", "Custom terms"},
                    false);

            y -= 175;

            // ROI section
            drawText(cs, HELVETICA_BOLD, 13, DARK_BLUE, MARGIN, y, "The ROI Case");
            y -= 20;
            y = drawWrappedText(cs, HELVETICA, 10, DARK_GRAY, MARGIN, y, CONTENT_W,
                    "Building a credible Swing-to-PDF solution from scratch takes a senior Java developer " +
                    "3\u20138 weeks. That is $15,000\u2013$50,000+ in engineering cost \u2014 and the solution " +
                    "must be maintained indefinitely.");
            y -= 8;
            y = drawWrappedText(cs, HELVETICA_BOLD, 10, DARK_BLUE, MARGIN, y, CONTENT_W,
                    "swingtopdf costs $599/year for a single developer. The annual license pays for itself " +
                    "in less than one day of developer time.");

            y -= 28;
            drawText(cs, HELVETICA_BOLD, 13, DARK_BLUE, MARGIN, y, "Why Not Build It In-House?");
            y -= 20;
            y = drawWrappedText(cs, HELVETICA, 10, DARK_GRAY, MARGIN, y, CONTENT_W,
                    "Many teams attempt to build Swing-to-PDF internally. Initial progress is fast, " +
                    "but the edge cases consume weeks:");
            y -= 6;

            String[] diyPoints = {
                "Font embedding across operating systems \u2014 resolving AWT fonts to TrueType files",
                "Correct text layout for styled JTextPane and JEditorPane HTML documents",
                "Pagination that doesn't split table rows or break component groups",
                "20+ component handlers tested against real-world Swing UIs (174 passing tests)",
                "Every JDK and PDFBox update requires regression testing of your custom solution"
            };
            for (String point : diyPoints) {
                drawText(cs, HELVETICA_BOLD, 9, RED_SOFT, MARGIN + 4, y, "x");
                y = drawWrappedText(cs, HELVETICA, 9, DARK_GRAY, MARGIN + 18, y, CONTENT_W - 18, point);
                y -= 6;
            }

            // Who uses it
            y -= 14;
            drawText(cs, HELVETICA_BOLD, 13, DARK_BLUE, MARGIN, y, "Built For");
            y -= 18;

            String[][] segments = {
                {"Financial Services", "Trade confirmations, account statements, audit reports"},
                {"Healthcare", "Patient records, clinical summaries, lab results, regulatory submissions"},
                {"Government", "Official filings, internal reports, citizen-facing documents"},
                {"Enterprise Software Vendors", "ISVs embedding PDF export in products sold to customers"},
                {"ERP & Accounting", "Invoices, purchase orders, financial statements"},
            };

            for (String[] seg : segments) {
                drawText(cs, HELVETICA_BOLD, 9, MED_BLUE, MARGIN + 4, y, seg[0]);
                drawText(cs, HELVETICA, 8.5f, MED_GRAY, MARGIN + 185, y, seg[1]);
                y -= 16;
            }

            // Contact / CTA
            y -= 16;
            fillRect(cs, MARGIN, y - 50, CONTENT_W, 54, DARK_BLUE);
            drawText(cs, HELVETICA_BOLD, 14, WHITE, MARGIN + 16, y - 18,
                    "Ready to see the difference?");
            drawText(cs, HELVETICA, 10, new float[]{0.7f, 0.8f, 1f}, MARGIN + 16, y - 36,
                    "Request a demo PDF and trial license: swingtopdf.io");

            // Footer
            drawText(cs, HELVETICA, 8, MED_GRAY, MARGIN, 40, "swingtopdf \u2014 Professional PDF Export for Java Swing Applications");
            drawText(cs, HELVETICA, 8, MED_GRAY, PAGE_W - MARGIN - 30, 40, "Page 4");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: draw a pricing card
    // ─────────────────────────────────────────────────────────────
    private static void drawPricingCard(PDPageContentStream cs, float x, float y,
            float w, String title, String price, String period,
            String[] features, boolean highlight) throws IOException {

        float h = 155;
        float cardTop = y;

        if (highlight) {
            fillRect(cs, x - 1, cardTop - h - 1, w + 2, h + 2, ACCENT);
        }
        fillRect(cs, x, cardTop - h, w, h, WHITE);
        strokeRect(cs, x, cardTop - h, w, h, highlight ? ACCENT : LIGHT_GRAY, highlight ? 2f : 0.5f);

        // Title bar
        fillRect(cs, x, cardTop - 28, w, 28, highlight ? ACCENT : DARK_BLUE);
        float titleW = HELVETICA_BOLD.getStringWidth(title) / 1000f * 10;
        drawText(cs, HELVETICA_BOLD, 10, WHITE, x + (w - titleW) / 2, cardTop - 19, title);

        // Price
        float priceW = HELVETICA_BOLD.getStringWidth(price) / 1000f * 20;
        float periodW = HELVETICA.getStringWidth(period) / 1000f * 9;
        float totalW = priceW + periodW;
        drawText(cs, HELVETICA_BOLD, 20, DARK_BLUE, x + (w - totalW) / 2, cardTop - 58, price);
        drawText(cs, HELVETICA, 9, MED_GRAY, x + (w - totalW) / 2 + priceW, cardTop - 54, period);

        // Features
        float fy = cardTop - 80;
        for (String feat : features) {
            drawText(cs, HELVETICA, 8, GREEN, x + 10, fy, "+");
            drawText(cs, HELVETICA, 8, DARK_GRAY, x + 22, fy, feat);
            fy -= 16;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: draw a feature section with header + desc + bullets
    // ─────────────────────────────────────────────────────────────
    private static float drawFeatureSection(PDPageContentStream cs, float y,
            String title, String description, String[] bullets) throws IOException {
        drawText(cs, HELVETICA_BOLD, 11, DARK_BLUE, MARGIN, y, title);
        y -= 14;
        y = drawWrappedText(cs, HELVETICA, 9, MED_GRAY, MARGIN, y, CONTENT_W, description);
        y -= 6;
        for (String bullet : bullets) {
            drawText(cs, HELVETICA, 9, GREEN, MARGIN + 8, y, "+");
            drawText(cs, HELVETICA, 9, DARK_GRAY, MARGIN + 22, y, bullet);
            y -= 14;
        }
        return y;
    }

    // ─────────────────────────────────────────────────────────────
    //  Low-level drawing helpers
    // ─────────────────────────────────────────────────────────────
    private static void drawText(PDPageContentStream cs, PDType1Font font,
            float size, float[] color, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static float drawWrappedText(PDPageContentStream cs, PDType1Font font,
            float size, float[] color, float x, float y, float maxWidth, String text) throws IOException {
        List<String> lines = wrapText(text, font, size, maxWidth);
        for (String line : lines) {
            drawText(cs, font, size, color, x, y, line);
            y -= size * 1.4f;
        }
        return y;
    }

    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(test) / 1000f * fontSize;
            if (width > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static void fillRect(PDPageContentStream cs, float x, float y,
            float w, float h, float[] color) throws IOException {
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void strokeRect(PDPageContentStream cs, float x, float y,
            float w, float h, float[] color, float lineWidth) throws IOException {
        cs.setStrokingColor(color[0], color[1], color[2]);
        cs.setLineWidth(lineWidth);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }
}
