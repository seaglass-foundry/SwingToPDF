package com.seaglassfoundry.swingtopdf.api;

/**
 * Base unchecked exception for all errors raised by the swingtopdf library.
 *
 * <p>Subclasses distinguish specific failure categories:
 * <ul>
 *   <li>{@link FontEmbeddingException}  -- a font file could not be loaded or embedded</li>
 *   <li>{@link LayoutException}  -- the component tree has no computable layout</li>
 * </ul>
 *
 * <p>This is an unchecked exception because most callers cannot meaningfully recover
 * from a rendering failure mid-export. The cause chain typically contains the
 * underlying {@code IOException} from PDFBox.</p>
 *
 * @since 1.0.0
 */
public class SwingPdfExportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an exception with the given detail message.
     *
     * @param message description of the export failure
     */
    public SwingPdfExportException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the given detail message and cause.
     *
     * @param message description of the export failure
     * @param cause   the underlying exception (typically an {@code IOException})
     */
    public SwingPdfExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
