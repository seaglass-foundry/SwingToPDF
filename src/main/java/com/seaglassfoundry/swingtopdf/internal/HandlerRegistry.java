package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps component types to {@link ComponentHandler} instances.
 *
 * <p>Lookup walks the class hierarchy (superclasses only; not interfaces) until
 * a registered handler is found. Returns {@code null} if no handler is
 * registered for the type or any of its superclasses  -- the traverser will
 * then use the raster fallback.
 */
final class HandlerRegistry {

    private final Map<Class<?>, ComponentHandler> handlers = new LinkedHashMap<>();

    /** Register a handler for {@code type}. More-specific types should be registered first. */
    <T extends Component> void register(Class<T> type, ComponentHandler handler) {
        handlers.put(type, handler);
    }

    /**
     * Find the handler for {@code type} by walking up the class hierarchy.
     *
     * @return the handler, or {@code null} if none is registered
     */
    ComponentHandler lookup(Class<?> type) {
        Class<?> c = type;
        while (c != null && c != Object.class) {
            ComponentHandler h = handlers.get(c);
            if (h != null) return h;
            c = c.getSuperclass();
        }
        return null;
    }
}
