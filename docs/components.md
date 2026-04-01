# Supported Components

SwingToPDF includes dedicated vector handlers for 20+ Swing components. Each handler reads the component's state directly and emits PDF drawing operations -- text is real text, shapes are real vector paths. Components without a dedicated handler are rasterized at high quality as a fallback.

---

## Text Components

### JLabel

Full vector rendering with support for:
- Plain text and HTML content (inline styles: bold, italic, underline, strikethrough, color, size)
- Multi-line text (from `<br>` tags in HTML)
- Icons (rasterized via the icon painter)
- Horizontal alignment: LEFT, CENTER, RIGHT
- Vertical alignment: TOP, CENTER, BOTTOM
- Opaque background fill
- Border rendering

HTML content is parsed and rendered with per-run styling preserved -- each styled span gets its own font, color, and decorations.

### JTextField / JFormattedTextField

- Single-line text with horizontal alignment
- Background fill (uses inactive background color when disabled)
- Border rendering
- AcroForm: single-line text field widget

### JPasswordField

- Echo character substitution (displays dots/asterisks, not the actual text)
- Background and border rendering
- AcroForm: password text field (value cleared in PDF for security)

### JTextArea

- Multi-line text rendering
- Word-wrap support when `getLineWrap()` is true
- Wrap modes: word boundaries (`getWrapStyleWord()`) or character boundaries
- AcroForm: multiline text field widget
- **DATA_REPORT:** full content height rendered
- **UI_SNAPSHOT:** viewport-clipped height

### JTextPane

Styled text rendering that walks the `StyledDocument` element tree:
- Per-run styling: font family, size, bold, italic, foreground color, background highlight
- Text decorations: underline, strikethrough
- Embedded icons and components within the document
- Soft word-wrap handling (runs split at line boundaries)
- Exact positioning via `modelToView2D()`

### JEditorPane

HTML rendering with a three-pass approach:
1. Block backgrounds (table row and cell colors from CSS)
2. Table borders (HTML table structure with cell allocation)
3. Vector text leaves (CSS-aware font and color resolution)

Supported HTML features:
- `<table>` with borders, row/cell backgrounds, and column structure
- `<th>` bold rendering
- CSS font inheritance via `StyleSheet`
- CSS color inheritance
- Inline styling

> **Note:** Only `HTMLEditorKit` content is rendered in vector. RTF or other editor kits fall back to raster.

---

## Data Components

### JTable

The most feature-rich handler. Renders:
- All cell content via registered `TableCellRenderer` instances
- `JLabel` cell renderers are delegated to the label handler (vector)
- `AbstractButton` cell renderers are delegated to the button handler (vector)
- Other renderers are rasterized at cell dimensions
- Horizontal and vertical grid lines (color from `getGridColor()`)
- Per-row heights (from `getRowHeight(row)`)
- Column width scaling (auto-scales if wider than page; logs a warning)
- **Column header repetition** on continuation pages in DATA_REPORT mode

**Mode differences:**
- **DATA_REPORT:** Every row in the table model, regardless of scroll position
- **UI_SNAPSHOT:** Only rows visible in the viewport

### JTableHeader

Rendered automatically with its parent JTable:
- Per-column cells via registered `TableCellRenderer`
- Column separator lines
- Bottom horizontal rule separating header from data
- Shares column width scaling with JTableHandler
- Repeats on every page when a table spans multiple pages

### JTree

- Per-row rendering via registered `TreeCellRenderer`
- Indentation based on node depth (16px per level)
- `JLabel` cell renderers are delegated to the label handler (vector)
- Non-JLabel renderers: background fill + `toString()` text

**Mode differences:**
- **DATA_REPORT:** Recursively walks the entire `TreeModel`, rendering every node regardless of expansion state
- **UI_SNAPSHOT:** Only visible (expanded) nodes, using `getRowBounds()` for positioning

### JList

- Per-item rendering via registered `ListCellRenderer`
- Cell height: `getFixedCellHeight()` if set, otherwise renderer preferred size, otherwise 16px default
- `JLabel` cell renderers are delegated to the label handler (vector)
- Non-JLabel renderers: background fill + `toString()` text

**Mode differences:**
- **DATA_REPORT:** Every item in the `ListModel`
- **UI_SNAPSHOT:** Only items visible in the viewport

---

## Buttons and Selection Controls

### JButton / JToggleButton

- Background fill (toggle buttons use selection color when selected)
- Border rendering
- Icon rendering (rasterized)
- Text layout via `SwingUtilities.layoutCompoundLabel()`
- HTML text stripped to plain text
- Disabled state: uses disabled text color

### JCheckBox

- Stroked square indicator (sized from UIManager, default 13px)
- Checkmark lines when selected
- Label text to the right of the indicator
- AcroForm: checkbox widget with hand-drawn vector appearance

### JRadioButton

- Stroked circle indicator (same size as checkbox)
- Filled inner circle when selected (inset 3px)
- Label text to the right of the indicator
- AcroForm: radio button widget; buttons in the same `ButtonGroup` become a single PDF field
- Group discovery via reflection or behavioral fallback

### JComboBox

- Background fill with selected item text
- Arrow column with downward chevron
- Outer border and vertical separator
- AcroForm: combo box widget with all model items as options
- Always shows closed (selected value only)

---

## Container Components

### JPanel / Generic Containers

- Background fill (if opaque)
- Recursive child rendering
- Custom `paintComponent` detection: if a JPanel subclass overrides `paintComponent`, the entire panel is rasterized (since custom painting can't be decomposed into vector operations)

### Border Support

Containers render their borders with full vector support for:

| Border Type | Rendering |
|-------------|-----------|
| `LineBorder` | Stroked rectangle with configurable color and thickness |
| `EtchedBorder` | RAISED/LOWERED with highlight and shadow colors |
| `BevelBorder` | RAISED/LOWERED with outer/inner highlight/shadow |
| `MatteBorder` | Colored fill at inset margins |
| `TitledBorder` | Inner border + title text with configurable position and justification |
| `CompoundBorder` | Outer + inner borders (recursive) |
| `EmptyBorder` | No visible rendering |
| Other borders | Rasterized as fallback |

`TitledBorder` supports all six title positions (ABOVE_TOP, TOP, BELOW_TOP, ABOVE_BOTTOM, BOTTOM, BELOW_BOTTOM) and three justifications (LEFT, CENTER, RIGHT).

### JSplitPane

- Left/right or top/bottom content panels rendered via child traversal
- Vector divider bar with configurable thickness
- Grab dots on the divider (5-dot pattern when divider is 4px or wider)
- Divider color: slightly darker than the background

### JTabbedPane

- Tab bar with all tab labels and icons visible
- Tab styling: selected tab highlighted, unselected tabs dimmed

**Mode differences:**
- **DATA_REPORT:** All tab contents rendered sequentially, each preceded by a styled section header band with the tab title
- **UI_SNAPSHOT:** Only the selected tab's content

### JScrollPane

The scroll pane itself is a layout container. Its behavior depends on the export mode:
- **DATA_REPORT:** The full preferred-size content of the scrolled component
- **UI_SNAPSHOT:** Only the visible viewport area

The scroll bars themselves are rendered by the JScrollBarHandler.

### JInternalFrame

- Content pane rendered as vector (recursive child traversal)
- Vector title bar with:
  - Background color (active/inactive state from UIManager)
  - Frame icon (if present)
  - Title text (bold, white)
  - Window control button placeholders (close, maximize, iconify -- based on frame properties)
- Outer frame border

---

## Value Widgets

### JScrollBar

- Track background
- Proportional thumb at current scroll position
- End-cap arrow buttons with chevron icons
- Orientation: horizontal or vertical

### JSlider

- Track bar with filled portion up to current value
- Thumb (white square with dark border)
- Major and minor tick marks (if `getPaintTicks()`)
- Tick labels from `getLabelTable()` (if `getPaintLabels()`)
- Orientation: horizontal or vertical

### JProgressBar

- Background fill
- Proportional progress fill
- Border
- String overlay (if `isStringPainted()`): percentage text with auto-contrasting color
- Orientation: horizontal (fill from left) or vertical (fill from bottom)

### JSpinner

- Editor field (delegates to JTextComponentHandler if JFormattedTextField is found)
- Arrow column with increment/decrement chevrons
- Background color from UIManager

### JSeparator

- Single hairline (1px) in the component's foreground color (default: gray)
- Horizontal or vertical orientation

---

## Vector Component Handlers

Components that perform custom painting (e.g. chart libraries, diagram renderers, or any `JComponent` subclass that overrides `paintComponent`) would normally be rasterized. You can register a `VectorComponentHandler` via `registerHandler()` to render them as vector PDF instead:

```java
SwingPdfExporter.from(panel)
    .registerHandler(ChartPanel.class, (comp, g2, bounds) -> {
        ((ChartPanel) comp).getChart().draw(g2, bounds);
    })
    .export(path);
```

The handler receives a PDF-backed `Graphics2D`. All drawing operations (shapes, text, images) are emitted as vector PDF primitives -- text remains selectable and shapes are resolution-independent.

User-registered handlers override built-in handlers for the same component type. See the [Vector Handlers](vector-handlers.md) guide for details.

---

## Raster Fallback

Any component without a dedicated handler or registered vector handler -- or a `JPanel` subclass with a custom `paintComponent` override and no registered handler -- is rendered via the raster fallback:

1. A `BufferedImage` is created at the component's dimensions
2. The component's `paint(Graphics)` method draws into it with high-quality rendering hints (antialiasing, text antialiasing, quality rendering)
3. The image is embedded in the PDF via the deduplicating image encoder

This ensures every Swing component produces output, even custom or third-party components. The trade-off is that rasterized content is not selectable or searchable as text.

> **Tip:** If you have a custom-painted component and want vector output, register a `VectorComponentHandler` for it instead of accepting the raster fallback. See [Vector Handlers](vector-handlers.md).

---

## Image Deduplication

When multiple components use the same `BufferedImage` instance (e.g., the same `ImageIcon` on multiple labels), the image is embedded in the PDF **once** and referenced multiple times. This is based on object identity (`==`), not visual equality.

To benefit from deduplication, reuse the same icon instances across your UI:

```java
ImageIcon icon = new ImageIcon("status.png");
label1.setIcon(icon);    // same instance
label2.setIcon(icon);    // embedded once, referenced twice
```
