# Getting Started

This guide walks you through adding SwingToPDF to your project and producing your first PDF export.

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.seaglassfoundry</groupId>
    <artifactId>swingtopdf</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.seaglassfoundry:swingtopdf:1.0.1")
```

### Gradle (Groovy DSL)

```groovy
implementation 'com.seaglassfoundry:swingtopdf:1.0.1'
```

Apache PDFBox 3.x is pulled in automatically as a transitive dependency -- you do not need to declare it separately.

---

## JVM Flags

SwingToPDF uses `--add-opens` to access Swing internals for accurate font resolution and rendering. Add these flags to your application's JVM arguments:

```
--add-opens java.desktop/javax.swing=com.seaglassfoundry.swingtopdf
--add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
--add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
```

If you are running on the classpath (not using the Java module system), replace the module name with `ALL-UNNAMED`:

```
--add-opens java.desktop/javax.swing=ALL-UNNAMED
--add-opens java.desktop/java.awt=ALL-UNNAMED
--add-opens java.desktop/sun.font=ALL-UNNAMED
```

> **What happens without these flags?** The library still works, but font resolution falls back to scanning system font directories instead of reading JVM-internal font paths. This is slower on the first export and may miss fonts loaded via `Font.createFont()`. See the [Fonts](fonts.md) guide for details.

---

## Your First Export

```java
import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.Orientation;

// Any JComponent -- a JPanel, JTable, JTabbedPane, etc.
JPanel panel = buildYourUI();

SwingPdfExporter.from(panel)
    .pageSize(PageSize.A4)
    .orientation(Orientation.PORTRAIT)
    .margins(36, 36, 36, 36)    // 0.5 inch on all sides
    .title("My First PDF")
    .export(Path.of("output.pdf"));
```

That's it. Open `output.pdf` and you'll see your Swing UI rendered as crisp, selectable vector content.

---

## Exporting to a Stream

If you need to write the PDF to a network response, byte array, or any other `OutputStream`:

```java
try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    SwingPdfExporter.from(panel)
        .pageSize(PageSize.LETTER)
        .export(baos);

    byte[] pdfBytes = baos.toByteArray();
    // send to HTTP response, save to database, etc.
}
```

> **Note:** The caller is responsible for closing the stream. SwingToPDF writes to it but does not close it.

---

## Exporting a Fillable Form

To produce a PDF with interactive form fields (text inputs, checkboxes, dropdowns):

```java
SwingPdfExporter.from(myForm)
    .pageSize(PageSize.LETTER)
    .enableAcroForm()
    .export(Path.of("form.pdf"));
```

See the [AcroForm Fields](acroform.md) guide for the full component-to-field mapping and naming conventions.

---

## Export Modes

By default, SwingToPDF uses `DATA_REPORT` mode, which exports *all* data -- every table row, every tab, the full scroll content. If you want to capture only what is currently visible on screen, switch to `UI_SNAPSHOT`:

```java
import com.seaglassfoundry.swingtopdf.api.ExportMode;

SwingPdfExporter.from(panel)
    .exportMode(ExportMode.UI_SNAPSHOT)
    .export(Path.of("screenshot.pdf"));
```

See the [Export Modes](export-modes.md) guide for a detailed comparison.

---

## Headless / CI Environments

SwingToPDF works in headless environments (CI servers, Docker containers) as long as the component has a valid layout. Before exporting, make sure your component has been sized:

```java
// Option 1: explicit size
panel.setSize(800, 600);

// Option 2: pack a frame (triggers layout)
JFrame frame = new JFrame();
frame.setContentPane(panel);
frame.pack();
```

If the component has zero size and no preferred size, SwingToPDF throws a `LayoutException` with guidance on how to fix it.

---

## Vector Rendering for Custom Components

Components that perform custom painting (like chart libraries) are rasterized by default. Use `registerHandler()` to render them as vector PDF:

```java
SwingPdfExporter.from(panel)
    .pageSize(PageSize.A4)
    .registerHandler(ChartPanel.class, (comp, g2, bounds) -> {
        ((ChartPanel) comp).getChart().draw(g2, bounds);
    })
    .export(Path.of("chart.pdf"));
```

Text drawn via the handler remains selectable and shapes are resolution-independent. See the [Vector Handlers](vector-handlers.md) guide for details.

---

## Next Steps

- [Export Modes](export-modes.md) -- understand `DATA_REPORT` vs `UI_SNAPSHOT`
- [Builder Options](builder-options.md) -- explore every configuration option
- [Supported Components](components.md) -- see how each Swing component renders
- [Vector Handlers](vector-handlers.md) -- render custom-painted components as vector PDF
