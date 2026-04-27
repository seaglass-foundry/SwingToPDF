# swingtopdf

**Java Swing to True Vector PDF**

swingtopdf converts any Java Swing component tree into genuine vector PDF output. Text remains selectable, graphics stay sharp at any zoom level, and the output is a fraction of the size of rasterized alternatives. Built on Apache PDFBox.

## Features

- **True vector output** -- text, shapes, and lines render as PDF drawing operations, not images
- **20+ component handlers** -- JTable, JTree, JList, JTabbedPane, JTextPane, JEditorPane, and more, each with specialized rendering
- **Vector component handlers** -- register custom `Graphics2D` renderers for components like JFreeChart so their output is vector PDF (selectable text, sharp shapes) instead of rasterised bitmaps
- **AcroForm interactive fields** -- export JTextField, JCheckBox, JRadioButton, JComboBox, JTextArea, and JPasswordField as fillable PDF form fields
- **Rich text and HTML** -- JTextPane and JEditorPane content renders with formatting preserved
- **Two export modes** -- `DATA_REPORT` exports all data (full scroll content, all tabs) with auto-pagination; `UI_SNAPSHOT` captures only what is visible on screen
- **Pagination** -- automatic page breaks with configurable margins, headers, footers, and page-number tokens (`{page}`, `{pages}`)
- **Header/footer styling** -- configurable alignment, font size, text color, background color, and band height
- **PDF bookmarks** -- auto-generated from JTabbedPane tabs
- **Keep-together** -- mark any component with `SwingPdfExporter.KEEP_TOGETHER` to prevent it from splitting across pages
- **Font embedding** -- TTF and OTF fonts embedded directly in the PDF with a pluggable `FontResolver`
- **Image deduplication** -- identical images are embedded once and referenced multiple times
- **PDF metadata** -- title, author, subject, and keywords
- **Java module system** -- fully modular (`com.seaglassfoundry.swingtopdf`), exports only the public API

## Requirements

- Java 17 or later
- Apache PDFBox 3.x (transitive dependency)

## Maven

```xml
<dependency>
    <groupId>com.seaglassfoundry</groupId>
    <artifactId>swingtopdf</artifactId>
    <version>1.3.0</version>
</dependency>
```

## Quick Start

```java
import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.Orientation;

SwingPdfExporter.from(myPanel)
    .pageSize(PageSize.A4)
    .orientation(Orientation.PORTRAIT)
    .margins(36, 36, 36, 36)
    .title("My Report")
    .export(Path.of("report.pdf"));
```

To render a custom-painted component as vector PDF:

```java
SwingPdfExporter.from(chartPanel)
    .pageSize(PageSize.A4)
    .registerHandler(ChartPanel.class, (comp, g2, bounds) -> {
        ((ChartPanel) comp).getChart().draw(g2, bounds);
    })
    .export(Path.of("chart.pdf"));
```

To enable fillable form fields:

```java
SwingPdfExporter.from(myForm)
    .pageSize(PageSize.LETTER)
    .enableAcroForm()
    .export(Path.of("form.pdf"));
```

## JVM Flags

swingtopdf uses `--add-opens` to access Swing internals for accurate rendering. Add these flags when running your application:

```
--add-opens java.desktop/javax.swing=com.seaglassfoundry.swingtopdf
--add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
--add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
```

If you are not using the Java module system (classpath mode), replace `com.seaglassfoundry.swingtopdf` with `ALL-UNNAMED`.

## License

swingtopdf is dual-licensed:

- **AGPL-3.0** -- free for open-source projects. If you use swingtopdf in your project, your project must also be released under the AGPL-3.0. See [LICENSE](LICENSE) for the full text.
- **Commercial license** -- a one-time, perpetual license for use in closed-source or proprietary products. No royalties, no recurring fees, scoped per major version. See [LICENSE-COMMERCIAL.md](LICENSE-COMMERCIAL.md) for details.

**Building a commercial product?** Purchase a commercial license at [seaglassfoundry.com](https://seaglassfoundry.com).

## Documentation

For the full manual -- export modes, pagination, headers/footers, AcroForm fields, font embedding, component reference, and more -- see the [Documentation](docs/index.md).

## Links

- Website: [seaglassfoundry.com](https://seaglassfoundry.com)
- Documentation: [docs/index.md](docs/index.md)
- Commercial licensing: [seaglassfoundry.com](https://seaglassfoundry.com)
- Issues: [GitHub Issues](https://github.com/seaglass-foundry/swingtopdf/issues)
- Contact: [rich@seaglassfoundry.com](mailto:rich@seaglassfoundry.com)
