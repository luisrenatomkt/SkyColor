package com.skycolor;

import com.skycolor.config.SkyConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = SkyColorMod.MODID, name = "Sky Color", version = SkyColorMod.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class SkyColorMod {
    public static final String MODID = "skycolor";
    public static final String VERSION = "1.0.0";

    public static SkyConfig config;
    public static KeyBinding keyOpen;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new SkyConfig();
        config.load(event.getSuggestedConfigurationFile()); // config/skycolor.cfg
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        keyOpen = new KeyBinding("key.skycolor.open", Keyboard.KEY_RSHIFT, "key.categories.skycolor");
        ClientRegistry.registerKeyBinding(keyOpen);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }
}
