package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

import com.seaglassfoundry.swingtopdf.api.ExportMode;

/**
 * Renders a {@link JTabbedPane} as PDF vector graphics.
 *
 * <h3>UI_SNAPSHOT mode</h3>
 * Draws the tab bar and recurses into the selected tab's content panel only,
 * matching what is visible on screen.
 *
 * <h3>DATA_REPORT mode</h3>
 * Draws the tab bar as a visual index, then renders every tab's content panel
 * stacked vertically below it. Each tab is preceded by a titled section header
 * so the reader can identify which tab each block of content belongs to.
 * Non-selected tab panels are rendered via {@link HandlerContext#traverseAt},
 * which bypasses {@code isVisible()}.
 */
final class JTabbedPaneHandler implements ComponentHandler {

    static final JTabbedPaneHandler INSTANCE = new JTabbedPaneHandler();

    private static final int SECTION_PAD = 4;  // vertical padding above/below section header text

    private JTabbedPaneHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JTabbedPane tp = (JTabbedPane) comp;
        if (tp.getTabCount() == 0) return;

        // Whole-component background
        if (tp.isOpaque() && tp.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, tp.getWidth(), tp.getHeight(), tp.getBackground());
        }

        // Draw the tab bar (all tabs)
        renderTabBar(tp, absX, absY, ctx);

        if (ctx.config().exportMode() == ExportMode.DATA_REPORT) {
            renderAllTabs(tp, absX, absY, ctx);
        } else {
            renderSelectedTab(tp, absX, absY, ctx);
        }
    }

    // -----------------------------------------------------------------------
    // DATA_REPORT: all tabs stacked vertically
    // -----------------------------------------------------------------------

    /**
     * Render every tab panel stacked vertically below the tab bar.
     * Each panel is preceded by a titled section header.
     */
    private void renderAllTabs(JTabbedPane tp, int absX, int absY,
                                HandlerContext ctx) throws IOException {
        Component selectedComp = tp.getSelectedComponent();
        if (selectedComp == null) return;

        // Content area geometry  -- all panels share the same bounds after layout
        int contentX = absX + selectedComp.getX();
        int contentH = selectedComp.getHeight();
        int cursorY  = absY + selectedComp.getY(); // top of content area = bottom of tab bar

        for (int i = 0; i < tp.getTabCount(); i++) {
            Component panel = tp.getComponentAt(i);
            if (panel == null) continue;

            // Use content height from panel if valid; fall back to selected panel height
            int panelH = panel.getHeight() > 0 ? panel.getHeight() : contentH;

            // Section header
            int sectionH = sectionHeaderHeight(tp);
            renderSectionHeader(tp.getTitleAt(i), absX, cursorY,
                                tp.getWidth(), sectionH, tp, ctx);
            cursorY += sectionH;

            // Tab content
            ctx.traverseAt(panel, contentX, cursorY);
            cursorY += panelH;
        }
    }

    /**
     * Draw a titled section divider  -- a filled band with a bold tab title.
     * Colours are derived from the tabbed pane's own foreground/background
     * so the result adapts to whatever L&amp;F is active.
     */
    private void renderSectionHeader(String title, int x, int y, int w,
                                      int sectionH, JTabbedPane tp,
                                      HandlerContext ctx) throws IOException {
        Color bg = tp.getBackground() != null ? tp.getBackground() : Color.LIGHT_GRAY;
        Color headerBg = brighter(bg, 0.85f);
        Color headerFg = tp.getForeground() != null ? tp.getForeground() : Color.BLACK;

        ctx.writer().fillRect(x, y, w, sectionH, headerBg);
        ctx.writer().strokeRect(x, y, w, sectionH, bg.darker(), 0.5f);

        if (title == null || title.isBlank()) return;

        Font font = sectionFont(tp);
        FontMetrics fm    = tp.getFontMetrics(font);
        int         baseY = y + (sectionH - fm.getAscent() - fm.getDescent()) / 2 + fm.getAscent();
        ctx.writer().drawText(title, ctx.fontMapper().resolve(font),
                              font.getSize2D(), headerFg, x + 6, baseY);
    }

    /** Compute the section header height from font metrics + padding. */
    private static int sectionHeaderHeight(JTabbedPane tp) {
        Font font = sectionFont(tp);
        FontMetrics fm = tp.getFontMetrics(font);
        return fm.getAscent() + fm.getDescent() + 2 * SECTION_PAD;
    }

    private static Font sectionFont(JTabbedPane tp) {
        return tp.getFont() != null
                ? tp.getFont().deriveFont(Font.BOLD)
                : new Font(Font.SANS_SERIF, Font.BOLD, 11);
    }

    /** Blend a colour toward white by the given factor (0 = white, 1 = original). */
    private static Color brighter(Color c, float factor) {
        int r = c.getRed()   + Math.round((255 - c.getRed())   * (1 - factor));
        int g = c.getGreen() + Math.round((255 - c.getGreen()) * (1 - factor));
        int b = c.getBlue()  + Math.round((255 - c.getBlue())  * (1 - factor));
        return new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    // -----------------------------------------------------------------------
    // UI_SNAPSHOT: selected tab only
    // -----------------------------------------------------------------------

    private static void renderSelectedTab(JTabbedPane tp, int absX, int absY,
                                           HandlerContext ctx) throws IOException {
        Component selected = tp.getSelectedComponent();
        if (selected != null && selected.isVisible()
                && selected.getWidth() > 0 && selected.getHeight() > 0) {
            ctx.traverseAt(selected,
                           absX + selected.getX(),
                           absY + selected.getY());
        }
    }

    // -----------------------------------------------------------------------
    // Tab bar (shared by both modes)
    // -----------------------------------------------------------------------

    private void renderTabBar(JTabbedPane tp, int absX, int absY,
                               HandlerContext ctx) throws IOException {
        int    selectedIdx = tp.getSelectedIndex();
        Font   font        = tp.getFont() != null ? tp.getFont()
                                                  : new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        Color  fg          = tp.getForeground() != null ? tp.getForeground() : Color.BLACK;
        Color  bg          = tp.getBackground() != null ? tp.getBackground() : Color.LIGHT_GRAY;
        Color  selectedBg  = Color.WHITE;

        for (int i = 0; i < tp.getTabCount(); i++) {
            Rectangle r = tp.getBoundsAt(i);
            if (r == null) continue;

            int tx = absX + r.x;
            int ty = absY + r.y;

            // Tab cell background
            Color tabBg = (i == selectedIdx) ? selectedBg : bg.darker();
            ctx.writer().fillRect(tx, ty, r.width, r.height, tabBg);

            // Tab cell border
            ctx.writer().strokeRect(tx, ty, r.width, r.height, Color.GRAY, 1f);

            // Tab icon and title  -- centred together in the tab rect
            String title   = tp.getTitleAt(i);
            Icon   tabIcon = tp.getIconAt(i);
            boolean hasTitle = title != null && !title.isBlank();

            FontMetrics fm       = tp.getFontMetrics(font);
            int         iconW    = (tabIcon != null) ? tabIcon.getIconWidth()  : 0;
            int         iconH    = (tabIcon != null) ? tabIcon.getIconHeight() : 0;
            int         gap      = (tabIcon != null && hasTitle) ? 4 : 0;
            int         textW    = hasTitle ? fm.stringWidth(title) : 0;
            int         totalW   = iconW + gap + textW;
            int         startX   = tx + (r.width - totalW) / 2;

            // Icon (vertically centred)
            if (tabIcon != null) {
                int iconY = ty + (r.height - iconH) / 2;
                IconPainter.render(tabIcon, tp, startX, iconY, ctx);
            }

            // Text (vertically centred)
            if (hasTitle) {
                int textH  = fm.getAscent() + fm.getDescent();
                int baseY  = ty + (r.height - textH) / 2 + fm.getAscent();
                ctx.writer().drawText(title, ctx.fontMapper().resolve(font),
                                      font.getSize2D(), fg, startX + iconW + gap, baseY);
            }
        }
    }
}
