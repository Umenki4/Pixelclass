package com.pixelclasses.network;

import com.pixelclasses.PixelClassesMod;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.player.ClassApi;
import com.pixelclasses.player.ClassData;
import com.pixelclasses.player.ClassEffects;
import com.pixelclasses.vfx.VfxSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PixelClassesMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, S2C.Defs.class, S2C.Defs::encode, S2C.Defs::decode, S2C.Defs::handle);
        CHANNEL.registerMessage(id++, S2C.Player.class, S2C.Player::encode, S2C.Player::decode, S2C.Player::handle);
        CHANNEL.registerMessage(id++, S2C.Mana.class, S2C.Mana::encode, S2C.Mana::decode, S2C.Mana::handle);
        CHANNEL.registerMessage(id++, S2C.Cooldown.class, S2C.Cooldown::encode, S2C.Cooldown::decode, S2C.Cooldown::handle);
        CHANNEL.registerMessage(id++, S2C.Open.class, S2C.Open::encode, S2C.Open::decode, S2C.Open::handle);
        CHANNEL.registerMessage(id++, S2C.Vfx.class, S2C.Vfx::encode, S2C.Vfx::decode, S2C.Vfx::handle);
        CHANNEL.registerMessage(id++, C2S.Cast.class, C2S.Cast::encode, C2S.Cast::decode, C2S.Cast::handle);
        CHANNEL.registerMessage(id++, C2S.SelectClass.class, C2S.SelectClass::encode, C2S.SelectClass::decode, C2S.SelectClass::handle);
        CHANNEL.registerMessage(id++, C2S.Equip.class, C2S.Equip::encode, C2S.Equip::decode, C2S.Equip::handle);
    }

    /** Sends every class/skill definition to one player. */
    public static void sendDefs(ServerPlayer player) {
        List<S2C.Defs.Entry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, String> e : Definitions.SERVER.rawClasses().entrySet()) {
            if (e.getValue().length() < 32000) {
                entries.add(new S2C.Defs.Entry((byte) 0, e.getKey().toString(), e.getValue()));
            }
        }
        for (Map.Entry<ResourceLocation, String> e : Definitions.SERVER.rawSkills().entrySet()) {
            if (e.getValue().length() < 32000) {
                entries.add(new S2C.Defs.Entry((byte) 1, e.getKey().toString(), e.getValue()));
            }
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2C.Defs(entries));
    }

    /** Sends class, equipped skills and mana of the player to that player. */
    public static void sendPlayer(ServerPlayer player) {
        ClassData data = ClassApi.get(player);
        ClassDef cls = ClassEffects.current(player);
        String classId = cls == null ? "" : cls.id().toString();
        String[] equipped = new String[ClassData.SLOTS];
        Arrays.fill(equipped, "");
        if (cls != null) {
            equipped = data.loadout(classId).clone();
        }
        float max = cls == null ? 0.0F : (float) cls.maxMana();
        data.lastSentMana = data.getMana();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2C.Player(classId, equipped, data.getMana(), max));
    }

    public static void sendMana(ServerPlayer player) {
        ClassData data = ClassApi.get(player);
        ClassDef cls = ClassEffects.current(player);
        data.lastSentMana = data.getMana();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2C.Mana(data.getMana(), cls == null ? 0.0F : (float) cls.maxMana()));
    }

    public static void sendCooldown(ServerPlayer player, String skillId, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2C.Cooldown(skillId, ticks));
    }

    /** kind: 0 = class selection window, 1 = skill window. */
    public static void openScreen(ServerPlayer player, int kind) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2C.Open(kind));
    }

    /** Shows particle effects to everyone within 72 blocks. */
    public static void sendVfx(ServerLevel level, Vec3 pos, Vec3 dir, List<VfxSpec> specs) {
        sendVfx(level, pos, dir, specs, 0);
    }

    /** flightId != 0 lets the client follow ONE flying object (e.g. a 3D dagger) from packet to packet. */
    public static void sendVfx(ServerLevel level, Vec3 pos, Vec3 dir, List<VfxSpec> specs, int flightId) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, 72.0, level.dimension())),
                new S2C.Vfx(pos, dir, specs, flightId));
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
