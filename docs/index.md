# SwingToPDF Documentation

**Version 1.3.1**

SwingToPDF converts any Java Swing component tree into genuine vector PDF output. Text remains selectable, graphics stay sharp at any zoom level, and the output is a fraction of the size of rasterized alternatives.

---

## Guides

| Guide | Description |
|-------|-------------|
| [Getting Started](getting-started.md) | Installation, dependencies, JVM flags, and your first export |
| [Export Modes](export-modes.md) | `DATA_REPORT` vs `UI_SNAPSHOT` -- when to use each and how they differ |
| [Builder Options](builder-options.md) | Complete reference for every `SwingPdfExporter` builder method |
| [Pagination](pagination.md) | How content flows across pages, margins, keep-together, and scaling |
| [Headers & Footers](headers-footers.md) | Page banners with dynamic page numbers, styling, alignment, and per-page variation |
| [AcroForm Fields](acroform.md) | Exporting interactive, fillable PDF form fields from Swing components |
| [Fonts](fonts.md) | Font embedding, the resolution chain, custom `FontResolver`, and fallbacks |
| [Supported Components](components.md) | Every Swing component with handler-specific rendering details |
| [Vector Handlers](vector-handlers.md) | Render custom-painted components as vector PDF with `registerHandler` |
| [Troubleshooting](troubleshooting.md) | Common issues, error messages, and how to fix them |

---

## At a Glance

```java
SwingPdfExporter.from(myPanel)
    .pageSize(PageSize.LETTER)
    .orientation(Orientation.LANDSCAPE)
    .margins(36, 36, 36, 36)
    .title("Quarterly Report")
    .header(HeaderFooter.of("Quarterly Report").align(Alignment.LEFT))
    .footer(HeaderFooter.of("Page {page} of {pages}"))
    .export(Path.of("report.pdf"));
```

## Key Concepts

- **Vector-first rendering** -- handlers read Swing component state directly and emit PDF drawing operations. Text is real PDF text (selectable, searchable); shapes are real vector paths.
- **Vector component handlers** -- register a `VectorComponentHandler` via `registerHandler()` to render custom-painted components (charts, diagrams) as vector PDF instead of rasterised bitmaps.
- **Raster fallback** -- components without a dedicated handler or registered vector handler (or with custom `paintComponent` overrides) are rasterized at high quality and embedded as images.
- **Two export modes** -- `DATA_REPORT` exports *all* data (every table row, every tab, full scroll content). `UI_SNAPSHOT` captures exactly what is visible on screen.
- **Automatic pagination** -- content taller than one page is split intelligently at row boundaries and keep-together markers.
- **AcroForm support** -- opt-in interactive PDF form fields that map Swing input components to their PDF equivalents.

## Requirements

- **Java 17** or later
- **Apache PDFBox 3.x** (pulled in automatically as a transitive Maven dependency)

## Links

- [Maven Central](https://central.sonatype.com/artifact/com.seaglassfoundry/swingtopdf)
- [GitHub Repository](https://github.com/seaglass-foundry/SwingToPDF)
- [Issue Tracker](https://github.com/seaglass-foundry/SwingToPDF/issues)
- [Commercial Licensing](https://seaglassfoundry.com)
