package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.io.IOException;

/**
 * Renders a single component into a PDF page by reading the component's
 * properties and writing PDF primitives via {@link HandlerContext}.
 *
 * <p>Handlers never call into Swing's paint machinery  -- they read state
 * (text, font, color, model data) and write directly to the content stream.
 */
@FunctionalInterface
interface ComponentHandler {
    /**
     * @param comp  the component to render
     * @param absX  absolute X of the component's top-left corner in root pixel space
     * @param absY  absolute Y of the component's top-left corner in root pixel space
     * @param ctx   rendering context (writer, font mapper, traversal delegate)
     */
    void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException;
}
