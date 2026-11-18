package com.pixelclasses.client;

import com.pixelclasses.PixelClassesMod;
import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.network.C2S;
import com.pixelclasses.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only, Forge event bus: key presses and cleanup. */
@Mod.EventBusSubscriber(modid = PixelClassesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientData.tick++;
        ModelProjectiles.clientTick();
        Minecraft mc = Minecraft.getInstance();
        boolean canAct = mc.player != null && mc.screen == null;

        while (KeyBindings.OPEN.consumeClick()) {
            if (canAct) {
                mc.setScreen(new SkillScreen());
            }
        }
        for (int slot = 0; slot < KeyBindings.CAST.length; slot++) {
            while (KeyBindings.CAST[slot].consumeClick()) {
                if (canAct) {
                    tryCast(slot);
                }
            }
        }
    }

    /** Cheap client-side checks for instant feedback; the server checks everything again. */
    private static void tryCast(int slot) {
        String id = ClientData.equipped[slot];
        if (id.isEmpty()) {
            return;
        }
        SkillDef skill = Definitions.CLIENT.skillDef(ResourceLocation.tryParse(id));
        if (skill == null || ClientData.cooldownLeft(id) > 0) {
            return;
        }
        double cost = skill.manaCost() * ClassConfig.get(ClassConfig.MANA_COST_MULT);
        if (ClientData.mana < cost) {
            UiSounds.play("ui_deny", 0.8F, 0.7F);
        }
        ModNetwork.sendToServer(new C2S.Cast(slot));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ModelProjectiles.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.clear();
    }
}
