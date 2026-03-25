package com.seaglassfoundry.swingtopdf.internal;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HtmlTextExtractor}.
 */
class HtmlTextExtractorTest {

    // -----------------------------------------------------------------------
    // Pass-through / null handling
    // -----------------------------------------------------------------------

    @Test
    void null_returnsNull() {
        assertThat(HtmlTextExtractor.extractText(null)).isNull();
    }

    @Test
    void plainText_returnedUnchanged() {
        assertThat(HtmlTextExtractor.extractText("Hello World")).isEqualTo("Hello World");
    }

    @Test
    void nonHtmlTag_returnedUnchanged() {
        String text = "<nothtml>stuff</nothtml>";
        assertThat(HtmlTextExtractor.extractText(text)).isEqualTo(text);
    }

    // -----------------------------------------------------------------------
    // Basic HTML extraction
    // -----------------------------------------------------------------------

    @Test
    void simpleHtml_textExtracted() {
        assertThat(HtmlTextExtractor.extractText("<html>Hello</html>"))
                .isEqualTo("Hello");
    }

    @Test
    void htmlWithBody_textExtracted() {
        assertThat(HtmlTextExtractor.extractText("<html><body>Hello World</body></html>"))
                .isEqualTo("Hello World");
    }

    @Test
    void caseInsensitiveHtmlTag() {
        assertThat(HtmlTextExtractor.extractText("<HTML><BODY>Test</BODY></HTML>"))
                .isEqualTo("Test");
    }

    @Test
    void nestedInlineTags_textMerged() {
        assertThat(HtmlTextExtractor.extractText("<html><b>bold</b> and <i>italic</i></html>"))
                .isEqualTo("bold and italic");
    }

    // -----------------------------------------------------------------------
    // Entity decoding
    // -----------------------------------------------------------------------

    @Test
    void standardEntities_decoded() {
        String html = "<html>&amp; &lt; &gt; &quot;</html>";
        assertThat(HtmlTextExtractor.extractText(html)).isEqualTo("& < > \"");
    }

    @Test
    void nbspEntity_normalizedToSpace() {
        String html = "<html>word1&nbsp;word2</html>";
        assertThat(HtmlTextExtractor.extractText(html)).isEqualTo("word1 word2");
    }

    @Test
    void numericEntity_decoded() {
        // &#169; = copyright symbol
        String html = "<html>&#169;</html>";
        assertThat(HtmlTextExtractor.extractText(html)).isEqualTo("\u00A9");
    }

    // -----------------------------------------------------------------------
    // Block-level tags insert newlines
    // -----------------------------------------------------------------------

    @Test
    void brTag_insertsNewline() {
        assertThat(HtmlTextExtractor.extractText("<html>line1<br>line2</html>"))
                .isEqualTo("line1\nline2");
    }

    @Test
    void pTag_insertsNewline() {
        assertThat(HtmlTextExtractor.extractText("<html><p>para1</p><p>para2</p></html>"))
                .isEqualTo("para1\npara2");
    }

    @Test
    void divTag_insertsNewline() {
        assertThat(HtmlTextExtractor.extractText("<html><div>a</div><div>b</div></html>"))
                .isEqualTo("a\nb");
    }

    @Test
    void liTag_insertsNewline() {
        assertThat(HtmlTextExtractor.extractText("<html><ul><li>one</li><li>two</li></ul></html>"))
                .contains("one\ntwo");
    }

    @Test
    void headingTags_insertNewlines() {
        assertThat(HtmlTextExtractor.extractText("<html><h1>Title</h1><p>Body</p></html>"))
                .isEqualTo("Title\nBody");
    }

    @Test
    void h3AndH4_insertNewlines() {
        // Each heading start tag triggers a newline; bare text after </h4> has no start tag
        assertThat(HtmlTextExtractor.extractText("<html><h3>Sub</h3><h4>SubSub</h4><p>Text</p></html>"))
                .isEqualTo("Sub\nSubSub\nText");
    }

    @Test
    void blockquote_insertsNewlineBeforeContent() {
        // Newline inserted at <blockquote> start tag; text after </blockquote> follows
        // because the parser only fires handleStartTag (not handleEndTag) for newlines.
        assertThat(HtmlTextExtractor.extractText("<html>Before<blockquote>Quoted</blockquote><p>After</p></html>"))
                .isEqualTo("Before\nQuoted\nAfter");
    }

    @Test
    void dtAndDd_insertNewlines() {
        assertThat(HtmlTextExtractor.extractText("<html><dl><dt>Term</dt><dd>Definition</dd></dl></html>"))
                .contains("Term\nDefinition");
    }

    // -----------------------------------------------------------------------
    // Whitespace normalization
    // -----------------------------------------------------------------------

    @Test
    void multipleSpaces_collapsedToOne() {
        assertThat(HtmlTextExtractor.extractText("<html>a   b    c</html>"))
                .isEqualTo("a b c");
    }

    @Test
    void tripleNewlines_collapsedToDouble() {
        String html = "<html><p>a</p><p></p><p></p><p>b</p></html>";
        String result = HtmlTextExtractor.extractText(html);
        // Should not have more than 2 consecutive newlines
        assertThat(result).doesNotContain("\n\n\n");
    }

    @Test
    void leadingTrailingWhitespace_trimmed() {
        assertThat(HtmlTextExtractor.extractText("<html>  Hello  </html>"))
                .isEqualTo("Hello");
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void emptyHtml_returnsEmpty() {
        assertThat(HtmlTextExtractor.extractText("<html></html>"))
                .isEmpty();
    }

    @Test
    void htmlWithOnlyWhitespace_returnsEmpty() {
        assertThat(HtmlTextExtractor.extractText("<html>   </html>"))
                .isEmpty();
    }

    @Test
    void duplicateBrTags_singleNewline() {
        // <p><br> should not produce two newlines
        assertThat(HtmlTextExtractor.extractText("<html><p><br>text</html>"))
                .doesNotStartWith("\n\n");
    }
}
