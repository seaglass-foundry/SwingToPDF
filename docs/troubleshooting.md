# Troubleshooting

Common issues, error messages, and how to resolve them.

---

## Exceptions

### LayoutException

**Message:** *"Component has no computable layout"* (or similar)

**Cause:** The root component has zero size and SwingToPDF can't determine how to lay it out. This typically happens when:
- The component has never been added to a visible frame
- Running in a headless environment (CI, Docker) with no display
- The component has zero preferred size

**Fix:**

```java
// Option 1: Set an explicit size
panel.setSize(800, 600);

// Option 2: Pack a frame to trigger layout
JFrame frame = new JFrame();
frame.setContentPane(panel);
frame.pack();

// Option 3: Manual layout
panel.setSize(panel.getPreferredSize());
panel.doLayout();
```

SwingToPDF attempts automatic layout via `addNotify()` + `validate()`, but this requires the component to have a non-zero preferred size.

---

### SwingPdfExportException

**Message:** Varies -- wraps any rendering or I/O failure.

**Common causes:**
- Output file path is not writable (permissions, directory doesn't exist)
- Parent directory of the output path doesn't exist (create it first)
- I/O error writing to the output stream
- Internal rendering error

**Fix:** Check the exception's `getCause()` for the underlying error. Most commonly this is an `IOException`.

```java
try {
    exporter.export(path);
} catch (SwingPdfExportException e) {
    Throwable cause = e.getCause();
    if (cause instanceof IOException) {
        // Check file permissions, disk space, path validity
    }
}
```

---

### FontEmbeddingException

**Message:** *"Failed to embed font: [font name]"*

**Cause:** A font file was found but couldn't be loaded or parsed.

**Common causes:**
- Corrupt or truncated font file
- Unsupported font format (bitmap `.fon` files are not supported)
- File permission issues on the font file

**Fix:**
- Check the `getCause()` for the specific parsing error
- Use `e.getFont()` to identify which font failed
- Register a `FontResolver` to point to a known-good copy of the font
- As a workaround, change the component's font to one that can be embedded

> **Note:** This exception is only thrown when a file is found but can't be parsed. When a font file can't be *found*, the library silently falls back to a standard PDF Type-1 font.

---

## Font Issues

### Fonts look different in the PDF

**Symptom:** Text in the PDF uses Helvetica/Times/Courier instead of the font set on the Swing component.

**Cause:** The library couldn't find the `.ttf` or `.otf` file for the font and fell back to a standard PDF base font.

**Fix:**

1. **Add `--add-opens` flags** to enable JVM font path resolution:
   ```
   --add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
   --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
   ```

2. **Check SLF4J logs** for font fallback warnings. Enable debug logging to see which fonts are being resolved and which are falling back.

3. **Register a custom FontResolver** to explicitly provide font file paths:
   ```java
   .withFontResolver(font -> {
       if (font.getName().equals("ProblematicFont"))
           return Optional.of(Path.of("/path/to/font.ttf"));
       return Optional.empty();
   })
   ```

4. **Install the font on the system.** On Linux CI servers, install `fontconfig` and font packages:
   ```bash
   apt-get install fontconfig fonts-dejavu
   ```

### Missing `--add-opens` warnings

**Symptom:** Warnings about illegal reflective access or missing opens.

**Fix:** Add all three `--add-opens` flags. See [Getting Started](getting-started.md#jvm-flags).

The library works without these flags but uses slower font resolution (system directory scan) and may miss fonts loaded via `Font.createFont()`.

---

## Rendering Issues

### Custom JPanel appears as an image

**Symptom:** A `JPanel` subclass with custom painting is rasterized instead of rendered as vectors.

**Cause:** SwingToPDF detects `paintComponent` overrides and rasterizes the entire panel, because custom painting operations can't be decomposed into vector PDF primitives.

**This is expected behavior.** The raster fallback uses high-quality antialiasing and produces good-looking output. If you need vector output, consider:
- Moving the custom painting into a component that SwingToPDF has a handler for
- Restructuring the panel so the custom painting is in a small child component, while labels, text, and other standard components remain vectorized

### Table columns are scaled down

**Symptom:** A JTable with many columns appears smaller than expected.

**Cause:** The table is wider than the printable area, so SwingToPDF scales it down to fit. A warning is logged when this happens.

**Fix:**
- Switch to `Orientation.LANDSCAPE` for more horizontal space
- Use a larger page size (`PageSize.TABLOID` or a custom size)
- Increase margins to give more effective width (counterintuitive, but sometimes narrower margins help)
- Reduce the number of visible columns or their preferred widths

### Scroll content is clipped

**Symptom:** Only the visible portion of a scrollable component appears in the PDF.

**Cause:** You're using `ExportMode.UI_SNAPSHOT`, which intentionally captures only what's visible.

**Fix:** Switch to `ExportMode.DATA_REPORT` to export all content:
```java
.exportMode(ExportMode.DATA_REPORT)
```

---

## AcroForm Issues

### Form fields don't appear

**Symptom:** The PDF renders correctly but has no interactive form fields.

**Cause:** `enableAcroForm()` was not called on the builder.

**Fix:**
```java
SwingPdfExporter.from(form)
    .enableAcroForm()    // required for interactive fields
    .export(path);
```

### Radio buttons act independently

**Symptom:** Radio buttons in the PDF can all be selected simultaneously instead of being mutually exclusive.

**Cause:** The radio buttons are not in the same `ButtonGroup` in Swing, or group discovery failed.

**Fix:**
1. Make sure all related radio buttons are added to the same `ButtonGroup`:
   ```java
   ButtonGroup group = new ButtonGroup();
   group.add(option1);
   group.add(option2);
   group.add(option3);
   ```

2. Add the `--add-opens java.desktop/javax.swing` flag to enable reflection-based group discovery. Without it, the library uses behavioral testing, which is less reliable.

### Field names are auto-generated

**Symptom:** Form fields are named `field_1`, `field_2`, etc. instead of meaningful names.

**Cause:** The Swing components don't have names set.

**Fix:** Call `setName()` on each form component before export:
```java
textField.setName("customer_name");
checkBox.setName("terms_accepted");
```

---

## Performance

### First export is slow

**Symptom:** The first export takes noticeably longer than subsequent exports.

**Cause:** One-time initialization costs:
- System font directory scan (building the font index)
- PDFBox initialization

**Fix:** This is normal. Subsequent exports in the same JVM session are faster because the font index is cached. To warm up, you can do a dummy export at application startup.

### Large tables produce large PDFs

**Symptom:** Exporting a table with thousands of rows produces a very large PDF.

**Cause:** Each row generates vector drawing operations. Very large tables produce many PDF content streams.

**Fix:**
- Consider paginating the data at the application level (export only the relevant subset)
- If file size is critical, the `ImageHandler` hook can be used to JPEG-compress rasterized components

---

## Headless Environments

### Export fails in Docker / CI

**Symptom:** `LayoutException` or rendering errors when running without a display.

**Fix:**

1. Set the component size explicitly before export:
   ```java
   panel.setSize(800, 600);
   ```

2. If using AWT/Swing features that require a display toolkit, set headless mode:
   ```java
   System.setProperty("java.awt.headless", "true");
   ```

3. On Linux, install X11 libraries if needed:
   ```bash
   apt-get install libxrender1 libxtst6 libxi6
   ```

4. Alternatively, use Xvfb (virtual framebuffer) for full Swing toolkit support:
   ```bash
   xvfb-run java -jar myapp.jar
   ```
