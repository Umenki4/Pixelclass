package com.pixelclasses.network;

import com.pixelclasses.client.ClientPacketHandler;
import com.pixelclasses.vfx.VfxSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client packets. */
public final class S2C {
    private S2C() {}

    /** All class/skill JSON definitions (so the client UI shows the server's real numbers). */
    public static final class Defs {
        public static final class Entry {
            public final byte kind;   // 0 = class, 1 = skill
            public final String id;
            public final String json;

            public Entry(byte kind, String id, String json) {
                this.kind = kind;
                this.id = id;
                this.json = json;
            }
        }

        private final List<Entry> entries;

        public Defs(List<Entry> entries) {
            this.entries = entries;
        }

        public static void encode(Defs m, FriendlyByteBuf b) {
            b.writeVarInt(m.entries.size());
            for (Entry e : m.entries) {
                b.writeByte(e.kind);
                b.writeUtf(e.id, 256);
                b.writeUtf(e.json, 32767);
            }
        }

        public static Defs decode(FriendlyByteBuf b) {
            int n = Math.max(0, Math.min(b.readVarInt(), 4096));
            List<Entry> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(new Entry(b.readByte(), b.readUtf(256), b.readUtf(32767)));
            }
            return new Defs(list);
        }

        public static void handle(Defs m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onDefs(m.entries)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** The player's class, equipped skills and mana. */
    public static final class Player {
        private final String classId;
        private final String[] equipped;
        private final float mana;
        private final float maxMana;

        public Player(String classId, String[] equipped, float mana, float maxMana) {
            this.classId = classId;
            this.equipped = equipped;
            this.mana = mana;
            this.maxMana = maxMana;
        }

        public static void encode(Player m, FriendlyByteBuf b) {
            b.writeUtf(m.classId, 256);
            b.writeVarInt(m.equipped.length);
            for (String s : m.equipped) {
                b.writeUtf(s, 256);
            }
            b.writeFloat(m.mana);
            b.writeFloat(m.maxMana);
        }

        public static Player decode(FriendlyByteBuf b) {
            String id = b.readUtf(256);
            int n = Math.max(0, Math.min(b.readVarInt(), 16));
            String[] eq = new String[n];
            for (int i = 0; i < n; i++) {
                eq[i] = b.readUtf(256);
            }
            return new Player(id, eq, b.readFloat(), b.readFloat());
        }

        public static void handle(Player m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandler.onPlayer(m.classId, m.equipped, m.mana, m.maxMana)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Just the mana numbers (sent often). */
    public static final class Mana {
        private final float mana;
        private final float maxMana;

        public Mana(float mana, float maxMana) {
            this.mana = mana;
            this.maxMana = maxMana;
        }

        public static void encode(Mana m, FriendlyByteBuf b) {
            b.writeFloat(m.mana);
            b.writeFloat(m.maxMana);
        }

        public static Mana decode(FriendlyByteBuf b) {
            return new Mana(b.readFloat(), b.readFloat());
        }

        public static void handle(Mana m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandler.onMana(m.mana, m.maxMana)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** A skill went on cooldown. */
    public static final class Cooldown {
        private final String skillId;
        private final int ticks;

        public Cooldown(String skillId, int ticks) {
            this.skillId = skillId;
            this.ticks = ticks;
        }

        public static void encode(Cooldown m, FriendlyByteBuf b) {
            b.writeUtf(m.skillId, 256);
            b.writeVarInt(m.ticks);
        }

        public static Cooldown decode(FriendlyByteBuf b) {
            return new Cooldown(b.readUtf(256), b.readVarInt());
        }

        public static void handle(Cooldown m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandler.onCooldown(m.skillId, m.ticks)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Ask the client to open a window: 0 = class selection, 1 = skill window. */
    public static final class Open {
        private final int kind;

        public Open(int kind) {
            this.kind = kind;
        }

        public static void encode(Open m, FriendlyByteBuf b) {
            b.writeVarInt(m.kind);
        }

        public static Open decode(FriendlyByteBuf b) {
            return new Open(b.readVarInt());
        }

        public static void handle(Open m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onOpen(m.kind)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** A particle effect to draw at a position (the client builds the particles itself). */
    public static final class Vfx {
        private final Vec3 pos;
        private final Vec3 dir;
        private final List<VfxSpec> specs;

        public Vfx(Vec3 pos, Vec3 dir, List<VfxSpec> specs) {
            this.pos = pos;
            this.dir = dir;
            this.specs = specs;
        }

        public static void encode(Vfx m, FriendlyByteBuf b) {
            b.writeDouble(m.pos.x);
            b.writeDouble(m.pos.y);
            b.writeDouble(m.pos.z);
            b.writeFloat((float) m.dir.x);
            b.writeFloat((float) m.dir.y);
            b.writeFloat((float) m.dir.z);
            b.writeVarInt(m.specs.size());
            for (VfxSpec s : m.specs) {
                s.write(b);
            }
        }

        public static Vfx decode(FriendlyByteBuf b) {
            Vec3 pos = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
            Vec3 dir = new Vec3(b.readFloat(), b.readFloat(), b.readFloat());
            int n = Math.max(0, Math.min(b.readVarInt(), 32));
            List<VfxSpec> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                list.add(VfxSpec.read(b));
            }
            return new Vfx(pos, dir, list);
        }

        public static void handle(Vfx m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandler.onVfx(m.pos, m.dir, m.specs)));
            ctx.get().setPacketHandled(true);
        }
    }
}
