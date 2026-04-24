/**
 * Public API types for the swingtopdf library.
 *
 * <p>This package contains all the interfaces, enums, value classes, and exceptions
 * that constitute the library's public contract. Together with
 * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter} in the parent package, these types form the
 * complete API surface  -- everything in {@code com.seaglassfoundry.swingtopdf.internal} is deliberately
 * unexported from the module.
 *
 * <h2>Configuration types</h2>
 * <table>
 *   <caption>Configuration types used with the {@code SwingPdfExporter} builder</caption>
 *   <tr><th>Type</th><th>Purpose</th></tr>
 *   <tr><td>{@link PageSize}</td>
 *       <td>Standard and custom page dimensions (A3, A4, A5, Letter, Legal, Tabloid)</td></tr>
 *   <tr><td>{@link Orientation}</td>
 *       <td>Portrait or landscape page orientation</td></tr>
 *   <tr><td>{@link ExportMode}</td>
 *       <td>Full data export ({@code DATA_REPORT}) vs. visible-only screenshot ({@code UI_SNAPSHOT})</td></tr>
 *   <tr><td>{@link PaginationMode}</td>
 *       <td>Controls scaling and pagination behaviour (single page, auto, or no pagination)</td></tr>
 *   <tr><td>{@link HeaderFooter}</td>
 *       <td>Immutable header/footer band definition with token substitution (<code>&#123;page&#125;</code>, <code>&#123;pages&#125;</code>)</td></tr>
 * </table>
 *
 * <h2>Hook interfaces</h2>
 * <table>
 *   <caption>Functional interfaces for extending the export pipeline</caption>
 *   <tr><th>Interface</th><th>Purpose</th></tr>
 *   <tr><td>{@link FontResolver}</td>
 *       <td>Custom font file lookup  -- called when the built-in JVM and OS scan cannot find a {@code .ttf/.otf} file</td></tr>
 *   <tr><td>{@link ImageHandler}</td>
 *       <td>Custom image encoding  -- e.g. JPEG compression for photos instead of the default lossless PNG</td></tr>
 *   <tr><td>{@link VectorComponentHandler}</td>
 *       <td>Custom vector rendering  -- render custom-painted components (e.g. charts) via a PDF-backed {@code Graphics2D}
 *           instead of rasterising them</td></tr>
 * </table>
 *
 * <h2>Exception hierarchy</h2>
 * <pre>
 * RuntimeException
 *   +-- {@link SwingPdfExportException}       -- base for all library errors
 *         +-- {@link FontEmbeddingException}   -- font file cannot be embedded
 *         +-- {@link LayoutException}          -- component layout cannot be computed
 * </pre>
 *
 * <p>All exceptions are unchecked ({@link RuntimeException} subclasses) because most
 * callers cannot meaningfully recover from a rendering failure mid-export.
 *
 * @see com.seaglassfoundry.swingtopdf.SwingPdfExporter
 */
package com.seaglassfoundry.swingtopdf.api;
