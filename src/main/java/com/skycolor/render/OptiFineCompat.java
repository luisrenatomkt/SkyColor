package com.skycolor.render;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Le a opcao "Sky" do OptiFine (campo GameSettings.ofSky) por reflexao. Sem OptiFine = sempre ligada. */
public final class OptiFineCompat {
    private static boolean resolved = false;
    private static Field ofSky = null;

    private OptiFineCompat() {}

    public static boolean isSkyOff() {
        try {
            if (!resolved) {
                resolved = true;
                try {
                    ofSky = net.minecraft.client.settings.GameSettings.class.getField("ofSky");
                } catch (NoSuchFieldException e) {
                    ofSky = null; // OptiFine ausente
                }
            }
            return ofSky != null && !ofSky.getBoolean(Minecraft.getMinecraft().gameSettings);
        } catch (Throwable t) {
            return false;
        }
    }
}
