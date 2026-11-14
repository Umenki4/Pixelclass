package com.pixelclasses.events;

import com.pixelclasses.PixelClassesMod;
import com.pixelclasses.command.ClassCommand;
import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.DefinitionManager;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.player.ClassApi;
import com.pixelclasses.player.ClassData;
import com.pixelclasses.player.ClassDataProvider;
import com.pixelclasses.player.ClassEffects;
import com.pixelclasses.skill.SkillRuntime;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Server-side glue (registered automatically on the Forge event bus). */
@Mod.EventBusSubscriber(modid = PixelClassesMod.MODID)
public final class ClassEvents {
    private static final ResourceLocation CAP_ID = new ResourceLocation(PixelClassesMod.MODID, "data");
    private static final DefinitionManager DEFINITIONS = new DefinitionManager();

    private ClassEvents() {}

    // ------------------------------------------------------------------ data loading

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(DEFINITIONS);
    }

    /** Fired on login and after /reload: send definitions and re-apply class stats. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            syncAll(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                syncAll(player);
            }
        }
    }

    private static void syncAll(ServerPlayer player) {
        ModNetwork.sendDefs(player);
        ClassEffects.apply(player);
        ModNetwork.sendPlayer(player);
    }

    // ------------------------------------------------------------------ player data

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            ClassDataProvider provider = new ClassDataProvider();
            event.addCapability(CAP_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            ClassApi.get(event.getEntity()).copyFrom(ClassApi.get(original));
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        syncAll(player);
        if (ClassEffects.current(player) == null && ClassConfig.get(ClassConfig.OPEN_SELECTION_ON_JOIN)) {
            UUID id = player.getUUID();
            MinecraftServer server = player.server;
            SkillRuntime.later(id, 50, () -> {          // wait ~2.5 s so the world is loaded on the client
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null && ClassEffects.current(p) == null) {
                    ModNetwork.openScreen(p, 0);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SkillRuntime.cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffects.apply(player);
            ClassDef cls = ClassEffects.current(player);
            if (cls != null) {
                ClassApi.get(player).setMana((float) cls.maxMana());
            }
            if (!event.isEndConquered()) {
                player.setHealth(player.getMaxHealth());
            }
            ModNetwork.sendPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendPlayer(player);
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isServer() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ClassDef cls = ClassEffects.current(player);
        if (cls == null) {
            return;
        }
        ClassData data = ClassApi.get(player);
        float max = (float) cls.maxMana();
        if (data.getMana() < max) {
            double perTick = cls.manaRegen() * ClassConfig.get(ClassConfig.MANA_REGEN_MULT) / 20.0;
            data.setMana(Math.min(max, data.getMana() + (float) perTick));
        }
        if (player.tickCount % 5 == 0 && Math.abs(data.getMana() - data.lastSentMana) > 0.05F) {
            ModNetwork.sendMana(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SkillRuntime.tick(server);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SkillRuntime.clear();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ClassCommand.register(event.getDispatcher());
    }
}
