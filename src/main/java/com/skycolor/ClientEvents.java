package com.skycolor;

import com.skycolor.config.SkyConfig;
import com.skycolor.gui.GuiSkyColor;
import com.skycolor.render.SkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientEvents {
    private final SkyRenderer renderer = new SkyRenderer();

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
