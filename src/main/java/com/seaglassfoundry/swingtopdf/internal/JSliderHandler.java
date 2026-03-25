package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Renders a {@link JSlider} as PDF vector graphics.
 *
 * <p>Draws:
 * <ol>
 *   <li>Background fill</li>
 *   <li>Track bar with filled portion up to the current value</li>
 *   <li>Thumb rectangle at the value position</li>
 *   <li>Major tick marks when {@link JSlider#getPaintTicks()} is {@code true}</li>
 *   <li>Tick labels when {@link JSlider#getPaintLabels()} is {@code true}</li>
 * </ol>
 * Both horizontal and vertical orientations are supported.
 */
final class JSliderHandler implements ComponentHandler {

    static final JSliderHandler INSTANCE = new JSliderHandler();

    private JSliderHandler() {}

    private static final int TRACK_THICKNESS       = 4;
    private static final int THUMB_SIZE            = 12;
    private static final int TICK_MAJOR_H          = 6;
    private static final int TICK_MINOR_H          = 3;
    private static final int DEFAULT_LABEL_HEIGHT  = 12;

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JSlider slider = (JSlider) comp;
        int w = slider.getWidth();
        int h = slider.getHeight();

        // Background
        if (slider.isOpaque() && slider.getBackground() != null) {
            ctx.writer().fillRect(absX, absY, w, h, slider.getBackground());
        }

        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            renderHorizontal(slider, absX, absY, w, h, ctx);
        } else {
            renderVertical(slider, absX, absY, w, h, ctx);
        }
    }

    // -----------------------------------------------------------------------
    // Horizontal
    // -----------------------------------------------------------------------

    private void renderHorizontal(JSlider slider, int absX, int absY, int w, int h,
                                   HandlerContext ctx) throws IOException {
        int range   = slider.getMaximum() - slider.getMinimum();
        float frac  = range > 0
                ? Math.max(0f, Math.min(1f, (float)(slider.getValue() - slider.getMinimum()) / range))
                : 0f;

        // Track area  -- leave insets so the thumb doesn't clip the edge
        int inset   = THUMB_SIZE / 2;
        int trackX  = absX + inset;
        int trackW  = w - 2 * inset;
        int trackY  = absY + h / 2 - TRACK_THICKNESS / 2;

        // Labels live below the track when paintLabels=true; reserve space
        if (slider.getPaintLabels()) trackY -= labelAreaHeight(slider) / 2;

        // Empty track
        ctx.writer().fillRect(trackX, trackY, trackW, TRACK_THICKNESS, Color.LIGHT_GRAY);

        // Filled portion
        int fillW = Math.round(trackW * frac);
        Color fillColor = sliderFillColor(slider);
        if (fillW > 0) ctx.writer().fillRect(trackX, trackY, fillW, TRACK_THICKNESS, fillColor);

        // Track border
        ctx.writer().strokeRect(trackX, trackY, trackW, TRACK_THICKNESS, Color.GRAY, 0.5f);

        // Thumb
        int thumbX = trackX + Math.round(trackW * frac) - THUMB_SIZE / 2;
        ctx.writer().fillRect(thumbX, trackY - (THUMB_SIZE - TRACK_THICKNESS) / 2,
                              THUMB_SIZE, THUMB_SIZE, Color.WHITE);
        ctx.writer().strokeRect(thumbX, trackY - (THUMB_SIZE - TRACK_THICKNESS) / 2,
                                THUMB_SIZE, THUMB_SIZE, Color.DARK_GRAY, 1f);

        // Tick marks
        if (slider.getPaintTicks()) {
            drawHorizontalTicks(slider, absX, absY, trackX, trackW, trackY, h, ctx);
        }

        // Labels
        if (slider.getPaintLabels()) {
            drawHorizontalLabels(slider, absX, absY, trackX, trackW, trackY, h, ctx);
        }
    }

    private void drawHorizontalTicks(JSlider slider, int absX, int absY,
                                      int trackX, int trackW, int trackY, int h,
                                      HandlerContext ctx) throws IOException {
        int range = slider.getMaximum() - slider.getMinimum();
        if (range <= 0) return;
        int major = slider.getMajorTickSpacing();
        int minor = slider.getMinorTickSpacing();
        int tickBase = trackY + TRACK_THICKNESS + 1;

        if (minor > 0) {
            for (int v = slider.getMinimum(); v <= slider.getMaximum(); v += minor) {
                int x = trackX + Math.round(trackW * (float)(v - slider.getMinimum()) / range);
                ctx.writer().drawLine(x, tickBase, x, tickBase + TICK_MINOR_H, Color.GRAY, 0.5f);
            }
        }
        if (major > 0) {
            for (int v = slider.getMinimum(); v <= slider.getMaximum(); v += major) {
                int x = trackX + Math.round(trackW * (float)(v - slider.getMinimum()) / range);
                ctx.writer().drawLine(x, tickBase, x, tickBase + TICK_MAJOR_H, Color.DARK_GRAY, 1f);
            }
        }
    }

    private void drawHorizontalLabels(JSlider slider, int absX, int absY,
                                       int trackX, int trackW, int trackY, int h,
                                       HandlerContext ctx) throws IOException {
        Dictionary<?, ?> labels = slider.getLabelTable();
        if (labels == null) return;
        int range  = slider.getMaximum() - slider.getMinimum();
        if (range <= 0) return;

        Font font = slider.getFont() != null ? slider.getFont()
                                             : new Font(Font.SANS_SERIF, Font.PLAIN, 9);
        Color fg  = slider.getForeground() != null ? slider.getForeground() : Color.BLACK;

        int labelY = trackY + TRACK_THICKNESS + TICK_MAJOR_H + 2;
        FontMetrics fm = slider.getFontMetrics(font);

        Enumeration<?> keys = labels.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (!(key instanceof Integer v)) continue;
            Object val = labels.get(key);
            String text = val instanceof Component c ? (c instanceof javax.swing.JLabel l
                    ? l.getText() : val.toString()) : val.toString();
            if (text == null || text.isBlank()) continue;

            int x = trackX + Math.round(trackW * (float)(v - slider.getMinimum()) / range);
            int textX = x - fm.stringWidth(text) / 2;
            ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                                  font.getSize2D(), fg, textX, labelY + fm.getAscent());
        }
    }

    // -----------------------------------------------------------------------
    // Vertical
    // -----------------------------------------------------------------------

    private void renderVertical(JSlider slider, int absX, int absY, int w, int h,
                                 HandlerContext ctx) throws IOException {
        int range  = slider.getMaximum() - slider.getMinimum();
        float frac = range > 0
                ? Math.max(0f, Math.min(1f, (float)(slider.getValue() - slider.getMinimum()) / range))
                : 0f;

        int inset  = THUMB_SIZE / 2;
        int trackH = h - 2 * inset;
        int trackX = absX + w / 2 - TRACK_THICKNESS / 2;
        int trackY = absY + inset;

        // Empty track
        ctx.writer().fillRect(trackX, trackY, TRACK_THICKNESS, trackH, Color.LIGHT_GRAY);

        // Filled portion (bottom -&gt; value)
        int fillH = Math.round(trackH * frac);
        Color fillColor = sliderFillColor(slider);
        if (fillH > 0) {
            ctx.writer().fillRect(trackX, trackY + trackH - fillH,
                                  TRACK_THICKNESS, fillH, fillColor);
        }

        ctx.writer().strokeRect(trackX, trackY, TRACK_THICKNESS, trackH, Color.GRAY, 0.5f);

        // Thumb
        int thumbY = trackY + trackH - Math.round(trackH * frac) - THUMB_SIZE / 2;
        ctx.writer().fillRect(trackX - (THUMB_SIZE - TRACK_THICKNESS) / 2, thumbY,
                              THUMB_SIZE, THUMB_SIZE, Color.WHITE);
        ctx.writer().strokeRect(trackX - (THUMB_SIZE - TRACK_THICKNESS) / 2, thumbY,
                                THUMB_SIZE, THUMB_SIZE, Color.DARK_GRAY, 1f);
    }

    // -----------------------------------------------------------------------

    private static Color sliderFillColor(JSlider slider) {
        if (slider.getForeground() != null) return slider.getForeground();
        Color c = UIManager.getColor("Slider.focus");
        return c != null ? c : new Color(70, 130, 180);
    }

    private static int labelAreaHeight(JSlider slider) {
        Font font = slider.getFont();
        if (font == null) return DEFAULT_LABEL_HEIGHT;
        return slider.getFontMetrics(font).getHeight() + 2;
    }
}
