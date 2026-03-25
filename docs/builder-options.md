# Builder Options

`SwingPdfExporter` uses a fluent builder pattern. Every method returns `this`, so calls can be chained. This page documents every available option.

---

## Creating the Builder

```java
SwingPdfExporter exporter = SwingPdfExporter.from(myComponent);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `root` | `JComponent` | The Swing component tree to export. Must not be null. |

---

## Page Layout

### pageSize

```java
.pageSize(PageSize.LETTER)
```

Sets the output page dimensions. Dimensions are in PDF points (1 pt = 1/72 inch).

| Constant | Dimensions (pt) | Dimensions (inches) |
|----------|-----------------|---------------------|
| `PageSize.A3` | 842 x 1191 | 11.7 x 16.5 |
| `PageSize.A4` | 595 x 842 | 8.3 x 11.7 |
| `PageSize.A5` | 420 x 595 | 5.8 x 8.3 |
| `PageSize.LETTER` | 612 x 792 | 8.5 x 11 |
| `PageSize.LEGAL` | 612 x 1008 | 8.5 x 14 |
| `PageSize.TABLOID` | 792 x 1224 | 11 x 17 |

**Default:** `PageSize.A4`

**Custom sizes:**

```java
.pageSize(PageSize.of(720, 1080))    // 10 x 15 inches
```

### orientation

```java
.orientation(Orientation.LANDSCAPE)
```

| Value | Effect |
|-------|--------|
| `Orientation.PORTRAIT` | Height > width (default) |
| `Orientation.LANDSCAPE` | Width and height are swapped |

**Default:** `Orientation.PORTRAIT`

### margins

```java
.margins(top, right, bottom, left)    // CSS order, in PDF points
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `top` | `float` | Top margin in points |
| `right` | `float` | Right margin in points |
| `bottom` | `float` | Bottom margin in points |
| `left` | `float` | Left margin in points |

**Default:** 36 pt on all sides (0.5 inches)

Throws `IllegalArgumentException` if any value is negative.

> **Tip:** 72 points = 1 inch. Common margin sizes: 36 pt (0.5 in), 54 pt (0.75 in), 72 pt (1 in).

### dpi

```java
.dpi(120f)
```

The DPI used for converting Swing pixel coordinates to PDF points. Higher values produce smaller rendered output (more pixels per inch).

| Parameter | Type | Description |
|-----------|------|-------------|
| `dpi` | `float` | Dots per inch; must be positive |

**Default:** `96f` (standard desktop screen resolution)

The internal scale factor is `72 / dpi`. At the default of 96, one Swing pixel maps to 0.75 PDF points.

---

## Export Behavior

### exportMode

```java
.exportMode(ExportMode.DATA_REPORT)
```

| Value | Effect |
|-------|--------|
| `ExportMode.DATA_REPORT` | Export all data -- every row, every tab, full scroll content |
| `ExportMode.UI_SNAPSHOT` | Export only what is currently visible on screen |

**Default:** `ExportMode.DATA_REPORT`

See [Export Modes](export-modes.md) for a detailed comparison.

### enableAcroForm

```java
.enableAcroForm()
```

Turns on interactive PDF form field generation. When enabled, supported Swing input components (text fields, checkboxes, radio buttons, combo boxes) produce fillable widgets in the PDF.

**Default:** Disabled

See [AcroForm Fields](acroform.md) for the complete component mapping.

---

## PDF Metadata

These values are stored in the PDF document information dictionary and appear in the viewer's "Document Properties" dialog. They are also indexed by PDF search tools.

### title

```java
.title("Quarterly Sales Report")
```

### author

```java
.author("Jane Smith")
```

### subject

```java
.subject("Q2 2024 Financial Analysis")
```

### keywords

```java
.keywords("revenue, expenses, profit, Q2")
```

All metadata fields accept `null` (the default), which omits the field entirely. The `Creator` field is always set to `"swingtopdf"`.

---

## Headers and Footers

### header

```java
.header(HeaderFooter.of("Confidential").align(Alignment.RIGHT).color(Color.RED))
```

Renders a text band in the top margin of every page. See [Headers & Footers](headers-footers.md) for full styling options.

### footer

```java
.footer(HeaderFooter.of("Page {page} of {pages}"))
```

Renders a text band in the bottom margin of every page. Supports `{page}` and `{pages}` tokens.

---

## Custom Hooks

### withFontResolver

```java
.withFontResolver(font -> {
    if (font.getName().equals("MyCustomFont"))
        return Optional.of(Path.of("/fonts/MyCustomFont.ttf"));
    return Optional.empty();
})
```

Registers a custom font file resolver. Called when the library cannot automatically locate a `.ttf` or `.otf` file for an AWT `Font`. Returning `Optional.empty()` falls through to the next resolution strategy.

See [Fonts](fonts.md) for the full resolution chain.

### withImageHandler

```java
.withImageHandler((image, document) -> {
    if (image.getWidth() > 500)
        return Optional.of(JPEGFactory.createFromImage(document, image, 0.85f));
    return Optional.empty();
})
```

Registers a custom image embedding handler. Called whenever a `BufferedImage` needs to be embedded in the PDF (icons, rasterized components). Returning `Optional.empty()` uses the default lossless PNG encoding.

---

## Terminal Methods

These methods trigger the actual export. Call exactly one to produce output.

### export(Path)

```java
.export(Path.of("report.pdf"))
```

Writes the PDF to the given file path. If the file already exists, it is overwritten. Parent directories are **not** created automatically.

**Throws:**
- `SwingPdfExportException` -- if rendering or writing fails
- `LayoutException` -- if the component has no computable layout (zero size, headless with no explicit size)

### export(OutputStream)

```java
.export(outputStream)
```

Writes the PDF to the given stream. The caller is responsible for closing the stream.

**Throws:**
- `SwingPdfExportException` -- if rendering or writing fails
- `LayoutException` -- if the component has no computable layout

---

## Complete Example

```java
import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.*;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter.Alignment;
import java.awt.Color;

SwingPdfExporter.from(rootPanel)
    // Page layout
    .pageSize(PageSize.LETTER)
    .orientation(Orientation.LANDSCAPE)
    .margins(54, 54, 72, 54)          // extra bottom margin for footer
    .dpi(96f)

    // Export behavior
    .exportMode(ExportMode.DATA_REPORT)
    .enableAcroForm()

    // Metadata
    .title("Annual Report 2024")
    .author("Finance Team")
    .subject("Year-End Financial Summary")
    .keywords("annual, report, finance, 2024")

    // Headers and footers
    .header(HeaderFooter.of("CONFIDENTIAL")
        .align(Alignment.RIGHT)
        .fontSize(8f)
        .color(Color.RED))
    .footer(HeaderFooter.of("Page {page} of {pages}")
        .align(Alignment.CENTER)
        .fontSize(9f)
        .color(Color.GRAY)
        .backgroundColor(new Color(245, 245, 245))
        .height(20f))

    // Custom hooks
    .withFontResolver(font -> Optional.empty())
    .withImageHandler((img, doc) -> Optional.empty())

    // Export
    .export(Path.of("annual-report.pdf"));
```
