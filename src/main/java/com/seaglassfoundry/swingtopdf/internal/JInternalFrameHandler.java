package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;

/**
 * Renders a {@link JInternalFrame} as PDF vector graphics.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Fill the frame background.</li>
 *   <li>Recurse into all children via {@link HandlerContext#traverseChildren}.
 *       This renders the content pane as vector and rasterizes the L&amp;F title
 *       pane component.</li>
 *   <li>Draw a clean vector title bar <em>on top</em>, covering the rasterized
 *       one. Title bar height is derived from the content pane's Y offset inside
 *       the frame.</li>
 *   <li>Stroke a 1 px outer frame border last so it sits above all content.</li>
 * </ol>
 *
 * <p>The title bar shows the frame icon (if any), the title string, and small
 * hollow rectangles as placeholders for the close/maximise/iconify buttons.
 */
final class JInternalFrameHandler implements ComponentHandler {

    static final JInternalFrameHandler INSTANCE = new JInternalFrameHandler();

    /** Fallback title bar height (px) when UIManager has no value. */
    private static final int DEFAULT_TITLE_BAR_HEIGHT = 22;

    private JInternalFrameHandler() {}

    private static final Color FALLBACK_ACTIVE_BG   = new Color(0x4169AE);
    private static final Color FALLBACK_INACTIVE_BG = new Color(0x7F7F7F);

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JInternalFrame frame)) return;
        int w = frame.getWidth();
        int h = frame.getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Background fill
        Color bg = frame.getBackground() != null ? frame.getBackground() : Color.WHITE;
        ctx.writer().fillRect(absX, absY, w, h, bg);

        // 2. Recurse into all children (content pane -&gt; vector; title pane -&gt; rasterized)
        ctx.traverseChildren(frame, absX, absY);

        // 3. Vector title bar on top
        Container contentPane = frame.getContentPane();
        int titleBarH = contentPaneOffset(contentPane, frame);
        if (titleBarH > 0) {
            renderTitleBar(frame, absX, absY, w, titleBarH, ctx);
        }

        // 4. Outer frame border
        ctx.writer().strokeRect(absX, absY, w, h, Color.GRAY, 1f);
    }

    // -----------------------------------------------------------------------

    private static void renderTitleBar(JInternalFrame frame, int absX, int absY,
                                        int w, int titleBarH,
                                        HandlerContext ctx) throws IOException {
        Color activeBg   = uiColor("InternalFrame.activeTitleBackground",   FALLBACK_ACTIVE_BG);
        Color inactiveBg = uiColor("InternalFrame.inactiveTitleBackground", FALLBACK_INACTIVE_BG);
        Color titleBg    = frame.isSelected() ? activeBg : inactiveBg;
        Color titleFg    = uiColor("InternalFrame.activeTitleForeground", Color.WHITE);
        ctx.writer().fillRect(absX, absY, w, titleBarH, titleBg);

        int textStartX = absX + 4;

        // Frame icon (left side)
        Icon icon = frame.getFrameIcon();
        if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            int iconY = absY + (titleBarH - icon.getIconHeight()) / 2;
            IconPainter.render(icon, frame, absX + 4, iconY, ctx);
            textStartX += icon.getIconWidth() + 3;
        }

        // Window control button placeholders (right side)  -- close, then max, then min
        int btnSize = Math.max(4, titleBarH - 6);
        int btnY    = absY + (titleBarH - btnSize) / 2;
        int btnX    = absX + w - 4 - btnSize;
        if (frame.isClosable()) {
            ctx.writer().strokeRect(btnX, btnY, btnSize, btnSize, titleFg, 0.5f);
            btnX -= btnSize + 3;
        }
        if (frame.isMaximizable()) {
            ctx.writer().strokeRect(btnX, btnY, btnSize, btnSize, titleFg, 0.5f);
            btnX -= btnSize + 3;
        }
        if (frame.isIconifiable()) {
            ctx.writer().strokeRect(btnX, btnY, btnSize, btnSize, titleFg, 0.5f);
            btnX -= btnSize + 3;
        }

        // Title text (between icon and buttons, clipped by button area)
        String title = frame.getTitle();
        if (title != null && !title.isBlank()) {
            Font font = frame.getFont() != null
                    ? frame.getFont().deriveFont(Font.BOLD)
                    : new Font(Font.SANS_SERIF, Font.BOLD, 11);
            FontMetrics fm      = frame.getFontMetrics(font);
            int         baselineY = absY + (titleBarH - fm.getHeight()) / 2 + fm.getAscent();
            int         maxTextX  = btnX - 4; // don't run into button area
            if (textStartX < maxTextX) {
                ctx.writer().drawText(title, ctx.fontMapper().resolve(font),
                                      font.getSize2D(), titleFg, textStartX, baselineY);
            }
        }
    }

    /**
     * Returns the Y offset (in frame-local pixels) of the content pane's top edge.
     * This equals the title bar height for standard L&amp;F layouts.
     * Falls back to 22 px if the content pane cannot be found in the hierarchy.
     */
    private static int contentPaneOffset(Container contentPane, JInternalFrame frame) {
        int y = 0;
        Component c = contentPane;
        while (c != null && c != frame) {
            y += c.getY();
            c = c.getParent();
        }
        if (y > 0) return y;
        // Fall back to UIManager title-button height, then to a reasonable default
        int uiH = UIManager.getInt("InternalFrame.titleButtonHeight");
        return uiH > 0 ? uiH : DEFAULT_TITLE_BAR_HEIGHT;
    }

    private static Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }
}
