package com.pixelclasses.network;

import com.pixelclasses.player.ClassActions;
import com.pixelclasses.skill.SkillCaster;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server requests. The server re-checks everything, the client is never trusted. */
public final class C2S {
    private C2S() {}

    /** "I pressed the key of slot N." */
    public static final class Cast {
        private final int slot;

        public Cast(int slot) {
            this.slot = slot;
        }

        public static void encode(Cast m, FriendlyByteBuf b) {
            b.writeVarInt(m.slot);
        }

        public static Cast decode(FriendlyByteBuf b) {
            return new Cast(b.readVarInt());
        }

        public static void handle(Cast m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    SkillCaster.castSlot(player, m.slot);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** "I choose this class." */
    public static final class SelectClass {
        private final String classId;

        public SelectClass(String classId) {
            this.classId = classId;
        }

        public static void encode(SelectClass m, FriendlyByteBuf b) {
            b.writeUtf(m.classId, 256);
        }

        public static SelectClass decode(FriendlyByteBuf b) {
            return new SelectClass(b.readUtf(256));
        }

        public static void handle(SelectClass m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ClassActions.requestClass(player, m.classId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** "Put this skill in slot N" (empty id = clear the slot). */
    public static final class Equip {
        private final int slot;
        private final String skillId;

        public Equip(int slot, String skillId) {
            this.slot = slot;
            this.skillId = skillId;
        }

        public static void encode(Equip m, FriendlyByteBuf b) {
            b.writeVarInt(m.slot);
            b.writeUtf(m.skillId, 256);
        }

        public static Equip decode(FriendlyByteBuf b) {
            return new Equip(b.readVarInt(), b.readUtf(256));
        }

        public static void handle(Equip m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ClassActions.equip(player, m.slot, m.skillId);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
