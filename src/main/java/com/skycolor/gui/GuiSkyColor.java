package com.skycolor.gui;

import com.skycolor.SkyColorMod;
import com.skycolor.config.SkyConfig;
import com.skycolor.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSkyColor extends GuiScreen {
    private static final int PW = 360, PH = 216;

    private static final int B_ENABLE = 0, B_MODE = 1, B_TAB_TOP = 2, B_TAB_BOTTOM = 3,
            B_RESET = 4, B_SAVE = 5, B_CLOSE = 6;

    private final SkyConfig cfg = SkyColorMod.config;
    private boolean editingBottom = false;
    private float h, s, v; // HSV da cor em edicao (0..1), guardado a parte para nao perder o matiz em s=0

    private int left, top;
    private final List<ColorSlider> sliders = new ArrayList<ColorSlider>();
    private ColorSlider dragging;
    private ColorSlider sH, sS, sV, sR, sG, sB, sCurve;
    private GuiButton bEnable, bMode, bTop, bBottom;
    private boolean hsvLoaded = false;

    @Override
    public void initGui() {
        left = (width - PW) / 2;
        top = (height - PH) / 2;
        buttonList.clear();
        sliders.clear();

        bEnable = new GuiButton(B_ENABLE, left + 12, top + 22, 160, 20, "");
        bMode = new GuiButton(B_MODE, left + 188, top + 22, 160, 20, "");
        bTop = new GuiButton(B_TAB_TOP, left + 12, top + 48, 78, 20, "");
        bBottom = new GuiButton(B_TAB_BOTTOM, left + 94, top + 48, 78, 20, "");
        buttonList.add(bEnable);
        buttonList.add(bMode);
        buttonList.add(bTop);
        buttonList.add(bBottom);
        buttonList.add(new GuiButton(B_RESET, left + 12, top + 192, 108, 20, "Restaurar original"));
        buttonList.add(new GuiButton(B_SAVE, left + 126, top + 192, 108, 20, "Salvar"));
        buttonList.add(new GuiButton(B_CLOSE, left + 240, top + 192, 108, 20, "Fechar"));

        final int sx = left + 12, sw = 160, sh = 12;
        int y = top + 76;
        ColorSlider.Listener hsvL = new ColorSlider.Listener() {
            public void changed(ColorSlider sl) { onHsvChanged(); }
        };
        ColorSlider.Listener rgbL = new ColorSlider.Listener() {
            public void changed(ColorSlider sl) { onRgbChanged(); }
        };
        sH = add(new ColorSlider(sx, y, sw, sh, "H", 12, 0, 360, "%.0f\u00B0", new DrawUtil.Track() {
            public int at(float t) { return ColorUtil.hsvToRgb(t, 1f, 1f); }
        }, hsvL));
        sS = add(new ColorSlider(sx, y + 16, sw, sh, "S", 12, 0, 100, "%.0f%%", new DrawUtil.Track() {
            public int at(float t) { return ColorUtil.hsvToRgb(h, t, Math.max(v, 0.25f)); }
        }, hsvL));
        sV = add(new ColorSlider(sx, y + 32, sw, sh, "V", 12, 0, 100, "%.0f%%", new DrawUtil.Track() {
            public int at(float t) { return ColorUtil.hsvToRgb(h, s, t); }
        }, hsvL));
        sR = add(new ColorSlider(sx, y + 48, sw, sh, "R", 12, 0, 255, "%.0f", new DrawUtil.Track() {
            public int at(float t) { return (Math.round(t * 255) << 16) | (current() & 0x00FFFF); }
        }, rgbL));
        sG = add(new ColorSlider(sx, y + 64, sw, sh, "G", 12, 0, 255, "%.0f", new DrawUtil.Track() {
            public int at(float t) { return (current() & 0xFF00FF) | (Math.round(t * 255) << 8); }
        }, rgbL));
        sB = add(new ColorSlider(sx, y + 80, sw, sh, "B", 12, 0, 255, "%.0f", new DrawUtil.Track() {
            public int at(float t) { return (current() & 0xFFFF00) | Math.round(t * 255); }
        }, rgbL));
        sCurve = add(new ColorSlider(sx, y + 100, sw, sh, "Curva", 32, SkyConfig.MIN_CURVE, SkyConfig.MAX_CURVE, "%.2f",
                new DrawUtil.Track() {
                    public int at(float t) { return ColorUtil.lerp(0x404040, 0xE0E0E0, t); }
                }, new ColorSlider.Listener() {
            public void changed(ColorSlider sl) { cfg.curve = sl.value; cfg.dirty = true; }
        }));
        sCurve.value = cfg.curve;

        loadFromColor();
        refreshButtons();
    }

    private ColorSlider add(ColorSlider s) {
        sliders.add(s);
        return s;
    }

    // ------------------------------------------------------------ estado

    private int current() {
        return (editingBottom && cfg.gradient) ? cfg.bottomColor : cfg.topColor;
    }

    private void setCurrent(int rgb) {
        if (editingBottom && cfg.gradient) cfg.bottomColor = rgb & 0xFFFFFF;
        else cfg.topColor = rgb & 0xFFFFFF;
        cfg.dirty = true;
    }

    private void loadFromColor() {
        float[] hsv = ColorUtil.rgbToHsv(current());
        h = hsv[0]; s = hsv[1]; v = hsv[2];
        syncHsvSliders();
        syncRgbSliders();
    }

    private void syncHsvSliders() {
        sH.value = h * 360f; sS.value = s * 100f; sV.value = v * 100f;
    }

    private void syncRgbSliders() {
        int c = current();
        sR.value = (c >> 16) & 255; sG.value = (c >> 8) & 255; sB.value = c & 255;
    }

    private void onHsvChanged() {
        h = sH.value / 360f; s = sS.value / 100f; v = sV.value / 100f;
        setCurrent(ColorUtil.hsvToRgb(h, s, v));
        syncRgbSliders();
    }

    private void onRgbChanged() {
        int rgb = (Math.round(sR.value) << 16) | (Math.round(sG.value) << 8) | Math.round(sB.value);
        setCurrent(rgb);
        float[] hsv = ColorUtil.rgbToHsv(rgb);
        if (hsv[1] > 0f && hsv[2] > 0f) h = hsv[0];
        s = hsv[1]; v = hsv[2];
        syncHsvSliders();
    }

    private void refreshButtons() {
        bEnable.displayString = "Modifica\u00E7\u00E3o: " + (cfg.enabled ? "LIGADA" : "DESLIGADA");
        bMode.displayString = "Modo: " + (cfg.gradient ? "Gradiente" : "Cor \u00FAnica");
        if (!cfg.gradient) editingBottom = false;
        bTop.displayString = (cfg.gradient ? "Cor superior" : "Cor");
        bBottom.displayString = "Cor inferior";
        bBottom.enabled = cfg.gradient;
        bTop.enabled = editingBottom && cfg.gradient;   // aba ativa fica "apagada"
        bBottom.enabled = cfg.gradient && !editingBottom;
        sCurve.value = cfg.curve;
    }

    // ------------------------------------------------------------ eventos

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case B_ENABLE:
                cfg.enabled = !cfg.enabled; cfg.dirty = true; break;
            case B_MODE:
                cfg.gradient = !cfg.gradient;
                if (!cfg.gradient) editingBottom = false;
                cfg.dirty = true;
                loadFromColor();
                break;
            case B_TAB_TOP:
                editingBottom = false; loadFromColor(); break;
            case B_TAB_BOTTOM:
                if (cfg.gradient) { editingBottom = true; loadFromColor(); }
                break;
            case B_RESET:
                cfg.resetToDefaults(); editingBottom = false; loadFromColor(); break;
            case B_SAVE:
                cfg.save(); break;
            case B_CLOSE:
                mc.displayGuiScreen(null); return;
            default:
                break;
        }
        refreshButtons();
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button == 0) {
            for (ColorSlider sl : sliders) {
                if (sl.hit(mx, my)) {
                    dragging = sl;
                    sl.dragTo(mx);
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        super.mouseClickMove(mx, my, button, time);
        if (dragging != null) dragging.dragTo(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        dragging = null;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (SkyColorMod.keyOpen != null && key == SkyColorMod.keyOpen.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(c, key); // ESC fecha
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // o mundo continua visivel atras do painel = pre-visualizacao ao vivo
    }

    // ------------------------------------------------------------ desenho

    @Override
    public void drawScreen(int mx, int my, float pt) {
        // sem fundo escurecido: o ceu real aparece ao redor do painel
        drawRect(left - 1, top - 1, left + PW + 1, top + PH + 1, 0xFF3A3A4A);
        drawRect(left, top, left + PW, top + PH, 0xE0141420);
        drawCenteredString(fontRendererObj, "Sky Color", left + PW / 2, top + 7, 0xFFFFFF);
        if (cfg.dirty) {
            fontRendererObj.drawStringWithShadow("\u25CF n\u00E3o salvo", left + PW - 62, top + 7, 0xFFCC55);
        }

        // pre-visualizacao (mesma funcao de gradiente do renderizador)
        int px = left + 188, py = top + 64, pw = 160, ph = 100;
        fontRendererObj.drawStringWithShadow("Pr\u00E9-visualiza\u00E7\u00E3o", px, top + 52, 0xDDDDDD);
        DrawUtil.border(px, py, pw, ph, 0xFF000000);
        for (int i = 0; i < ph; i += 2) {
            float f = (i + 1) / (float) ph; // 0 = zenite, 1 = horizonte
            float t = cfg.gradient ? ColorUtil.gradientT(90.0 * (1.0 - f), cfg.curve) : 0f;
            int c = ColorUtil.lerp(cfg.topColor, cfg.bottomColor, t);
            drawRect(px, py + i, px + pw, py + i + 2, 0xFF000000 | c);
        }
        fontRendererObj.drawStringWithShadow((cfg.gradient ? "Superior " : "Cor ") + SkyConfig.hex(cfg.topColor), px, top + 169, 0xDDDDDD);
        if (cfg.gradient) {
            fontRendererObj.drawStringWithShadow("Inferior " + SkyConfig.hex(cfg.bottomColor), px + 82, top + 169, 0xDDDDDD);
        }
        fontRendererObj.drawStringWithShadow("(escurece \u00E0 noite/chuva como o vanilla)", px, top + 180, 0x888888);

        for (ColorSlider sl : sliders) sl.draw(fontRendererObj);
        super.drawScreen(mx, my, pt); // botoes
    }
}
