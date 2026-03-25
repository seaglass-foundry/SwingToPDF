package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import com.seaglassfoundry.swingtopdf.api.ExportMode;

/**
 * Renders {@link JTextComponent} subclasses as PDF vector text.
 *
 * <ul>
 *   <li><b>Single-line</b> ({@link JTextField}, {@code JFormattedTextField}): vertically
 *       centred, honours {@code getHorizontalAlignment()}.</li>
 *   <li><b>Multi-line</b> ({@link JTextArea}): renders every logical line; applies word-wrap
 *       when {@code getLineWrap()} is {@code true}.</li>
 *   <li><b>{@link JPasswordField}</b>: content is replaced with the echo character repeated.</li>
 * </ul>
 */
final class JTextComponentHandler implements ComponentHandler {

    static final JTextComponentHandler INSTANCE = new JTextComponentHandler();

    private JTextComponentHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JTextComponent tc)) return;

        // When a JTextComponent is the view inside a JViewport (wrapped in a JScrollPane),
        // JViewport scrolls by repositioning the view: getY() = -scrollOffset.  The raw
        // absY therefore undershoots by the scroll amount.  Snap both position and size to
        // the JViewport's visible bounds so the AcroForm annotation and background fill are
        // anchored to the visible area rather than the (possibly off-screen) view origin.
        int renderX = absX;
        int renderY = absY;
        int renderW = tc.getWidth();
        int renderH = tc.getHeight();
        if (tc.getParent() instanceof JViewport vp) {
            renderX = absX - tc.getX();   // == absX(JViewport)
            renderY = absY - tc.getY();   // == absY(JViewport), correct even when tc.getY() < 0
            if (ctx.config().exportMode() == ExportMode.DATA_REPORT) {
                renderW = tc.getWidth();    // same as vp.getWidth() in practice
                renderH = tc.getHeight();   // full preferred-size height, not viewport clip
            } else {
                renderW = vp.getWidth();
                renderH = vp.getHeight();
            }
        }

        // 1. Background
        if (tc.isOpaque() && tc.getBackground() != null) {
            Color bg = tc.getBackground();
            if (!tc.isEnabled()) {
                Color inactiveBg = UIManager.getColor("TextField.inactiveBackground");
                if (inactiveBg != null) bg = inactiveBg;
            }
            ctx.writer().fillRect(renderX, renderY, renderW, renderH, bg);
        }

        // 2. Border (no child recursion  -- text components have no child components)
        ContainerHandler.renderBorderOnly(tc, renderX, renderY, ctx);

        // 3a. AcroForm widget (if enabled)  -- the widget appearance owns the text
        //     display, so skip vector text rendering to avoid double-drawing.
        if (ctx.acroFormEmitter() != null) {
            ctx.acroFormEmitter().addTextField(tc, renderX, renderY, renderW, renderH,
                    ctx.currentPage(), ctx.writer());
            return;
        }

        // 3b. Vector text content (only when AcroForm is not active)
        String text = getText(tc);
        if (text == null || text.isEmpty()) return;

        Font font = tc.getFont();
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        Color fg = tc.getForeground();
        if (fg == null) fg = Color.BLACK;
        if (!tc.isEnabled()) {
            Color inactiveFg = UIManager.getColor("TextField.inactiveForeground");
            fg = inactiveFg != null ? inactiveFg : Color.GRAY;
        }

        Insets insets  = tc.getInsets();
        int contentX   = renderX + insets.left;
        int contentY   = renderY + insets.top;
        int contentW   = renderW - insets.left - insets.right;
        int contentH   = renderH - insets.top  - insets.bottom;

        FontMetrics fm = tc.getFontMetrics(font);

        if (tc instanceof JTextArea area) {
            renderMultiLine(area, text, font, fg, fm, contentX, contentY, contentW, contentH, ctx);
        } else {
            renderSingleLine(tc, text, font, fg, fm, contentX, contentY, contentW, contentH, ctx);
        }
    }

    // -----------------------------------------------------------------------

    private static String getText(JTextComponent tc) {
        if (tc instanceof JPasswordField pf) {
            char[] pwd = pf.getPassword();
            if (pwd.length == 0) return null;
            char echo = pf.getEchoChar();
            return String.valueOf(echo).repeat(pwd.length);
        }
        return tc.getText();
    }

    private void renderSingleLine(JTextComponent tc, String text, Font font, Color fg,
                                   FontMetrics fm, int contentX, int contentY,
                                   int contentW, int contentH,
                                   HandlerContext ctx) throws IOException {
        int textWidth  = fm.stringWidth(text);
        int ascent     = fm.getAscent();
        int descent    = fm.getDescent();
        int textHeight = ascent + descent;

        int hAlign = (tc instanceof JTextField tf)
                ? tf.getHorizontalAlignment()
                : SwingConstants.LEADING;
        int textX;
        switch (hAlign) {
            case SwingConstants.CENTER                              -> textX = contentX + (contentW - textWidth) / 2;
            case SwingConstants.RIGHT, SwingConstants.TRAILING     -> textX = contentX + contentW - textWidth;
            default                                                 -> textX = contentX;
        }

        int baselineY = contentY + (contentH - textHeight) / 2 + ascent;
        ctx.writer().drawText(text, ctx.fontMapper().resolve(font), font.getSize2D(), fg,
                              textX, baselineY);
    }

    private void renderMultiLine(JTextArea area, String text, Font font, Color fg,
                                  FontMetrics fm, int contentX, int contentY,
                                  int contentW, int contentH,
                                  HandlerContext ctx) throws IOException {
        int lineHeight = fm.getHeight();
        int ascent     = fm.getAscent();
        List<String> lines = buildLines(area, text, fm, contentW);
        int cursorY = contentY + ascent; // baseline of first line
        for (String line : lines) {
            if (cursorY - ascent > contentY + contentH) break; // below component bounds
            ctx.writer().drawText(line, ctx.fontMapper().resolve(font), font.getSize2D(), fg,
                                  contentX, cursorY);
            cursorY += lineHeight;
        }
    }

    // -----------------------------------------------------------------------
    // Line-wrap helpers
    // -----------------------------------------------------------------------

    private static List<String> buildLines(JTextArea area, String text,
                                            FontMetrics fm, int contentW) {
        String[] paragraphs = text.split("\n", -1);
        List<String> result = new ArrayList<>();
        if (!area.getLineWrap() || contentW <= 0) {
            for (String p : paragraphs) result.add(p);
            return result;
        }
        for (String para : paragraphs) {
            if (para.isEmpty()) { result.add(""); continue; }
            if (area.getWrapStyleWord()) {
                wrapWords(para, fm, contentW, result);
            } else {
                wrapChars(para, fm, contentW, result);
            }
        }
        return result;
    }

    /** Word-wrap: break at whitespace boundaries. */
    private static void wrapWords(String para, FontMetrics fm, int maxW, List<String> out) {
        // Split keeping delimiters so spaces land on the same token as the preceding word
        String[] tokens = para.split("(?<=\\s)|(?=\\s)", -1);
        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            String candidate = line + token;
            if (fm.stringWidth(candidate) <= maxW) {
                line.append(token);
            } else {
                if (!line.isEmpty()) {
                    out.add(line.toString().stripTrailing());
                    line.setLength(0);
                }
                String stripped = token.stripLeading();
                if (fm.stringWidth(stripped) > maxW) {
                    wrapChars(stripped, fm, maxW, out);
                } else {
                    line.append(stripped);
                }
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
    }

    /** Character-wrap: break at the last character that fits. */
    private static void wrapChars(String para, FontMetrics fm, int maxW, List<String> out) {
        int start = 0;
        while (start < para.length()) {
            int end = start + 1;
            while (end < para.length()
                    && fm.stringWidth(para.substring(start, end + 1)) <= maxW) {
                end++;
            }
            out.add(para.substring(start, end));
            start = end;
        }
    }
}
