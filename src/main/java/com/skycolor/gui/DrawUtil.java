package com.skycolor.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public final class DrawUtil {
    private DrawUtil() {}

    /** Cor (0xRRGGBB) em funcao de t = 0..1. */
    public interface Track {
        int at(float t);
    }

    public static void horizontalGradient(int x, int y, int w, int h, Track track, int steps) {
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(7425);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < steps; i++) {
            int c0 = track.at(i / (float) steps), c1 = track.at((i + 1) / (float) steps);
            double x0 = x + w * (double) i / steps, x1 = x + w * (double) (i + 1) / steps;
            wr.pos(x0, y + h, 0).color((c0 >> 16) & 255, (c0 >> 8) & 255, c0 & 255, 255).endVertex();
            wr.pos(x1, y + h, 0).color((c1 >> 16) & 255, (c1 >> 8) & 255, c1 & 255, 255).endVertex();
            wr.pos(x1, y, 0).color((c1 >> 16) & 255, (c1 >> 8) & 255, c1 & 255, 255).endVertex();
            wr.pos(x0, y, 0).color((c0 >> 16) & 255, (c0 >> 8) & 255, c0 & 255, 255).endVertex();
        }
        tess.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableTexture2D();
    }

    public static void border(int x, int y, int w, int h, int argb) {
        Gui.drawRect(x - 1, y - 1, x + w + 1, y, argb);
        Gui.drawRect(x - 1, y + h, x + w + 1, y + h + 1, argb);
        Gui.drawRect(x - 1, y, x, y + h, argb);
        Gui.drawRect(x + w, y, x + w + 1, y + h, argb);
    }
}
