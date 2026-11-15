package com.skycolor;

import com.skycolor.config.SkyConfig;
import com.skycolor.gui.GuiSkyColor;
import com.skycolor.render.OptiFineCompat;
import com.skycolor.render.SkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraft.block.material.Material;
import net.minecraftforge.client.EntityViewRenderEvent;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientEvents {
    private final SkyRenderer renderer = new SkyRenderer();
    private boolean skyDrawn = false;
    private boolean fallbackDone = false;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            skyDrawn = false;
            fallbackDone = false;
        }
    }

    /**
     * OptiFine com "Sky: OFF": o vanilla/OptiFine nao desenha o ceu, entao o IRenderHandler pode nao
     * ser chamado. Este evento dispara em setupFog(-1), imediatamente antes de renderSky, com as mesmas
     * matrizes. Se esse evento nao ocorrer, usamos o primeiro setupFog(0) do frame (antes do terreno).
     */
    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        SkyConfig cfg = SkyColorMod.config;
        if (!cfg.enabled || !OptiFineCompat.isSkyOff()) return;
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.theWorld;
        if (world == null || mc.gameSettings.renderDistanceChunks < 4) return;
        WorldProvider provider = world.provider;
        if (provider.getDimensionId() != 0 || !provider.isSurfaceWorld()) return;
        IRenderHandler other = provider.getSkyRenderer();
        if (other != null && other != renderer) return;

        float pt = (float) event.renderPartialTicks;
        if (event.fogMode == -1) {
            if (!skyDrawn) {
                skyDrawn = true;
                renderer.renderDiscOnly(mc, world, pt);
            }
        } else if (event.fogMode == 0 && !skyDrawn && !fallbackDone) {
            fallbackDone = true;
            Material m = event.block.getMaterial();
            if (m != Material.water && m != Material.lava) {
                renderer.renderDiscOnly(mc, world, pt);
            }
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (SkyColorMod.keyOpen.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new GuiSkyColor());
        }
    }

    /**
     * Liga/desliga o nosso IRenderHandler no WorldProvider do Overworld.
     * Quando o mod esta desativado o handler e removido e o Minecraft volta
     * a desenhar o ceu 100% vanilla (nenhum codigo nosso roda).
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        WorldProvider provider = world.provider;
        if (provider.getDimensionId() != 0 || !provider.isSurfaceWorld()) return;

        SkyConfig cfg = SkyColorMod.config;
        IRenderHandler current = provider.getSkyRenderer();
        if (cfg.enabled) {
            if (current == null) provider.setSkyRenderer(renderer); // nao sobrescreve outros mods
        } else if (current == renderer) {
            provider.setSkyRenderer(null);
        }
    }
}
