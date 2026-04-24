# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-04-24

### Added

- `HeaderFooter.html(String)` factory -- a new header/footer mode that renders inline HTML as styled vector text. Supports bold, italic, underline, strikethrough, `<font color>` / `<font size>`, inline CSS (`color`, `font-size`, `font-weight`, `font-style`, `text-decoration`), block breaks (`<br>`, `<p>`, `<div>`, `<h1>`--`<h6>`). Wraps automatically to fit the printable width.
- `HeaderFooter.of(JComponent)` factory -- render any Swing component as a header or footer through the normal handler pipeline. Works with JLabel, JPanel, or any custom-painted component registered via `registerHandler`. `{page}` / `{pages}` tokens inside JLabel / JTextComponent text are resolved at render time and restored after each page.
- `HeaderFooter.wrap(boolean)` -- opt-in greedy word-wrap for plain-text bands (default `false`, preserves 1.1 behaviour). HTML and component bands always wrap.
- `HeaderFooter.Mode` enum (`TEXT`, `HTML`, `COMPONENT`) and `mode()` accessor for introspection.
- Repeated JTable column headers on continuation pages: multi-page tables now show their column header in a reserved band at the top of every continuation page (not just page 1), with rows rendering at their natural positions below the band.

### Fixed

- Last row of a multi-page JTable is no longer clipped out of the content area on continuation pages. Previously the row's cell fills / grid lines / text glyphs landed below the page clip while the text operators still emitted (extractable but invisible). Page-break snapping now reserves space for the repeated column header.

### Changed

- `HeaderFooter.align(Alignment)` is now a no-op in `Mode.COMPONENT`; the component spans the full printable width so opaque backgrounds, borders, and `BorderLayout` children stretch edge to edge. Use the component's own alignment API (e.g. `JLabel.setHorizontalAlignment`, or the enclosing layout manager) to position visible content within the band.

## [1.1.0] - 2026-03-31

### Added

- Vector component handler API (`registerHandler`) for rendering custom-painted components as vector PDF instead of rasterised bitmaps
- New public interface `VectorComponentHandler` — a functional interface that receives a PDF-backed `Graphics2D` for drawing
- Text drawn via vector handlers remains selectable; shapes are resolution-independent
- User-registered handlers override built-in handlers for the same component type
- New dependency: PdfBoxGraphics2D (Graphics2D bridge for vector rendering via PDFBox)

## [1.0.1] - 2026-03-25

### Changed

- PDFBox module dependency is no longer transitive; consumers who directly use PDFBox types must add their own dependency

## [1.0.0] - 2026-03-24

### Added

- Core Swing-to-PDF export engine with true vector output via Apache PDFBox 3.x
- Fluent builder API (`SwingPdfExporter.from(component)...export(path)`)
- 20+ component handlers: JTable, JTree, JList, JTabbedPane, JTextPane, JEditorPane, JComboBox, JCheckBox, JRadioButton, JButton, JToggleButton, JTextField, JTextArea, JPasswordField, JSlider, JSpinner, JProgressBar, JScrollPane, JSplitPane, JLayeredPane, JInternalFrame, and more
- AcroForm support for interactive PDF form fields (JTextField, JCheckBox, JRadioButton, JComboBox, JPasswordField, JTextArea)
- Rich text and HTML rendering for JTextPane and JEditorPane
- Two export modes: DATA_REPORT (full data, all tabs, auto-pagination) and UI_SNAPSHOT (visible content only)
- Automatic pagination with configurable margins
- Configurable headers and footers with token substitution ({page}, {pages}), alignment, font size, text color, background color, and explicit band height
- PDF bookmarks auto-generated from JTabbedPane tabs
- KEEP_TOGETHER client property to prevent page splits on individual components
- TTF/OTF font embedding with pluggable FontResolver
- Image deduplication (identical images embedded once)
- PDF metadata: title, author, subject, keywords
- Configurable page sizes (A3, A4, A5, Letter, Legal, Tabloid, custom) and orientations
- Pluggable ImageHandler for custom image encoding
- Java 9+ module system support (module `com.seaglassfoundry.swingtopdf`)
- Requires Java 17+
