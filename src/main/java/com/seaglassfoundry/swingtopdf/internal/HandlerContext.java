package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Container;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Context object passed to every {@link ComponentHandler}.
 * Provides access to the page writer, font mapper, configuration,
 * and the traversal delegate for recursing into child components.
 */
final class HandlerContext {

    private final PdfPageWriter             writer;
    private final DefaultFontMapper         fontMapper;
    private final ExportConfig              config;
    private final PDDocument                document;
    private final PDPage                    currentPage;
    private final float                     sliceTopPx;
    private final float                     sliceBottomPx;
    private final float                     pageWidthPx;
    private final DeduplicatingImageEncoder imageEncoder;
    private final AcroFormEmitter           acroFormEmitter; // null if acroForm disabled

    // Set after construction to avoid circular dependency
    private ComponentTraverser traverser;

    HandlerContext(PdfPageWriter writer,
                   DefaultFontMapper fontMapper,
                   ExportConfig config,
                   PDDocument document,
                   PDPage currentPage,
                   float sliceTopPx,
                   float sliceBottomPx,
                   float pageWidthPx,
                   DeduplicatingImageEncoder imageEncoder,
                   AcroFormEmitter acroFormEmitter) {
        this.writer          = writer;
        this.fontMapper      = fontMapper;
        this.config          = config;
        this.document        = document;
        this.currentPage     = currentPage;
        this.sliceTopPx      = sliceTopPx;
        this.sliceBottomPx   = sliceBottomPx;
        this.pageWidthPx     = pageWidthPx;
        this.imageEncoder    = imageEncoder;
        this.acroFormEmitter = acroFormEmitter;
    }

    void setTraverser(ComponentTraverser traverser) {
        this.traverser = traverser;
    }

    PdfPageWriter             writer()           { return writer; }
    DefaultFontMapper         fontMapper()       { return fontMapper; }
    ExportConfig              config()           { return config; }
    PDDocument                document()         { return document; }
    PDPage                    currentPage()      { return currentPage; }
    DeduplicatingImageEncoder imageEncoder()     { return imageEncoder; }
    /** Non-null only when {@code ExportConfig.acroFormEnabled()} is true. */
    AcroFormEmitter           acroFormEmitter()  { return acroFormEmitter; }
    /** Absolute pixel Y of the top of the current page slice (0 on page 1). */
    float                     sliceTopPx()       { return sliceTopPx; }
    /** Absolute pixel Y of the bottom of the current page slice. */
    float                     sliceBottomPx()    { return sliceBottomPx; }
    /** Printable page width in Swing-pixel units. */
    float                     pageWidthPx()      { return pageWidthPx; }

    /**
     * Recurse into all direct children of {@code parent}.
     * Container handlers call this after rendering their own background/border.
     *
     * @param parent the container whose children should be traversed
     * @param absX   absolute X of parent's top-left in root pixel space
     * @param absY   absolute Y of parent's top-left in root pixel space
     */
    void traverseChildren(Container parent, int absX, int absY) throws IOException {
        traverser.traverseChildren(parent, absX, absY);
    }

    /**
     * Render {@code comp} at an explicit absolute position, bypassing the
     * component's own {@code getX()}/{@code getY()} offset and {@code isVisible()}.
     * Used by handlers that need to place content at a computed position
     * (e.g. {@code JTabbedPane} in DATA_REPORT mode stacking tabs vertically).
     */
    void traverseAt(java.awt.Component comp, int absX, int absY) throws IOException {
        traverser.traverseAt(comp, absX, absY);
    }
}
