# Fonts

SwingToPDF embeds TrueType and OpenType fonts directly in the PDF to ensure text renders identically on every system. This guide explains how font resolution works, how to provide custom fonts, and what happens when a font can't be found.

---

## How Font Resolution Works

When a Swing component uses an AWT `Font`, SwingToPDF needs to find the corresponding `.ttf` or `.otf` file to embed it in the PDF. The resolution chain tries four strategies, in order:

### 1. Custom FontResolver (if registered)

Your application-supplied resolver gets first priority:

```java
.withFontResolver(font -> {
    if (font.getName().equals("BrandFont"))
        return Optional.of(Path.of("/fonts/BrandFont-Regular.ttf"));
    return Optional.empty();    // pass to next strategy
})
```

Return `Optional.empty()` to skip this font and let the library try the next strategy.

### 2. JVM Internal Path (reflection)

The fastest path. SwingToPDF uses reflection to read the JVM's internal font-to-file mapping (`Font.getFont2D()` -> `PhysicalFont.platName`). This works for:

- System fonts resolved by the JVM
- Fonts loaded via `Font.createFont(int, InputStream)`
- Any font the JVM has already resolved to a file

**Requires `--add-opens` flags:**

```
--add-opens java.desktop/java.awt=com.seaglassfoundry.swingtopdf
--add-opens java.desktop/sun.font=com.seaglassfoundry.swingtopdf
```

If these flags are missing, this strategy is silently skipped and resolution falls through to the next strategy.

### 3. System Font Directory Scan

A one-time scan of standard OS font directories:

| OS | Directories scanned |
|----|-------------------|
| **Windows** | `%WINDIR%\Fonts`, `%APPDATA%\Microsoft\Windows\Fonts` |
| **macOS** | `/Library/Fonts`, `/System/Library/Fonts`, `~/Library/Fonts` |
| **Linux** | `/usr/share/fonts`, `~/.fonts`, `~/.local/share/fonts` |

The scan builds an in-memory index mapping font family names to file paths. When multiple files match a family, a scoring function prioritizes filenames containing "Bold" or "Italic" to match the requested style.

This scan happens once per JVM lifetime and is cached. Subsequent exports reuse the index.

### 4. Standard PDF Type-1 Fallback

If none of the above strategies locate the font file, SwingToPDF falls back to one of the 14 standard PDF base fonts. The mapping is based on the font family name:

| Font family contains | Fallback |
|---------------------|----------|
| "mono" or "courier" | Courier, Courier-Bold, Courier-Oblique, Courier-BoldOblique |
| "serif" (but not "sans") | Times-Roman, Times-Bold, Times-Italic, Times-BoldItalic |
| Everything else | Helvetica, Helvetica-Bold, Helvetica-Oblique, Helvetica-BoldOblique |

Bold and italic styles are mapped to their corresponding Type-1 variants.

> **Note:** When the fallback is used, a warning is logged via SLF4J. The PDF will render correctly, but the font will look different from the original Swing UI.

---

## Font Caching

Resolved fonts are cached per export session (one cache per `export()` call). The cache key is `fontName + "-" + style`, so the same logical font is only resolved and embedded once even if it's used by dozens of components.

---

## Subset Embedding

TrueType and OpenType fonts are embedded as **subsets** -- only the glyphs actually used in the document are included. This is handled automatically by PDFBox and significantly reduces PDF file size compared to full font embedding.

---

## Writing a Custom FontResolver

The `FontResolver` interface is a `@FunctionalInterface` with a single method:

```java
@FunctionalInterface
public interface FontResolver {
    Optional<Path> resolve(Font font);
}
```

### Common Use Cases

**Bundled application fonts:**

```java
.withFontResolver(font -> {
    String name = font.getName().toLowerCase();
    if (name.contains("opensans"))
        return Optional.of(Path.of("fonts/OpenSans-Regular.ttf"));
    if (name.contains("robotomono"))
        return Optional.of(Path.of("fonts/RobotoMono-Regular.ttf"));
    return Optional.empty();
})
```

**Style-aware resolution:**

```java
.withFontResolver(font -> {
    String base = "fonts/MyFont";
    String suffix = "";
    if (font.isBold() && font.isItalic()) suffix = "-BoldItalic";
    else if (font.isBold()) suffix = "-Bold";
    else if (font.isItalic()) suffix = "-Italic";
    else suffix = "-Regular";

    Path path = Path.of(base + suffix + ".ttf");
    return Files.exists(path) ? Optional.of(path) : Optional.empty();
})
```

**Classpath resources:**

```java
// Extract the font to a temp file first
.withFontResolver(font -> {
    if (!font.getName().equals("MyFont")) return Optional.empty();
    try {
        Path temp = Files.createTempFile("font-", ".ttf");
        try (InputStream is = getClass().getResourceAsStream("/fonts/MyFont.ttf")) {
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        temp.toFile().deleteOnExit();
        return Optional.of(temp);
    } catch (IOException e) {
        return Optional.empty();
    }
})
```

---

## Exceptions

### FontEmbeddingException

Thrown when a font file is found but cannot be loaded or parsed. This is a subclass of `SwingPdfExportException` (unchecked).

```java
try {
    exporter.export(path);
} catch (FontEmbeddingException e) {
    Font problematic = e.getFont();
    System.err.println("Could not embed: " + problematic.getFontName());
}
```

**Common causes:**
- Corrupt or truncated font file
- Unsupported font format (e.g., bitmap `.fon` files)
- File permission issues

> **Note:** The library does *not* throw `FontEmbeddingException` when a font file can't be *found*. It silently falls back to a Type-1 font and logs a warning. The exception is only thrown when a file is found but can't be embedded.

---

## Tips

- Always add the `--add-opens` JVM flags for the best font resolution (strategy 2 is the fastest and most reliable)
- If you're using custom or bundled fonts, register a `FontResolver` to avoid relying on system font directories
- Check your SLF4J logs for font fallback warnings -- if you see "falling back to Helvetica for X", that means the font file wasn't found
- On Linux CI servers, install the `fontconfig` and `fonts-dejavu` packages to ensure basic font availability
- Fonts loaded via `Font.createFont()` are resolved via the JVM internal path (strategy 2) -- no extra configuration needed if `--add-opens` flags are set
