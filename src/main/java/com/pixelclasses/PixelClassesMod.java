package com.pixelclasses;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.player.ClassData;
import com.pixelclasses.registry.ModItems;
import com.pixelclasses.registry.ModSounds;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Pixel Classes - data-driven RPG classes with equippable skills. */
@Mod(PixelClassesMod.MODID)
public class PixelClassesMod {
    public static final String MODID = "pixelclasses";

    public PixelClassesMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::buildCreativeTabs);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ClassConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SHADOW_DAGGER);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ClassData.class);
    }
}
