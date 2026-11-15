package com.skycolor.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class SkyConfig {
    public static final int DEFAULT_TOP = 0x78A7FF;     // azul do ceu vanilla (aprox.)
    public static final int DEFAULT_BOTTOM = 0xC0D8FF;
    public static final float DEFAULT_CURVE = 1.0f;
    public static final float MIN_CURVE = 0.2f, MAX_CURVE = 4.0f;

    public boolean enabled = false;
    public boolean gradient = true;
    /** Cobre a regiao abaixo do horizonte com a cor inferior (em vez da neblina/vazio do vanilla). */
    public boolean fillBelow = true;
    public int topColor = DEFAULT_TOP;
    public int bottomColor = DEFAULT_BOTTOM;
    public float curve = DEFAULT_CURVE;
    /** Ha alteracoes ainda nao gravadas em disco. */
    public boolean dirty = false;

    private Configuration cfg;

    public void load(File file) {
        cfg = new Configuration(file);
        cfg.load();
        enabled = cfg.getBoolean("enabled", "general", false, "Ativa a modificacao da cor do ceu.");
        gradient = cfg.getBoolean("gradient", "general", true, "true = gradiente (superior/inferior); false = cor unica.");
        fillBelow = cfg.getBoolean("fillBelow", "general", true, "Cobre a area abaixo do horizonte com a cor inferior.");
        topColor = parse(cfg.getString("topColor", "colors", hex(DEFAULT_TOP), "Cor superior (ou cor unica), formato #RRGGBB."), DEFAULT_TOP);
        bottomColor = parse(cfg.getString("bottomColor", "colors", hex(DEFAULT_BOTTOM), "Cor inferior (horizonte), formato #RRGGBB."), DEFAULT_BOTTOM);
        curve = cfg.getFloat("curve", "gradient", DEFAULT_CURVE, MIN_CURVE, MAX_CURVE, "Curva do gradiente. <1 espalha a cor inferior para cima; >1 concentra no horizonte.");
        if (cfg.hasChanged()) cfg.save();
        dirty = false;
    }

    public void save() {
        if (cfg == null) return;
        cfg.get("general", "enabled", false).set(enabled);
        cfg.get("general", "gradient", true).set(gradient);
        cfg.get("general", "fillBelow", true).set(fillBelow);
        cfg.get("colors", "topColor", hex(DEFAULT_TOP)).set(hex(topColor));
        cfg.get("colors", "bottomColor", hex(DEFAULT_BOTTOM)).set(hex(bottomColor));
        cfg.get("gradient", "curve", (double) DEFAULT_CURVE).set((double) curve);
        cfg.save();
        dirty = false;
    }

    /** Volta ao estado original: modificacao desligada e valores padrao. */
    public void resetToDefaults() {
        enabled = false;
        gradient = true;
        fillBelow = true;
        topColor = DEFAULT_TOP;
        bottomColor = DEFAULT_BOTTOM;
        curve = DEFAULT_CURVE;
        dirty = true;
    }

    public static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim().replace("#", ""), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return def;
        }
    }
}
