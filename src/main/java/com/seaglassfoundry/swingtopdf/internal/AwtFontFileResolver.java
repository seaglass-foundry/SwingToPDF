package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Font;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a {@link Font} to its backing font file by reflecting into JVM
 * internals ({@code java.awt.Font#getFont2D()} -&gt;
 * {@code sun.font.PhysicalFont#platName}).
 *
 * <p>This is more reliable than directory scanning because the JVM has already
 * resolved the mapping.  It also works for fonts loaded via
 * {@link Font#createFont(int, java.io.InputStream)}, since the JVM writes font
 * bytes to a temp file and stores that path in {@code platName}.
 *
 * <p>Requires:
 * <pre>
 *   --add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
 *   --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
 * </pre>
 *
 * <p>If those opens are not present the class initialiser catches the
 * exception, sets {@link #AVAILABLE} to {@code false}, and all calls to
 * {@link #resolve} return {@link Optional#empty()} silently.
 */
public final class AwtFontFileResolver {

    private static final Logger log = LoggerFactory.getLogger(AwtFontFileResolver.class);

    private static final Method GET_FONT2D;
    private static final Field  PLAT_NAME;
    public  static final boolean AVAILABLE;

    static {
        Method gf   = null;
        Field  pn   = null;
        boolean ok  = false;
        try {
            // java.awt.Font#getFont2D()  (private, returns sun.font.Font2D)
            gf = Font.class.getDeclaredMethod("getFont2D");
            gf.setAccessible(true);

            // sun.font.PhysicalFont#platName  (platform file path)
            Class<?> physicalFontClass = Class.forName("sun.font.PhysicalFont");
            pn = findField(physicalFontClass, "platName");
            pn.setAccessible(true);

            ok = true;
            log.debug("AwtFontFileResolver: JVM-internal font resolution available");
        } catch (Exception e) {
            log.debug("AwtFontFileResolver unavailable ({}). " +
                      "Add --add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf for faster font lookup.",
                      e.getMessage());
        }
        GET_FONT2D = gf;
        PLAT_NAME  = pn;
        AVAILABLE  = ok;
    }

    private AwtFontFileResolver() {}

    /**
     * Attempt to find the font file for {@code font} via JVM internals.
     *
     * @return the font file, or {@link Optional#empty()} if not resolvable
     */
    public static Optional<File> resolve(Font font) {
        if (!AVAILABLE) return Optional.empty();
        try {
            Object font2D = GET_FONT2D.invoke(font);
            // Only PhysicalFont subclasses have platName
            if ((font2D == null) || !PLAT_NAME.getDeclaringClass().isInstance(font2D)) return Optional.empty();

            String platName = (String) PLAT_NAME.get(font2D);
            if (platName == null || platName.isBlank()) return Optional.empty();

            File f = new File(platName);
            if (f.exists() && isFontFile(f)) {
                log.debug("AwtFontFileResolver: '{}' -&gt; {}", font.getFontName(), f.getName());
                return Optional.of(f);
            }
        } catch (Exception e) {
            log.debug("AwtFontFileResolver: reflection failed for '{}': {}",
                      font.getFontName(), e.getMessage());
        }
        return Optional.empty();
    }

    // -----------------------------------------------------------------------

    /** Walk the class hierarchy looking for a field with the given name. */
    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " not found in " + cls.getName() + " or its superclasses");
    }

    private static boolean isFontFile(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".ttf") || n.endsWith(".otf");
    }
}
