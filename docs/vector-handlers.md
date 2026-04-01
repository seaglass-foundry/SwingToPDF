# Vector Component Handlers

By default, components that perform custom painting (overriding `paintComponent`) are rasterized -- captured as a bitmap image and embedded in the PDF. This means text is not selectable and shapes lose quality when zoomed.

The `registerHandler()` API lets you provide a custom rendering function that draws into a PDF-backed `Graphics2D`. All drawing operations are emitted as vector PDF primitives: text remains selectable and searchable, shapes are resolution-independent, and file sizes are typically smaller.

---

## When to Use

Use `registerHandler()` when you have a component that:

- Performs custom painting via `paintComponent` (e.g. chart libraries like JFreeChart, custom diagram renderers, gauge widgets)
- Already has a `draw(Graphics2D, Rectangle2D)` method or equivalent
- Produces output where text selectability or zoom quality matters

You do **not** need `registerHandler()` for standard Swing components -- SwingToPDF already has dedicated vector handlers for 20+ built-in component types.

---

## Basic Usage

```java
import com.seaglassfoundry.swingtopdf.SwingPdfExporter;
import com.seaglassfoundry.swingtopdf.api.PageSize;

SwingPdfExporter.from(panel)
    .pageSize(PageSize.A4)
    .registerHandler(MyChartPanel.class, (comp, g2, bounds) -> {
        ((MyChartPanel) comp).draw(g2, bounds);
    })
    .export(Path.of("report.pdf"));
```

The handler is a `VectorComponentHandler` -- a functional interface with a single method:

```java
void render(Component component, Graphics2D g2, Rectangle2D bounds) throws IOException;
```

| Parameter | Description |
|-----------|-------------|
| `component` | The Swing component instance being rendered |
| `g2` | A PDF-backed `Graphics2D` -- all drawing operations produce vector PDF output |
| `bounds` | The component's bounds as `(0, 0, width, height)` |

---

## JFreeChart Example

```java
import org.jfree.chart.ChartPanel;

SwingPdfExporter.from(dashboard)
    .pageSize(PageSize.A4)
    .registerHandler(ChartPanel.class, (comp, g2, bounds) -> {
        ((ChartPanel) comp).getChart().draw(g2, bounds);
    })
    .export(Path.of("dashboard.pdf"));
```

The chart's text labels, axis titles, and legend entries will be selectable in the PDF. Lines and shapes remain sharp at any zoom level.

---

## Multiple Handlers

You can register handlers for different component types. Each handler is matched by exact type or superclass:

```java
SwingPdfExporter.from(panel)
    .registerHandler(BarChartPanel.class, (comp, g2, bounds) -> {
        ((BarChartPanel) comp).drawChart(g2, bounds);
    })
    .registerHandler(LineChartPanel.class, (comp, g2, bounds) -> {
        ((LineChartPanel) comp).drawChart(g2, bounds);
    })
    .export(path);
```

If you register a handler for a parent class, it also matches subclasses. For example, registering a handler for `AbstractChartPanel` will match `BarChartPanel extends AbstractChartPanel`.

---

## Overriding Built-in Handlers

User-registered handlers take priority over the library's built-in handlers. If you register a handler for `JLabel`, your handler will be called instead of the built-in label renderer:

```java
.registerHandler(JLabel.class, (comp, g2, bounds) -> {
    // Custom rendering replaces the built-in JLabel handler
    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
    g2.drawString(((JLabel) comp).getText(), 10, 30);
})
```

---

## Children Are Not Traversed

When a vector handler is registered for a component type, that component is treated as a **leaf node**. Its child components are **not** automatically rendered -- the handler is responsible for drawing everything within the component's bounds.

If your component is a container with children you want rendered, you must draw them yourself in the handler body. For example:

```java
.registerHandler(MyContainer.class, (comp, g2, bounds) -> {
    // Draw the container's own content
    g2.setColor(Color.WHITE);
    g2.fill(bounds);

    // Manually paint children into the same Graphics2D
    for (Component child : ((Container) comp).getComponents()) {
        Graphics2D childG2 = (Graphics2D) g2.create(
                child.getX(), child.getY(), child.getWidth(), child.getHeight());
        child.paint(childG2);
        childG2.dispose();
    }
})
```

> **Tip:** If you only need vector output for a specific child (like a chart) inside a larger panel, register the handler on the chart component's class rather than the parent container. This way the library handles the container and siblings normally, and only the chart is vector-rendered.

---

## Coordinate System

The `bounds` rectangle always starts at `(0, 0)` with the component's width and height. The `Graphics2D` is pre-configured so that drawing within `bounds` maps to the component's correct position on the PDF page. You do not need to account for the component's position in the Swing hierarchy.

---

## Supported Drawing Operations

The PDF-backed `Graphics2D` supports standard Java 2D operations:

- **Shapes:** `fill()`, `draw()` with `Rectangle2D`, `Ellipse2D`, `Line2D`, `Path2D`, `Arc2D`, etc.
- **Text:** `drawString()` -- rendered as real PDF text (selectable, searchable)
- **Fonts:** `setFont()` -- standard Java fonts are mapped to PDF fonts
- **Colors and paints:** `setColor()`, `setPaint()` including `GradientPaint`
- **Strokes:** `setStroke()` with `BasicStroke` (width, cap, join, dash patterns)
- **Rendering hints:** antialiasing, text antialiasing, etc.
- **Images:** `drawImage()` -- embedded in the PDF
- **Transforms:** `translate()`, `scale()`, `rotate()`
- **Clipping:** `setClip()`, `clip()`

---

## Null Safety

Both `type` and `handler` must be non-null. Passing `null` throws `NullPointerException`:

```java
// Both throw NullPointerException
.registerHandler(null, (c, g, b) -> {})
.registerHandler(JPanel.class, null)
```

---

## Comparison: With and Without Vector Handlers

| Aspect | Without handler (raster) | With `registerHandler` (vector) |
|--------|--------------------------|--------------------------------|
| Text | Not selectable | Selectable and searchable |
| Zoom quality | Pixelates at high zoom | Sharp at any zoom level |
| File size | Larger (embedded bitmap) | Smaller (vector operations) |
| Shapes | Fixed resolution | Resolution-independent |
| Setup | None (automatic fallback) | Requires a handler function |
