package com.skycolor.render;

import com.skycolor.SkyColorMod;
import com.skycolor.config.SkyConfig;
import com.skycolor.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Substitui SOMENTE a chamada vanilla RenderGlobal.renderSky (via o hook oficial do Forge,
 * WorldProvider#setSkyRenderer). Sol, lua, estrelas, brilho do por-do-sol e o plano escuro
 * abaixo do horizonte sao reproduzidos exatamente como no vanilla 1.8.9; so a cor do plano
 * do ceu (y = +16) e calculada por nos. A neblina nao e tocada: World#getSkyColor nao e alterado.
 */
public class SkyRenderer extends IRenderHandler {
    private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");

    // aneis do disco do ceu (raio horizontal em blocos; o plano fica em y = 16)
    private static final float[] RADII = {0f, 6f, 11f, 18f, 26f, 36f, 52f, 75f, 114f, 183f, 305f, 420f};
    private static final int SEGMENTS = 32;
    private static final float PLANE_Y = 16f;

    private int starList = -1;

    @Override
    public void render(float pt, WorldClient world, Minecraft mc) {
        SkyConfig cfg = SkyColorMod.config;
        boolean anaglyph = mc.gameSettings.anaglyph;

        float[] top = shade(cfg.topColor, world, pt);
        float[] bottom = cfg.gradient ? shade(cfg.bottomColor, world, pt) : top;
        if (anaglyph) { top = anaglyph(top); bottom = anaglyph(bottom); }

        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.disableFog(); // cores exatas; a neblina continua vanilla para o resto

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // ---- 1. plano do ceu (unica parte realmente modificada) ----
        drawSkyDisc(wr, tess, top, bottom, cfg.curve);

        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.disableStandardItemLighting();

        // ---- 2. brilho do nascer/por do sol (vanilla) ----
        float[] glow = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(pt), pt);
        if (glow != null) {
            GlStateManager.disableTexture2D();
            GlStateManager.shadeModel(7425);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(MathHelper.sin(world.getCelestialAngleRadians(pt)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            float gr = glow[0], gg = glow[1], gb = glow[2];
            if (anaglyph) {
                float[] a = anaglyph(new float[]{gr, gg, gb});
                gr = a[0]; gg = a[1]; gb = a[2];
            }
            wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(0.0D, 100.0D, 0.0D).color(gr, gg, gb, glow[3]).endVertex();
            for (int i = 0; i <= 16; ++i) {
                float a = (float) i * (float) Math.PI * 2.0F / 16.0F;
                float s = MathHelper.sin(a);
                float c = MathHelper.cos(a);
                wr.pos((double) (s * 120.0F), (double) (c * 120.0F), (double) (-c * 40.0F * glow[3]))
                        .color(glow[0], glow[1], glow[2], 0.0F).endVertex();
            }
            tess.draw();
            GlStateManager.popMatrix();
            GlStateManager.shadeModel(7424);
        }

        // ---- 3. sol, lua, estrelas (vanilla) ----
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        GlStateManager.pushMatrix();
        float rainDim = 1.0F - world.getRainStrength(pt);
        GlStateManager.color(1.0F, 1.0F, 1.0F, rainDim);
        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(world.getCelestialAngle(pt) * 360.0F, 1.0F, 0.0F, 0.0F);

        float size = 30.0F;
        mc.getTextureManager().bindTexture(SUN);
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos((double) (-size), 100.0D, (double) (-size)).tex(0.0D, 0.0D).endVertex();
        wr.pos((double) size, 100.0D, (double) (-size)).tex(1.0D, 0.0D).endVertex();
        wr.pos((double) size, 100.0D, (double) size).tex(1.0D, 1.0D).endVertex();
        wr.pos((double) (-size), 100.0D, (double) size).tex(0.0D, 1.0D).endVertex();
        tess.draw();

        size = 20.0F;
        mc.getTextureManager().bindTexture(MOON);
        int phase = world.getMoonPhase();
        int px = phase % 4;
        int py = phase / 4 % 2;
        float u0 = (float) (px) / 4.0F;
        float v0 = (float) (py) / 2.0F;
        float u1 = (float) (px + 1) / 4.0F;
        float v1 = (float) (py + 1) / 2.0F;
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos((double) (-size), -100.0D, (double) size).tex((double) u1, (double) v1).endVertex();
        wr.pos((double) size, -100.0D, (double) size).tex((double) u0, (double) v1).endVertex();
        wr.pos((double) size, -100.0D, (double) (-size)).tex((double) u0, (double) v0).endVertex();
        wr.pos((double) (-size), -100.0D, (double) (-size)).tex((double) u1, (double) v0).endVertex();
        tess.draw();

        GlStateManager.disableTexture2D();
        float starBrightness = world.getStarBrightness(pt) * rainDim;
        if (starBrightness > 0.0F) {
            GlStateManager.color(starBrightness, starBrightness, starBrightness, starBrightness);
            ensureStars(wr, tess);
            GlStateManager.callList(starList);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
        GlStateManager.popMatrix();

        // ---- 4. plano escuro abaixo do horizonte (vanilla; usa a neblina vanilla) ----
        GlStateManager.disableTexture2D();
        GlStateManager.color(0.0F, 0.0F, 0.0F);
        double d0 = mc.thePlayer.getPositionEyes(pt).yCoord - world.getHorizon();
        if (d0 < 0.0D) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 12.0F, 0.0F);
            drawUnderPlane(wr, tess);
            GlStateManager.popMatrix();

            float lo = -((float) (d0 + 65.0D));
            float hi = -1.0F;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            quad(wr, -1, lo, 1, 1, lo, 1, 1, hi, 1, -1, hi, 1);
            quad(wr, -1, hi, -1, 1, hi, -1, 1, lo, -1, -1, lo, -1);
            quad(wr, -1, hi, 1, -1, hi, -1, -1, lo, -1, -1, lo, 1);
            quad(wr, 1, lo, 1, 1, lo, -1, 1, hi, -1, 1, hi, 1);
            tess.draw();
        }
        // cor do plano escuro: mesma formula do vanilla, aplicada a cor inferior escolhida
        GlStateManager.color(bottom[0] * 0.2F + 0.04F, bottom[1] * 0.2F + 0.04F, bottom[2] * 0.6F + 0.1F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -((float) (d0 - 16.0D)), 0.0F);
        drawUnderPlane(wr, tess);
        GlStateManager.popMatrix();

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        if (cull) GlStateManager.enableCull();
    }

    // ------------------------------------------------------------------ geometria

    /** Disco horizontal em y=16 com cor por vertice: cor superior no zenite, inferior no horizonte. */
    private void drawSkyDisc(WorldRenderer wr, Tessellator tess, float[] top, float[] bottom, float curve) {
        int rings = RADII.length;
        float[][] col = new float[rings][3];
        for (int i = 0; i < rings; i++) {
            double elev = (i == rings - 1) ? 0.0 : Math.toDegrees(Math.atan2(PLANE_Y, RADII[i]));
            float t = ColorUtil.gradientT(elev, curve);
            for (int k = 0; k < 3; k++) col[i][k] = top[k] + (bottom[k] - top[k]) * t;
        }
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < rings - 1; i++) {
            float ri = RADII[i], ro = RADII[i + 1];
            float[] ci = col[i], co = col[i + 1];
            for (int j = 0; j < SEGMENTS; j++) {
                double a0 = j * Math.PI * 2.0 / SEGMENTS, a1 = (j + 1) * Math.PI * 2.0 / SEGMENTS;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                wr.pos(ri * c0, PLANE_Y, ri * s0).color(ci[0], ci[1], ci[2], 1f).endVertex();
                wr.pos(ro * c0, PLANE_Y, ro * s0).color(co[0], co[1], co[2], 1f).endVertex();
                wr.pos(ro * c1, PLANE_Y, ro * s1).color(co[0], co[1], co[2], 1f).endVertex();
                wr.pos(ri * c1, PLANE_Y, ri * s1).color(ci[0], ci[1], ci[2], 1f).endVertex();
            }
        }
        tess.draw();
    }

    private void drawUnderPlane(WorldRenderer wr, Tessellator tess) {
        float e = 420f, y = -16f;
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos(-e, y, -e).endVertex();
        wr.pos(-e, y, e).endVertex();
        wr.pos(e, y, e).endVertex();
        wr.pos(e, y, -e).endVertex();
        tess.draw();
    }

    private static void quad(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        wr.pos(x1, y1, z1).color(0, 0, 0, 255).endVertex();
        wr.pos(x2, y2, z2).color(0, 0, 0, 255).endVertex();
        wr.pos(x3, y3, z3).color(0, 0, 0, 255).endVertex();
        wr.pos(x4, y4, z4).color(0, 0, 0, 255).endVertex();
    }

    /** Estrelas: mesma geracao do vanilla (Random(10842), 1500 tentativas), em display list propria. */
    private void ensureStars(WorldRenderer wr, Tessellator tess) {
        if (starList != -1) return;
        starList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(starList, GL11.GL_COMPILE);
        Random random = new Random(10842L);
        wr.begin(7, DefaultVertexFormats.POSITION);
        for (int i = 0; i < 1500; ++i) {
            double d0 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d1 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d2 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d3 = (double) (0.15F + random.nextFloat() * 0.1F);
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;
            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 1.0D / Math.sqrt(d4);
                d0 *= d4; d1 *= d4; d2 *= d4;
                double d5 = d0 * 100.0D, d6 = d1 * 100.0D, d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8), d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11), d13 = Math.cos(d11);
                double d14 = random.nextDouble() * Math.PI * 2.0D;
                double d15 = Math.sin(d14), d16 = Math.cos(d14);
                for (int j = 0; j < 4; ++j) {
                    double d18 = (double) ((j & 2) - 1) * d3;
                    double d19 = (double) ((j + 1 & 2) - 1) * d3;
                    double d21 = d18 * d16 - d19 * d15;
                    double d22 = d19 * d16 + d18 * d15;
                    double d23 = d21 * d12;
                    double d24 = -d21 * d13;
                    double d25 = d24 * d9 - d22 * d10;
                    double d26 = d22 * d9 + d24 * d10;
                    wr.pos(d5 + d25, d6 + d23, d7 + d26).endVertex();
                }
            }
        }
        tess.draw();
        GL11.glEndList();
    }

    // ------------------------------------------------------------------ cor

    /**
     * Aplica a cor escolhida o mesmo escurecimento que o vanilla aplica ao ceu:
     * ciclo dia/noite, chuva e tempestade. Assim a cor escolhida e a cor "de dia limpo".
     */
    private static float[] shade(int rgb, WorldClient world, float pt) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float day = MathHelper.clamp_float(MathHelper.cos(world.getCelestialAngle(pt) * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0F, 1.0F);
        r *= day; g *= day; b *= day;

        float rain = world.getRainStrength(pt);
        if (rain > 0.0F) {
            float l = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.6F;
            float k = 1.0F - rain * 0.75F;
            r = r * k + l * (1.0F - k); g = g * k + l * (1.0F - k); b = b * k + l * (1.0F - k);
        }
        float thunder = world.getThunderStrength(pt);
        if (thunder > 0.0F) {
            float l = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.2F;
            float k = 1.0F - thunder * 0.75F;
            r = r * k + l * (1.0F - k); g = g * k + l * (1.0F - k); b = b * k + l * (1.0F - k);
        }
        return new float[]{r, g, b};
    }

    private static float[] anaglyph(float[] c) {
        return new float[]{(c[0] * 30F + c[1] * 59F + c[2] * 11F) / 100F, (c[0] * 30F + c[1] * 70F) / 100F, (c[0] * 30F + c[2] * 70F) / 100F};
    }
}
