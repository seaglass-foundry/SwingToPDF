package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Renders HTML-formatted text from a {@link JLabel} as styled PDF vector text,
 * preserving colours, font sizes, bold, italic, underline, and strikethrough.
 *
 * <p>Supported HTML constructs:
 * <ul>
 *   <li>Inline: {@code <b>}, {@code <i>}, {@code <u>}, {@code <s>},
 *       {@code <strong>}, {@code <em>}, {@code <strike>}</li>
 *   <li>Colour: {@code <font color="...">}  -- hex ({@code #rrggbb}, {@code #rgb})
 *       and CSS named colours</li>
 *   <li>Size: {@code <font size="+2">} / {@code size="-1"} / {@code size="4"}</li>
 *   <li>Inline CSS via {@code style="..."} on any tag</li>
 *   <li>Headings: {@code <h1>}-{@code <h6>} (bold + proportionally scaled)</li>
 *   <li>Block breaks: {@code <br>}, {@code <p>}, {@code <div>}, {@code <li>}</li>
 *   <li>Tables: {@code <tr>} -&gt; line break; {@code <td>}/{@code <th>}
 *       -&gt; two-space separator between cells</li>
 * </ul>
 *
 * <p>Word-wrapping is not implemented  -- content that overflows the label width
 * is clipped, matching the behaviour of the existing multi-line plain-text path.
 */
final class HtmlStyledTextRenderer {

    /** One styled text run within a rendered line. */
    record StyledRun(String text, Font font, Color color, boolean underline, boolean strike) {}

    private HtmlStyledTextRenderer() {}

    // Pixel sizes for HTML font sizes 1-7 (size 3 = 12 px = label default)
    private static final float[] HTML_FONT_PX = {8f, 10f, 12f, 14f, 18f, 24f, 36f};

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parses {@code html} into styled text runs grouped by logical line.
     *
     * @param html       HTML string (must start with {@code <html>})
     * @param baseFont   label's base font
     * @param baseColor  label's base foreground colour
     * @return list of lines; each line is a (possibly empty) list of styled runs
     */
    static List<List<StyledRun>> parse(String html, Font baseFont, Color baseColor) {
        ParseState state = new ParseState(baseFont, baseColor);
        try {
            new ParserDelegator().parse(new StringReader(html), state, true);
        } catch (IOException ignored) {}
        state.finish();
        return state.result;
    }

    /**
     * Renders the parsed styled runs at the label's position in the PDF.
     *
     * @param label  source label (used for alignment, insets, icon, font metrics)
     * @param icon   the label's icon, or {@code null}
     * @param lines  output of {@link #parse}
     * @param absX   absolute X of the label's top-left corner (root-space pixels)
     * @param absY   absolute Y of the label's top-left corner (root-space pixels)
     * @param ctx    per-page handler context
     */
    static void renderLines(JLabel label, Icon icon, List<List<StyledRun>> lines,
                            int absX, int absY, HandlerContext ctx) throws IOException {
        Insets insets = label.getInsets();
        int viewX = absX + insets.left;
        int viewY = absY + insets.top;
        int viewW = label.getWidth()  - insets.left - insets.right;
        int viewH = label.getHeight() - insets.top  - insets.bottom;

        // Icon at leading edge, vertically centred
        if (icon != null) {
            int iconY = viewY + (viewH - icon.getIconHeight()) / 2;
            IconPainter.render(icon, label, viewX, iconY, ctx);
            viewX += icon.getIconWidth() + label.getIconTextGap();
            viewW -= icon.getIconWidth() + label.getIconTextGap();
        }

        if (lines.isEmpty()) return;

        // Compute per-line ascent and total height (max font metrics across runs)
        Font baseFont = label.getFont();
        if (baseFont == null) baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        int[] lineAscents = new int[lines.size()];
        int[] lineHeights = new int[lines.size()];
        int totalH = 0;
        for (int i = 0; i < lines.size(); i++) {
            int maxAsc = 0, maxDesc = 0;
            for (StyledRun run : lines.get(i)) {
                FontMetrics fm = label.getFontMetrics(run.font());
                maxAsc  = Math.max(maxAsc,  fm.getAscent());
                maxDesc = Math.max(maxDesc, fm.getDescent() + fm.getLeading());
            }
            if (maxAsc == 0 && maxDesc == 0) {
                FontMetrics fm = label.getFontMetrics(baseFont);
                maxAsc  = fm.getAscent();
                maxDesc = fm.getDescent() + fm.getLeading();
            }
            lineAscents[i] = maxAsc;
            lineHeights[i] = maxAsc + maxDesc;
            totalH += lineHeights[i];
        }

        // Vertical alignment
        int startY;
        int va = label.getVerticalAlignment();
        if (va == SwingConstants.BOTTOM) {
            startY = viewY + viewH - totalH;
        } else if (va == SwingConstants.TOP) {
            startY = viewY;
        } else {
            startY = viewY + (viewH - totalH) / 2;
        }

        // Render each line
        int ha = label.getHorizontalAlignment();
        for (int li = 0; li < lines.size(); li++) {
            List<StyledRun> line = lines.get(li);
            int baseline = startY + lineAscents[li];

            // Measure total line width for horizontal alignment
            int lineW = 0;
            for (StyledRun run : line) {
                lineW += label.getFontMetrics(run.font()).stringWidth(run.text());
            }

            int x;
            if (ha == SwingConstants.CENTER) {
                x = viewX + (viewW - lineW) / 2;
            } else if (ha == SwingConstants.RIGHT || ha == SwingConstants.TRAILING) {
                x = viewX + viewW - lineW;
            } else {
                x = viewX;
            }

            // Render each run
            for (StyledRun run : line) {
                FontMetrics fm  = label.getFontMetrics(run.font());
                int         runW = fm.stringWidth(run.text());
                if (runW > 0) {
                    ctx.writer().drawText(run.text(),
                                          ctx.fontMapper().resolve(run.font()),
                                          run.font().getSize2D(), run.color(),
                                          x, baseline);
                    if (run.underline()) {
                        ctx.writer().drawLine(x, baseline + 1, x + runW, baseline + 1,
                                              run.color(), 1f);
                    }
                    if (run.strike()) {
                        int stY = baseline - fm.getAscent() / 3;
                        ctx.writer().drawLine(x, stY, x + runW, stY, run.color(), 1f);
                    }
                }
                x += runW;
            }
            startY += lineHeights[li];
        }
    }

    // -----------------------------------------------------------------------
    // HTML parser state (ParserCallback subclass holds all mutable state)
    // -----------------------------------------------------------------------

    private static final class ParseState extends HTMLEditorKit.ParserCallback {

        final List<List<StyledRun>> result = new ArrayList<>();
        final List<StyledRun>       currentLine = new ArrayList<>();
        final StringBuilder         buf = new StringBuilder();
        final Deque<StyleState>     stack = new ArrayDeque<>();
        final Font                  baseFont;
        int     tdCount = 0;
        boolean inHead  = false;

        ParseState(Font baseFont, Color baseColor) {
            this.baseFont = baseFont;
            stack.push(new StyleState(baseFont, baseColor, false, false));
        }

        StyleState top() { return stack.peek(); }

        /** Flush buffered text as a styled run onto the current line. */
        void flushBuf() {
            if (buf.length() == 0) return;
            String t = buf.toString().replace('\u00A0', ' ');
            buf.setLength(0);
            if (!t.isEmpty()) {
                StyleState s = top();
                currentLine.add(new StyledRun(t, s.font, s.color, s.underline, s.strike));
            }
        }

        /** Close the current line and start a fresh one. */
        void breakLine() {
            flushBuf();
            if (!currentLine.isEmpty()) {
                result.add(new ArrayList<>(currentLine));
                currentLine.clear();
            }
        }

        /** Called when parsing is complete: flush remaining text. */
        void finish() {
            flushBuf();
            if (!currentLine.isEmpty()) {
                result.add(new ArrayList<>(currentLine));
                currentLine.clear();
            }
        }

        @Override
        public void handleText(char[] data, int pos) {
            if (!inHead) buf.append(data);
        }

        @Override
        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (t == HTML.Tag.HEAD || t == HTML.Tag.STYLE || t == HTML.Tag.SCRIPT) {
                inHead = true;
                return;
            }
            if (inHead) return;

            // BR is a void element  -- treat it like a simple tag
            if (t == HTML.Tag.BR) {
                flushBuf();
                breakLine();
                return;   // do NOT push onto the style stack
            }

            flushBuf();
            StyleState parent = top();
            StyleState next   = parent.copy();

            if (t == HTML.Tag.B || t == HTML.Tag.STRONG) {
                next.font = next.font.deriveFont(next.font.getStyle() | Font.BOLD);

            } else if (t == HTML.Tag.I || t == HTML.Tag.EM) {
                next.font = next.font.deriveFont(next.font.getStyle() | Font.ITALIC);

            } else if (t == HTML.Tag.U) {
                next.underline = true;

            } else if (t == HTML.Tag.S || t == HTML.Tag.STRIKE) {
                next.strike = true;

            } else if (t == HTML.Tag.FONT) {
                Object colorAttr = a.getAttribute(HTML.Attribute.COLOR);
                if (colorAttr != null) {
                    Color c = parseColor(colorAttr.toString());
                    if (c != null) next.color = c;
                }
                Object sizeAttr = a.getAttribute(HTML.Attribute.SIZE);
                if (sizeAttr != null) {
                    float sz = parseHtmlFontSize(sizeAttr.toString(), baseFont.getSize2D());
                    if (sz > 0) next.font = next.font.deriveFont(sz);
                }
                Object faceAttr = a.getAttribute(HTML.Attribute.FACE);
                if (faceAttr != null) {
                    next.font = new Font(faceAttr.toString(),
                                         next.font.getStyle(),
                                         Math.round(next.font.getSize2D()));
                }
                applyStyleAttr(a, next);

            } else if (t == HTML.Tag.SPAN || t == HTML.Tag.BODY) {
                applyStyleAttr(a, next);

            } else if (t == HTML.Tag.H1 || t == HTML.Tag.H2 || t == HTML.Tag.H3
                    || t == HTML.Tag.H4 || t == HTML.Tag.H5 || t == HTML.Tag.H6) {
                next.font = next.font.deriveFont(next.font.getStyle() | Font.BOLD);
                next.font = next.font.deriveFont(next.font.getSize2D() * headingScale(t));
                breakLine();

            } else if (t == HTML.Tag.P || t == HTML.Tag.DIV) {
                applyStyleAttr(a, next);
                breakLine();

            } else if (t == HTML.Tag.LI) {
                breakLine();
                buf.append("  \u2022 ");

            } else if (t == HTML.Tag.TR) {
                breakLine();
                tdCount = 0;

            } else if (t == HTML.Tag.TD || t == HTML.Tag.TH) {
                if (tdCount > 0) buf.append("  ");
                tdCount++;
            }

            stack.push(next);
        }

        @Override
        public void handleEndTag(HTML.Tag t, int pos) {
            if (t == HTML.Tag.HEAD || t == HTML.Tag.STYLE || t == HTML.Tag.SCRIPT) {
                inHead = false;
                return;
            }
            if (inHead || (t == HTML.Tag.BR)) return;   // BR was not pushed

            flushBuf();
            if (stack.size() > 1) stack.pop();

            if (t == HTML.Tag.P  || t == HTML.Tag.DIV
                    || t == HTML.Tag.H1 || t == HTML.Tag.H2 || t == HTML.Tag.H3
                    || t == HTML.Tag.H4 || t == HTML.Tag.H5 || t == HTML.Tag.H6
                    || t == HTML.Tag.TR) {
                breakLine();
            }
        }

        @Override
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (inHead) return;
            if (t == HTML.Tag.BR) {
                flushBuf();
                breakLine();
            }
        }

        // --- helpers --------------------------------------------------------

        private static void applyStyleAttr(MutableAttributeSet a, StyleState state) {
            Object styleObj = a.getAttribute(HTML.Attribute.STYLE);
            if (styleObj == null) return;
            for (String decl : styleObj.toString().split(";")) {
                int colon = decl.indexOf(':');
                if (colon < 0) continue;
                String prop = decl.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String val  = decl.substring(colon + 1).trim();
                switch (prop) {
                    case "color" -> {
                        Color c = parseColor(val);
                        if (c != null) state.color = c;
                    }
                    case "font-size" -> {
                        float sz = parseCssFontSize(val);
                        if (sz > 0) state.font = state.font.deriveFont(sz);
                    }
                    case "font-weight" -> {
                        if (val.contains("bold"))
                            state.font = state.font.deriveFont(state.font.getStyle() | Font.BOLD);
                    }
                    case "font-style" -> {
                        if (val.contains("italic"))
                            state.font = state.font.deriveFont(state.font.getStyle() | Font.ITALIC);
                    }
                    case "text-decoration" -> {
                        if (val.contains("underline"))    state.underline = true;
                        if (val.contains("line-through")) state.strike    = true;
                    }
                }
            }
        }

        private static float headingScale(HTML.Tag t) {
            if (t == HTML.Tag.H1) return 2.00f;
            if (t == HTML.Tag.H2) return 1.50f;
            if (t == HTML.Tag.H3) return 1.17f;
            if (t == HTML.Tag.H5) return 0.83f;
            if (t == HTML.Tag.H6) return 0.67f;
            return 1.00f; // h4 = 1x
        }
    }

    // -----------------------------------------------------------------------
    // Style frame (one entry per HTML tag on the stack)
    // -----------------------------------------------------------------------

    private static final class StyleState {
        Font    font;
        Color   color;
        boolean underline;
        boolean strike;

        StyleState(Font font, Color color, boolean underline, boolean strike) {
            this.font = font;   this.color = color;
            this.underline = underline;  this.strike = strike;
        }

        StyleState copy() {
            return new StyleState(font, color, underline, strike);
        }
    }

    // -----------------------------------------------------------------------
    // Color parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a CSS/HTML colour value into a {@link Color}.
     * Handles {@code #rrggbb}, {@code #rgb}, {@code rgb(r,g,b)}, and CSS named colours.
     * Returns {@code null} if the value cannot be decoded.
     */
    static Color parseColor(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        if (value.startsWith("#")) {
            // Expand #RGB -&gt; #RRGGBB
            if (value.length() == 4) {
                value = "#"
                        + String.valueOf(value.charAt(1)).repeat(2)
                        + String.valueOf(value.charAt(2)).repeat(2)
                        + String.valueOf(value.charAt(3)).repeat(2);
            }
            try { return Color.decode(value); } catch (NumberFormatException e) { return null; }
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgb(") && lower.endsWith(")")) {
            String[] parts = lower.substring(4, lower.length() - 1).split(",");
            if (parts.length == 3) {
                try {
                    return new Color(Integer.parseInt(parts[0].trim()),
                                     Integer.parseInt(parts[1].trim()),
                                     Integer.parseInt(parts[2].trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return NAMED_COLORS.get(lower);
    }

    // -----------------------------------------------------------------------
    // Font-size parsing
    // -----------------------------------------------------------------------

    /**
     * Converts an HTML {@code <font size="...">} attribute to a pixel size.
     *
     * <ul>
     *   <li>{@code "+2"} -&gt; {@code baseSizePx x 1.2^2}</li>
     *   <li>{@code "-1"} -&gt; {@code baseSizePx / 1.2}</li>
     *   <li>{@code "4"}  -&gt; absolute HTML size 4 (14 px when base = 12 px)</li>
     * </ul>
     *
     * @return the pixel size, or {@code 0} if the attribute cannot be parsed
     */
    private static float parseHtmlFontSize(String attr, float baseSizePx) {
        attr = attr.trim();
        try {
            if (attr.startsWith("+")) {
                int delta = Integer.parseInt(attr.substring(1));
                return baseSizePx * (float) Math.pow(1.2, delta);
            } else if (attr.startsWith("-")) {
                int delta = Integer.parseInt(attr.substring(1));
                return baseSizePx / (float) Math.pow(1.2, delta);
            } else {
                int size = Integer.parseInt(attr);
                if (size >= 1 && size <= 7) {
                    // Scale relative to base font: size-3 slot = baseSizePx
                    return HTML_FONT_PX[size - 1] / 12f * baseSizePx;
                }
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    /**
     * Converts a CSS {@code font-size} value to pixels.
     * Handles {@code px}, {@code pt}, and keyword sizes.
     *
     * @return the pixel size, or {@code 0} if the value cannot be interpreted
     */
    private static float parseCssFontSize(String value) {
        value = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("px"))
                return Float.parseFloat(value.substring(0, value.length() - 2));
            if (value.endsWith("pt"))
                return Float.parseFloat(value.substring(0, value.length() - 2)) * 96f / 72f;
        } catch (NumberFormatException ignored) {}
        return switch (value) {
            case "xx-small" ->  8f;
            case "x-small"  -> 10f;
            case "small"    -> 12f;
            case "medium"   -> 14f;
            case "large"    -> 18f;
            case "x-large"  -> 24f;
            case "xx-large" -> 36f;
            default         ->  0f;
        };
    }

    // -----------------------------------------------------------------------
    // Named colour table (CSS Level 1 + common extensions)
    // -----------------------------------------------------------------------

    private static final Map<String, Color> NAMED_COLORS = Map.ofEntries(
            Map.entry("black",      Color.BLACK),
            Map.entry("white",      Color.WHITE),
            Map.entry("red",        Color.RED),
            Map.entry("green",      new Color(0x008000)),
            Map.entry("blue",       Color.BLUE),
            Map.entry("yellow",     Color.YELLOW),
            Map.entry("orange",     Color.ORANGE),
            Map.entry("purple",     new Color(0x800080)),
            Map.entry("gray",       Color.GRAY),
            Map.entry("grey",       Color.GRAY),
            Map.entry("silver",     Color.LIGHT_GRAY),
            Map.entry("lime",       new Color(0x00FF00)),
            Map.entry("aqua",       Color.CYAN),
            Map.entry("cyan",       Color.CYAN),
            Map.entry("magenta",    Color.MAGENTA),
            Map.entry("fuchsia",    Color.MAGENTA),
            Map.entry("maroon",     new Color(0x800000)),
            Map.entry("navy",       new Color(0x000080)),
            Map.entry("teal",       new Color(0x008080)),
            Map.entry("olive",      new Color(0x808000)),
            Map.entry("pink",       Color.PINK),
            Map.entry("darkgray",   Color.DARK_GRAY),
            Map.entry("darkgrey",   Color.DARK_GRAY),
            Map.entry("lightgray",  Color.LIGHT_GRAY),
            Map.entry("lightgrey",  Color.LIGHT_GRAY)
    );
}
