package com.seaglassfoundry.swingtopdf.api;

/**
 * Thrown when a component's layout cannot be computed before rendering.
 *
 * <p>This typically occurs when exporting a component that:
 * <ul>
 *   <li>Has never been added to a visible container (no peer, no layout)</li>
 *   <li>Has zero preferred size and no explicit size set via {@code setSize()}</li>
 *   <li>Is running in a headless environment where layout requires a display</li>
 * </ul>
 *
 * <p><b>Resolution:</b> call {@code component.setSize(width, height)} or
 * {@code pack()} on the enclosing frame before exporting. The library
 * attempts automatic layout via {@code addNotify()} + {@code validate()},
 * but this requires the component to have a non-zero preferred size.</p>
 *
 * @since 1.0.0
 */
public class LayoutException extends SwingPdfExportException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a layout exception with the given detail message.
     *
     * @param message description of the layout failure
     */
    public LayoutException(String message) {
        super(message);
    }

    /**
     * Constructs a layout exception with the given detail message and cause.
     *
     * @param message description of the layout failure
     * @param cause   the underlying exception
     */
    public LayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
