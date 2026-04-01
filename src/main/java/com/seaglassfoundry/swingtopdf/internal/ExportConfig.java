package com.seaglassfoundry.swingtopdf.internal;

import java.util.Map;

import javax.swing.JComponent;

import com.seaglassfoundry.swingtopdf.api.ExportMode;
import com.seaglassfoundry.swingtopdf.api.FontResolver;
import com.seaglassfoundry.swingtopdf.api.HeaderFooter;
import com.seaglassfoundry.swingtopdf.api.ImageHandler;
import com.seaglassfoundry.swingtopdf.api.Orientation;
import com.seaglassfoundry.swingtopdf.api.PageSize;
import com.seaglassfoundry.swingtopdf.api.VectorComponentHandler;

/**
 * Immutable snapshot of all builder settings passed from
 * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter SwingPdfExporter} to {@link ExportEngine}.
 *
 * <p>Created once per export and shared (read-only) across all pages and handlers.
 * Margins are stored as a 4-element float array in CSS order:
 * {@code [top, right, bottom, left]}, all in PDF points.</p>
 *
 * @param root            the root component to export
 * @param pageSize        the page size (e.g. A4, Letter)
 * @param orientation     portrait or landscape
 * @param margins         page margins in points: [top, right, bottom, left]
 * @param dpi             DPI for Swing pixel to PDF point conversion (default 96)
 * @param exportMode      DATA_REPORT or UI_SNAPSHOT
 * @param fontResolver    user-supplied font resolver, or {@code null}
 * @param imageHandler    user-supplied image handler, or {@code null}
 * @param title           PDF document title, or {@code null}
 * @param author          PDF document author, or {@code null}
 * @param subject         PDF document subject, or {@code null}
 * @param keywords        PDF document keywords, or {@code null}
 * @param header          header band definition, or {@code null}
 * @param footer          footer band definition, or {@code null}
 * @param acroFormEnabled whether to generate interactive form fields
 * @param vectorHandlers  user-registered vector component handlers (type to handler)
 */
public record ExportConfig(
        JComponent root,
        PageSize    pageSize,
        Orientation orientation,
        float[]     margins,
        float       dpi,
        ExportMode  exportMode,
        FontResolver fontResolver,
        ImageHandler imageHandler,
        String       title,
        String       author,
        String       subject,
        String       keywords,
        HeaderFooter header,
        HeaderFooter footer,
        boolean      acroFormEnabled,
        Map<Class<?>, VectorComponentHandler> vectorHandlers
) {
    /** Effective page width in points, accounting for orientation. */
    public float effectivePageWidthPt() {
        return orientation == Orientation.LANDSCAPE
                ? pageSize.getHeightPt()
                : pageSize.getWidthPt();
    }

    /** Effective page height in points, accounting for orientation. */
    public float effectivePageHeightPt() {
        return orientation == Orientation.LANDSCAPE
                ? pageSize.getWidthPt()
                : pageSize.getHeightPt();
    }

    /** Printable width (page width minus left and right margins). */
    public float printableWidthPt() {
        return effectivePageWidthPt() - margins[1] - margins[3];
    }

    /** Printable height (page height minus top and bottom margins). */
    public float printableHeightPt() {
        return effectivePageHeightPt() - margins[0] - margins[2];
    }

    /** Convert a Swing pixel distance to PDF points using the configured DPI. */
    public float toPt(float pixels) {
        return pixels * 72f / dpi;
    }
}
