package com.seaglassfoundry.swingtopdf.internal;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Extracts plain text from Swing HTML strings (those starting with {@code <html>}).
 *
 * <p>Unlike a simple tag-stripping regex, this uses the JDK's built-in
 * {@link ParserDelegator} so that:
 * <ul>
 *   <li>HTML entities ({@code &amp;}, {@code &lt;}, {@code &nbsp;}, etc.) are decoded.</li>
 *   <li>{@code <br>} and block-level tags ({@code <p>}, {@code <div>}, {@code <li>},
 *       {@code <h1>}–{@code <h6>}, {@code <blockquote>}, {@code <pre>}, {@code <hr>},
 *       {@code <dt>}, {@code <dd>}) are converted to {@code \n} so callers can render
 *       multi-line text.</li>
 * </ul>
 *
 * <p>If {@code text} does not start with {@code <html>} (case-insensitive) it is
 * returned unchanged. Non-breaking spaces ({@code \u00A0}) are normalised to
 * regular spaces. Multiple consecutive spaces/tabs on the same line are collapsed
 * to one space, but {@code \n} characters are preserved.
 */
final class HtmlTextExtractor {

    private HtmlTextExtractor() {}

    /**
     * Returns the plain-text content of {@code text}.
     *
     * <p>Returns {@code null} if the input is {@code null}. Returns the input
     * unchanged if it does not begin with {@code <html>}.
     */
    static String extractText(String text) {
        if (text == null) return null;
        if (!text.regionMatches(true, 0, "<html>", 0, 6)) return text;

        StringBuilder sb = new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(text),
                    new HTMLEditorKit.ParserCallback() {
                        @Override
                        public void handleText(char[] data, int pos) {
                            sb.append(data);
                        }

                        @Override
                        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                            // Insert a newline at block boundaries so lines are preserved
                            if (t == HTML.Tag.BR || t == HTML.Tag.P
                                    || t == HTML.Tag.DIV || t == HTML.Tag.LI
                                    || t == HTML.Tag.H1 || t == HTML.Tag.H2
                                    || t == HTML.Tag.H3 || t == HTML.Tag.H4
                                    || t == HTML.Tag.H5 || t == HTML.Tag.H6
                                    || t == HTML.Tag.BLOCKQUOTE || t == HTML.Tag.PRE
                                    || t == HTML.Tag.DT || t == HTML.Tag.DD) {
                                appendNewline(sb);
                            }
                        }

                        @Override
                        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                            // <br/> and <hr> are emitted as simple tags by some parsers
                            if (t == HTML.Tag.BR || t == HTML.Tag.HR) {
                                appendNewline(sb);
                            }
                        }
                    }, true);
        } catch (IOException e) {
            // Fallback: tag stripping + entity decoding (should never happen for valid Swing HTML)
            String fallback = text.replaceAll("<[^>]+>", "")
                       .replace("&amp;",  "&")
                       .replace("&lt;",   "<")
                       .replace("&gt;",   ">")
                       .replace("&quot;", "\"")
                       .replace("&apos;", "'")
                       .replace("&nbsp;", " ")
                       .replace("&#39;",  "'")
                       .replace("&mdash;", "\u2014")
                       .replace("&ndash;", "\u2013")
                       .replace("&copy;",  "\u00A9")
                       .replace("&reg;",   "\u00AE")
                       .replace("&bull;",  "\u2022")
                       .replace("&euro;",  "\u20AC");
            // Decode numeric character references: &#xHHH; and &#NNN;
            fallback = Pattern.compile("&#x([0-9A-Fa-f]+);").matcher(fallback)
                    .replaceAll(m -> String.valueOf((char) Integer.parseInt(m.group(1), 16)));
            fallback = Pattern.compile("&#(\\d+);").matcher(fallback)
                    .replaceAll(m -> String.valueOf((char) Integer.parseInt(m.group(1))));
            return fallback.trim();
        }

        return sb.toString()
                .replace('\u00A0', ' ')             // normalise &nbsp;
                .replaceAll("[ \\t]+", " ")         // collapse inline whitespace only
                .replaceAll("(\n )+", "\n")         // trim leading space after newline
                .replaceAll(" \n", "\n")            // trim trailing space before newline
                .replaceAll("\n{3,}", "\n\n")       // collapse 3+ consecutive newlines to 2
                .trim();
    }

    private static void appendNewline(StringBuilder sb) {
        // Avoid duplicate newlines (e.g. <p><br> should produce one blank line, not two)
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) != '\n') {
            sb.append('\n');
        }
    }
}
