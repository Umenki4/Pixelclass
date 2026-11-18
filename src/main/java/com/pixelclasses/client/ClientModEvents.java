package com.pixelclasses.client;

import com.pixelclasses.PixelClassesMod;
import com.pixelclasses.client.fx.FxParticle;
import com.pixelclasses.client.fx.FxProvider;
import com.pixelclasses.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only, mod event bus. */
@Mod.EventBusSubscriber(modid = PixelClassesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN);
        for (var key : KeyBindings.CAST) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLASH.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.SLASH));
        event.registerSpriteSet(ModParticles.SWIRL.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.SWIRL));
        event.registerSpriteSet(ModParticles.SPARKLE.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.SPARKLE));
        event.registerSpriteSet(ModParticles.SHARD.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.SHARD));
        event.registerSpriteSet(ModParticles.BURST.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.BURST));
        event.registerSpriteSet(ModParticles.GLOW.get(), sprites -> new FxProvider(sprites, FxParticle.Kind.GLOW));
    }

    @SubscribeEvent
    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelProjectiles.DAGGER_MODEL);   // so the flying dagger model gets baked
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_bar", new HudOverlay());
    }
}
