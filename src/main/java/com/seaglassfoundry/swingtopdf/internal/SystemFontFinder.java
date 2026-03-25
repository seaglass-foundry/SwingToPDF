package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Font;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers system font files and resolves AWT {@link Font} instances to their
 * backing {@code .ttf} / {@code .otf} files.
 *
 * <h3>Platform support (v1.0)</h3>
 * <ul>
 *   <li><b>Windows</b> - {@code %WINDIR%\Fonts} and
 *       {@code %APPDATA%\Microsoft\Windows\Fonts}</li>
 *   <li><b>macOS</b>   - {@code /Library/Fonts}, {@code /System/Library/Fonts},
 *       and {@code ~/Library/Fonts}</li>
 *   <li><b>Linux</b>   - {@code /usr/share/fonts} and {@code ~/.fonts}</li>
 * </ul>
 *
 * <p>The first call to {@link #getInstance()} triggers a one-time directory
 * scan; the result is cached for the lifetime of the JVM process.
 */
public final class SystemFontFinder {

    private static final Logger log = LoggerFactory.getLogger(SystemFontFinder.class);

    // Lazy-init singleton  -- scan runs exactly once per JVM process
    private static final class Holder {
        static final SystemFontFinder INSTANCE = new SystemFontFinder();
        static { INSTANCE.buildIndex(); }
    }

    public static SystemFontFinder getInstance() {
        return Holder.INSTANCE;
    }

    /** family name (lower-case) -&gt; list of matching font files */
    private final Map<String, List<File>> familyIndex = new HashMap<>();

    private SystemFontFinder() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Scan system font directories and populate the index. Called once by the Holder. */
    private void buildIndex() {
        for (Path dir : systemFontDirs()) {
            if (Files.isDirectory(dir)) {
                indexDirectory(dir.toFile());
            }
        }
        log.debug("SystemFontFinder indexed {} font families", familyIndex.size());
    }

    /**
     * Attempt to find a font file for {@code font}.
     *
     * @return the font file, or {@link Optional#empty()} if not found
     */
    public Optional<File> findFile(Font font) {
        String family = font.getFamily(Locale.ROOT).toLowerCase(Locale.ROOT);
        List<File> candidates = familyIndex.getOrDefault(family, List.of());
        if (candidates.isEmpty()) {
            log.debug("No font file found for family '{}'", font.getFamily());
            return Optional.empty();
        }
        // Prefer a file whose name contains style hints (Bold, Italic, etc.)
        File best = pickBestMatch(font, candidates);
        log.debug("Resolved '{}' -&gt; {}", font.getFontName(), best.getName());
        return Optional.of(best);
    }

    // -----------------------------------------------------------------------
    // Indexing
    // -----------------------------------------------------------------------

    private void indexDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                indexDirectory(f);
            } else if (isFontFile(f)) {
                indexFontFile(f);
            }
        }
    }

    private void indexFontFile(File file) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, file);
            String family = font.getFamily(Locale.ROOT).toLowerCase(Locale.ROOT);
            familyIndex.computeIfAbsent(family, k -> new ArrayList<>()).add(file);
        } catch (Exception e) {
            log.trace("Skipping font file {} ({})", file.getName(), e.getMessage());
        }
    }

    private static boolean isFontFile(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".ttf") || name.endsWith(".otf");
    }

    // -----------------------------------------------------------------------
    // Best-match selection
    // -----------------------------------------------------------------------

    private static File pickBestMatch(Font font, List<File> candidates) {
        boolean bold   = font.isBold();
        boolean italic = font.isItalic();

        File best = candidates.get(0);
        int  bestScore = score(best.getName(), bold, italic);

        for (int i = 1; i < candidates.size(); i++) {
            int s = score(candidates.get(i).getName(), bold, italic);
            if (s > bestScore) {
                best = candidates.get(i);
                bestScore = s;
            }
        }
        return best;
    }

    private static int score(String filename, boolean wantBold, boolean wantItalic) {
        String lower = filename.toLowerCase(Locale.ROOT);
        int score = 0;
        if (wantBold   && (lower.contains("bold")   || lower.contains("-bd"))) score += 2;
        if (wantItalic && (lower.contains("italic")  || lower.contains("oblique") || lower.contains("-it") || lower.contains("-ob"))) score += 2;
        if (!wantBold   && !lower.contains("bold"))   score += 1;
        if (!wantItalic && !lower.contains("italic")) score += 1;
        return score;
    }

    // -----------------------------------------------------------------------
    // Platform font directories
    // -----------------------------------------------------------------------

    private static List<Path> systemFontDirs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<Path> dirs = new ArrayList<>();

        if (os.contains("win")) {
            String windir = System.getenv("WINDIR");
            if (windir == null) windir = "C:\\Windows";
            dirs.add(Path.of(windir, "Fonts"));

            String appdata = System.getenv("APPDATA");
            if (appdata != null) {
                dirs.add(Path.of(appdata, "Microsoft", "Windows", "Fonts"));
            }
        } else if (os.contains("mac")) {
            dirs.add(Path.of("/Library/Fonts"));
            dirs.add(Path.of("/System/Library/Fonts"));
            dirs.add(Path.of(System.getProperty("user.home"), "Library", "Fonts"));
        } else {
            // Linux / other Unix
            dirs.add(Path.of("/usr/share/fonts"));
            dirs.add(Path.of("/usr/local/share/fonts"));
            dirs.add(Path.of(System.getProperty("user.home"), ".fonts"));
            dirs.add(Path.of(System.getProperty("user.home"), ".local", "share", "fonts"));
        }
        return dirs;
    }
}
