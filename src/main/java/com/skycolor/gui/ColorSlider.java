package com.skycolor.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.util.Locale;

/** Slider com a propria trilha colorida (usado para H/S/V, R/G/B e curva do gradiente). */
public class ColorSlider {
    public interface Listener {
        void changed(ColorSlider slider);
    }

    private static final int VALUE_W = 34;

    public final int x, y, w, h;
    public final String label, format;
    public final float min, max;
    public final int labelW;
    public float value;
    private final DrawUtil.Track track;
    private final Listener listener;

    public ColorSlider(int x, int y, int w, int h, String label, int labelW, float min, float max,
                       String format, DrawUtil.Track track, Listener listener) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.labelW = labelW;
        this.min = min; this.max = max; this.format = format;
        this.track = track; this.listener = listener;
        this.value = min;
    }

    private int trackX() { return x + labelW; }
    private int trackW() { return w - labelW - VALUE_W; }

    public boolean hit(int mx, int my) {
        return mx >= trackX() - 2 && mx <= trackX() + trackW() + 2 && my >= y && my < y + h;
    }

    public void dragTo(int mx) {
        float t = (mx - trackX()) / (float) trackW();
        t = Math.max(0f, Math.min(1f, t));
        value = min + t * (max - min);
        listener.changed(this);
    }

    public void draw(FontRenderer fr) {
        int tx = trackX(), tw = trackW();
        fr.drawStringWithShadow(label, x, y + (h - 8) / 2f + 0.5f, 0xDDDDDD);
        DrawUtil.border(tx, y, tw, h, 0xFF000000);
        DrawUtil.horizontalGradient(tx, y, tw, h, track, 24);
        float t = (value - min) / (max - min);
        int hx = tx + Math.round(t * tw);
        Gui.drawRect(hx - 2, y - 2, hx + 2, y + h + 2, 0xFF000000);
        Gui.drawRect(hx - 1, y - 1, hx + 1, y + h + 1, 0xFFFFFFFF);
        fr.drawStringWithShadow(String.format(Locale.ROOT, format, value), tx + tw + 5, y + (h - 8) / 2f + 0.5f, 0xDDDDDD);
    }
}
