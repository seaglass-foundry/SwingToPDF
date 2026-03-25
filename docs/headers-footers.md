# Headers & Footers

SwingToPDF can render a text band in the top margin (header) and bottom margin (footer) of every page. These bands support dynamic page numbering, alignment, font sizing, colors, and background fills.

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

HeaderFooter header = HeaderFooter.of("CONFIDENTIAL -- Internal Use Only")
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
