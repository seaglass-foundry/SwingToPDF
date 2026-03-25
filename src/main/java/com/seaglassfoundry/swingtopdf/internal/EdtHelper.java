package com.seaglassfoundry.swingtopdf.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.seaglassfoundry.swingtopdf.api.SwingPdfExportException;

/**
 * Utilities for marshalling work to the Event Dispatch Thread (EDT).
 *
 * <p>If the calling thread is already the EDT, the action runs inline.
 * Otherwise it is submitted via {@link SwingUtilities#invokeAndWait}.</p>
 *
 * <p>All Swing property reads in the rendering pipeline are channelled through
 * this class to ensure thread safety. Exceptions thrown on the EDT are
 * unwrapped and re-thrown on the calling thread, preserving the original
 * stack trace.</p>
 */
public final class EdtHelper {

    private EdtHelper() {}

    /**
     * Run {@code action} on the EDT and block until it completes.
     *
     * @throws SwingPdfExportException if the EDT invocation fails or is interrupted
     */
    public static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SwingPdfExportException("Interrupted while waiting for EDT", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new SwingPdfExportException("EDT execution failed", cause);
        }
    }

    /**
     * Call {@code action} on the EDT, block until it completes, and return the result.
     *
     * @throws SwingPdfExportException if the EDT invocation fails or is interrupted
     */
    public static <T> T callOnEdt(Callable<T> action) {
        AtomicReference<T>         result    = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();

        runOnEdt(() -> {
            try {
                result.set(action.call());
            } catch (Throwable t) {
                exception.set(t);
            }
        });

        Throwable t = exception.get();
        if (t != null) {
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error err) throw err;
            throw new SwingPdfExportException("EDT call failed", t);
        }
        return result.get();
    }
}
