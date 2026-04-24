# Headers & Footers

SwingToPDF can render a header band in the top margin and a footer band in the bottom margin of every page. Bands support three rendering modes (plain text, HTML, or any Swing component), dynamic page numbering, alignment, font sizing, colors, and background fills.

---

## Quick Start

```java
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter.Alignment;

SwingPdfExporter.from(panel)
    .margins(54, 54, 54, 54)
    .header(HeaderFooter.of("My Report"))
    .footer(HeaderFooter.of("Page {page} of {pages}"))
    .export(Path.of("report.pdf"));
```

---

## Creating a HeaderFooter

```java
HeaderFooter band = HeaderFooter.of("text");
```

This creates a band with default styling:

| Property | Default |
|----------|---------|
| Text | *(your text)* |
| Alignment | `CENTER` |
| Font size | 9 pt |
| Text color | `Color.GRAY` |
| Background | Transparent (none) |
| Height | Full margin height |

`HeaderFooter` is immutable. Each styling method returns a **new instance**, so you can chain freely without mutating the original.

---

## Page Number Tokens

The text string supports two tokens that are resolved at render time:

| Token | Replaced with | Example |
|-------|--------------|---------|
| `{page}` | Current page number (1-based) | `1`, `2`, `3` |
| `{pages}` | Total page count | `5` |

### Examples

```java
HeaderFooter.of("Page {page} of {pages}")     // "Page 1 of 5"
HeaderFooter.of("{page}/{pages}")              // "1/5"
HeaderFooter.of("- {page} -")                 // "- 1 -"
```

---

## Styling Options

### Alignment

```java
.align(Alignment.LEFT)
.align(Alignment.CENTER)     // default
.align(Alignment.RIGHT)
```

Text is positioned within the printable width (between the left and right margins), with 4 pt of padding from the edge.

### Font Size

```java
.fontSize(12f)    // in PDF points
```

**Default:** 9 pt. The font is always Helvetica (a standard PDF base font that requires no embedding).

### Text Color

```java
.color(Color.BLACK)
.color(new Color(0x336699))
```

**Default:** `Color.GRAY`

### Background Color

```java
.backgroundColor(new Color(240, 240, 240))    // light gray band
.backgroundColor(null)                          // transparent (default)
```

When set, the entire band area is filled with this color before the text is drawn. The background spans the full page width (edge to edge, not just the printable area).

### Band Height

```java
.height(20f)    // 20 pt tall band
.height(0f)     // fill entire margin (default)
```

When set to a positive value, the band is sized to exactly that height instead of filling the entire margin. The band is anchored to the page edge:
- **Headers** anchor to the top of the page
- **Footers** anchor to the bottom of the page

Values greater than the margin are clamped automatically.

---

## Full Example

```java
import java.awt.Color;

HeaderFooter header = HeaderFooter.of("DRAFT -- Not for distribution")
    .align(Alignment.RIGHT)
    .fontSize(8f)
    .color(Color.RED);

HeaderFooter footer = HeaderFooter.of("Page {page} of {pages}")
    .align(Alignment.CENTER)
    .fontSize(9f)
    .color(new Color(100, 100, 100))
    .backgroundColor(new Color(245, 245, 245))
    .height(24f);

SwingPdfExporter.from(panel)
    .pageSize(PageSize.LETTER)
    .margins(54, 54, 54, 54)
    .header(header)
    .footer(footer)
    .export(Path.of("report.pdf"));
```

---

## Layout Details

### Vertical Centering

Text is vertically centered within the band height. The positioning formula places the text baseline at the visual center of the band, accounting for font metrics.

### Rendering Order

Headers and footers are rendered **after** the page content and **outside** the content clip rectangle. This means they always appear on top and are never obscured by component content.

### Margin Interaction

The header renders inside the top margin. The footer renders inside the bottom margin. If your margins are too small, the text may appear cramped or clipped. A good rule of thumb:

- For a 9 pt footer, use at least 36 pt (0.5 in) bottom margin
- For a 12 pt header, use at least 54 pt (0.75 in) top margin
- If using a background band with explicit height, ensure the margin is at least as tall as the band

---

## Tips

- Use `{page}` and `{pages}` in footers for page numbering -- this is by far the most common use case
- Use headers for document titles, classification markings, or dates
- The `backgroundColor` option with a subtle gray is a clean way to visually separate the footer from content
- Set an explicit `height` when you want a compact band that doesn't fill the entire margin

---

## HTML Headers and Footers (v1.2+)

Use `HeaderFooter.html(String)` to render a band with inline HTML styling. The HTML is parsed and rendered as selectable vector text; bold, italic, underline, strikethrough, color, and size are all preserved.

```java
HeaderFooter.html(
    "<b style='color:#2B4C7E'>Quarterly Report</b> " +
    "&mdash; <span style='color:#888'>Page {page} of {pages}</span>")
    .fontSize(11f)
    .height(24f);
```

**Supported markup:**

| Feature | Example |
|---------|---------|
| Bold, italic, underline, strike | `<b>`, `<i>`, `<u>`, `<s>`, `<strong>`, `<em>` |
| Color | `<font color='red'>`, `<span style='color:#336699'>` |
| Size (relative) | `<font size='+2'>`, `<font size='-1'>` |
| Size (absolute, CSS) | `style='font-size:14pt'`, `style='font-size:18px'` |
| Explicit line break | `<br>` |
| Paragraph break | `<p>`, `<div>` |
| Headings | `<h1>` -- `<h6>` |

**Word-wrapping:** HTML bands wrap automatically to fit the printable width. Wrap points are at whitespace boundaries; inline styling is preserved across wrapped lines.

**Token substitution:** `{page}` and `{pages}` resolve in HTML strings just like plain text.

The `.color(Color)` and `.fontSize(float)` settings act as defaults for runs that the HTML doesn't explicitly style.

---

## Component Headers and Footers (v1.2+)

Use `HeaderFooter.of(JComponent)` to render any Swing component as a header or footer. The component is rendered through SwingToPDF's normal handler pipeline, so every component type that works in the document body (JLabel with HTML, JPanel with icons and borders, custom-painted components via `registerHandler`) also works in a band.

### JLabel with HTML and background

```java
JLabel header = new JLabel(
    "<html><b style='color:#2B4C7E'>Quarterly Report</b> " +
    "<span style='color:#999'>&mdash; Draft</span></html>");
header.setOpaque(true);
header.setBackground(new Color(0xEFEFEF));
header.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

SwingPdfExporter.from(panel)
    .header(HeaderFooter.of(header).height(28f))
    .export(out);
```

### JLabel with icon

```java
JLabel header = new JLabel("Warehouse Inventory");
header.setHorizontalAlignment(SwingConstants.LEFT);
header.setIcon(new ImageIcon(logoImage));
header.setIconTextGap(8);
header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
HeaderFooter.of(header).height(28f);
```

### Composite JPanel footer

```java
JPanel footer = new JPanel(new BorderLayout());
footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xCCCCCC)));
footer.add(new JLabel("Seaglass Foundry"),         BorderLayout.WEST);
footer.add(new JLabel("Page {page} of {pages}"),   BorderLayout.EAST);
HeaderFooter.of(footer).height(24f);
```

### What component mode honors

| Setting | Behavior in COMPONENT mode |
|---------|---------------------------|
| `.backgroundColor(...)` | Paints a band-wide fill **behind** the component |
| `.height(...)` | Overrides the band height (otherwise the component's preferred height is used, clamped to the margin) |
| `.align(...)` | **Ignored** -- the component always spans the full printable width. Use `JLabel.setHorizontalAlignment(...)` or the enclosing layout manager to position content within the band |
| `.color(...)` | **Ignored** -- use the component's `setForeground(...)` |
| `.fontSize(...)` | **Ignored** -- use the component's `setFont(...)` |

### Token substitution in components

`{page}` and `{pages}` tokens are resolved at render time inside any `JLabel` or `JTextComponent` in the component tree. The originals are restored after each page, so the same component instance can be reused on subsequent exports.

---

## Text Wrapping (v1.2+)

Plain-text bands do not wrap by default (long strings overflow the right edge, preserving the original v1.1 behavior). Enable wrapping with `.wrap(true)`:

```java
HeaderFooter.of("A deliberately long header that should wrap across multiple lines")
    .wrap(true)
    .height(42f);
```

- Wrapping is a simple greedy word-wrap using the PDF font's glyph metrics.
- Lines that don't fit in the band height are clipped (they don't push into the content area).
- HTML and component bands always wrap to fit; `.wrap(true)` is a no-op for those modes.

---

## Which mode should I use?

- **Plain text (`of(String)`)** -- simple, one-line bands. Fastest, smallest PDF.
- **HTML (`html(String)`)** -- quick inline styling (colors, bold, size, links) without building a Swing component.
- **Component (`of(JComponent)`)** -- full control: icons, borders, multiple sub-labels, custom painting. Use this when you want the band to look like part of the document, not just a margin annotation.
