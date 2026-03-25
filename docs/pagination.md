# Pagination

SwingToPDF automatically handles content that exceeds a single page. This guide explains how page breaks work, how to control them, and how margins and scaling interact.

---

## How It Works

When the rendered component tree is taller than the printable area of a single page, SwingToPDF splits the content across multiple pages. The algorithm is designed to produce clean breaks that never cut through the middle of a table row or a marked component.

### The Page Break Algorithm

For each page boundary, SwingToPDF uses a two-tier snapping strategy:

1. **Keep-together check** -- if the ideal break point would split a component marked with `KEEP_TOGETHER` (or an auto-detected atomic widget), the break snaps **up** to the top edge of that component, pushing it entirely to the next page.

2. **Row boundary snap** -- otherwise, the break snaps **down** to the last complete table row, list item, or tree row boundary that fits on the current page.

If neither applies (e.g., a large text block with no row structure), the break falls at the raw page boundary.

---

## Pagination Modes

The `PaginationMode` enum controls how oversized content is handled:

### AUTO (default)

```java
// AUTO is the default -- no need to set it explicitly
```

Content is split across as many pages as needed. Page breaks respect row boundaries and keep-together markers. This is the standard mode for reports and data exports.

### SINGLE_PAGE

Scales the entire component tree uniformly to fit on a single page. Useful for dashboards, forms, or any layout that should never span multiple pages.

### NONE

Renders at natural size with no pagination. Content that extends beyond the printable area is clipped. Use this only when you know your component fits on one page.

---

## Keep-Together

The keep-together mechanism prevents a component from being split across a page boundary.

### Explicit Keep-Together

Mark any `JComponent` with the `KEEP_TOGETHER` client property:

```java
JPanel card = new JPanel();
card.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);
```

When the paginator encounters this component at a page boundary, it pushes the entire component to the next page -- as long as it fits on a single page. If the component is taller than the printable area, it will be split regardless (there's no way to avoid it).

### Automatic Keep-Together

SwingToPDF automatically treats certain "atomic" widgets as keep-together, even without an explicit marker:

- Buttons, checkboxes, radio buttons
- Text fields, combo boxes, spinners
- Labels, separators, progress bars, sliders
- Small panels whose children are all under 80px tall

These are components that would look broken if split mid-way.

**Not** auto-detected (they handle their own row-level pagination):
- `JTable` -- breaks at row boundaries
- `JTree` -- breaks at row boundaries
- `JList` -- breaks at row boundaries
- `JScrollPane` / `JViewport` -- layout containers
- Large panels with tall children

### Example: Keeping a Form Card Together

```java
// A card with a title, two text fields, and a button
JPanel card = new JPanel(new GridBagLayout());
card.add(new JLabel("Contact Info"));
card.add(nameField);
card.add(emailField);
card.add(submitButton);

// Prevent this card from splitting across pages
card.putClientProperty(SwingPdfExporter.KEEP_TOGETHER, Boolean.TRUE);
```

---

## Margins

Margins define the space between the page edge and the printable content area. They are set in PDF points (1 pt = 1/72 inch), following CSS order:

```java
.margins(top, right, bottom, left)
```

**Default:** 36 pt on all sides (0.5 inches)

### Margin Layout

```
+--------------------------------------------------+
|                   top margin                      |
|  +--------------------------------------------+  |
|  |              header band                    |  |
|  +--------------------------------------------+  |
|  |                                            |  |
|l |           printable content                | r |
|e |               area                         | i |
|f |                                            | g |
|t |                                            | h |
|  |                                            | t |
|  +--------------------------------------------+  |
|  |              footer band                    |  |
|  +--------------------------------------------+  |
|                  bottom margin                    |
+--------------------------------------------------+
```

Headers and footers render **inside** the top and bottom margins respectively. The printable content area is the space between the margins, minus any header/footer height.

### Common Margin Presets

| Style | Values | Inches |
|-------|--------|--------|
| Narrow | `.margins(36, 36, 36, 36)` | 0.5" all sides |
| Normal | `.margins(54, 54, 54, 54)` | 0.75" all sides |
| Wide | `.margins(72, 72, 72, 72)` | 1" all sides |
| Print-friendly | `.margins(72, 54, 72, 54)` | 1" top/bottom, 0.75" left/right |

---

## Scaling and DPI

### Component Fitting

If the component tree is wider than the printable area, SwingToPDF scales it down uniformly to fit. This is automatic -- no configuration needed. The scale factor is:

```
fitScale = min(1.0, printableWidthPt / componentWidthPt)
```

Content is never scaled *up* -- only down, if necessary.

### DPI Conversion

The `dpi` setting controls how Swing pixels map to PDF points:

```
baseScale = 72 / dpi
```

| DPI | Scale | Effect |
|-----|-------|--------|
| 72 | 1.0 | 1 pixel = 1 point (large output) |
| 96 | 0.75 | Standard desktop (default) |
| 120 | 0.6 | Slightly smaller output |
| 144 | 0.5 | High-DPI scaling |

The final scale applied to all coordinates is `baseScale * fitScale`.

---

## Table Header Repetition

When a `JTable` spans multiple pages in `DATA_REPORT` mode, the column headers are automatically repeated at the top of each continuation page. This ensures every page of a long table is readable without flipping back to the first page.

The repeated header occupies the same height as the original and shifts the data rows down accordingly.

---

## Multi-Page Rendering Internals

For those who want to understand the mechanics:

1. The engine computes the total content height in Swing pixels
2. It collects all row boundaries (table rows, list items, tree rows) and keep-together bounds
3. It iterates page by page, computing `sliceTopPx` and `sliceBottomPx` for each page
4. Each page gets a clip rectangle covering only its slice of the content
5. All handlers render their content, but only the portion within the clip is visible
6. Headers and footers are rendered outside the clip, on top of the margin area
