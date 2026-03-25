# Export Modes

SwingToPDF offers two export modes that control *what content* gets exported. Choose the right mode based on whether you want a complete data export or a visual screenshot.

---

## DATA_REPORT (default)

```java
.exportMode(ExportMode.DATA_REPORT)
```

Renders **all data** in every component, regardless of scroll position, viewport size, or which tab is selected. This is the mode you want for reports, data exports, and printable documents.

### Behavior by Component

| Component | What gets exported |
|-----------|-------------------|
| **JTable** | Every row in the model, not just the visible viewport. Column headers repeat on continuation pages. |
| **JTree** | Every node in the `TreeModel`, fully expanded, regardless of which nodes are collapsed on screen. Indentation reflects tree depth. |
| **JList** | Every item in the `ListModel`. |
| **JTabbedPane** | All tabs are rendered sequentially, each preceded by a section header band showing the tab title. |
| **JScrollPane** | The full preferred-size content of the scrolled component, not just the visible viewport. |
| **JTextArea / JTextPane** | The complete text content, not just what fits in the viewport. |

### Pagination

Content taller than a single page is automatically paginated. Page breaks snap to table row boundaries and respect keep-together markers. See [Pagination](pagination.md) for details.

### Tab Section Headers

When a `JTabbedPane` is exported in `DATA_REPORT` mode, each tab's content is preceded by a styled section header:

- Background: a lighter tint of the component's background color
- Text: the tab title, rendered in bold
- Height: sized to the font metrics plus padding

This makes each tab's content clearly delineated in the output PDF.

---

## UI_SNAPSHOT

```java
.exportMode(ExportMode.UI_SNAPSHOT)
```

Renders **only what is currently visible** on screen -- equivalent to a high-fidelity vector screenshot. Scroll positions, viewport clips, and the selected tab are all preserved exactly as-is.

### Behavior by Component

| Component | What gets exported |
|-----------|-------------------|
| **JTable** | Only the rows visible in the viewport. |
| **JTree** | Only expanded, visible nodes. |
| **JList** | Only items visible in the viewport. |
| **JTabbedPane** | Only the currently selected tab's content. |
| **JScrollPane** | Only the visible viewport area. |
| **JTextArea / JTextPane** | Only the text visible in the viewport. |

### When to Use

- Capturing the exact current state of a UI for review or approval
- Generating a visual record of what the user is seeing right now
- Exporting dashboards or displays where scroll content is not relevant

---

## Side-by-Side Comparison

| Aspect | DATA_REPORT | UI_SNAPSHOT |
|--------|-------------|-------------|
| **Default?** | Yes | No |
| **Table rows** | All rows from model | Visible rows only |
| **Tree nodes** | Full model, expanded | Visible (expanded) nodes |
| **Tabs** | All tabs, stacked | Selected tab only |
| **Scroll content** | Full content | Viewport clip |
| **Pagination** | Automatic, multi-page | Automatic, multi-page |
| **Best for** | Reports, data export, printing | Screenshots, visual records |

---

## Setting the Mode

```java
SwingPdfExporter.from(panel)
    .exportMode(ExportMode.DATA_REPORT)    // explicit default
    .export(Path.of("report.pdf"));

SwingPdfExporter.from(panel)
    .exportMode(ExportMode.UI_SNAPSHOT)
    .export(Path.of("snapshot.pdf"));
```

Both modes still produce vector PDF output. The difference is purely about *which content* is included, not how it is rendered.
