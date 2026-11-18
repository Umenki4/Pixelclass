package com.pixelclasses.vfx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pixelclasses.data.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

/**
 * Describes one particle effect (shape + particle type). The server only sends this small description;
 * the client generates the particles itself (see client/VfxRenderer) so even huge effects cost almost no bandwidth.
 *
 * shapes: point, sphere, ring, disc, line, spiral, column, burst, arc, cone
 */
public final class VfxSpec {
    public final String particle;
    public final int color;
    public final int color2;
    public final float size;
    public final String shape;
    public final float radius;
    public final float height;
    public final float turns;
    public final float yOffset;
    public final float angle;
    public final float roll;
    public final float length;
    public final float speed;
    public final float sx;
    public final float sy;
    public final float sz;
    public final int count;

    private ParticleOptions cached;
    private boolean resolved;

    public VfxSpec(String particle, int color, int color2, float size, String shape, float radius, float height, float turns,
                   float yOffset, float angle, float roll, float length, float speed, float sx, float sy, float sz, int count) {
        this.particle = particle;
        this.color = color;
        this.color2 = color2;
        this.size = size;
        this.shape = shape;
        this.radius = radius;
        this.height = height;
        this.turns = turns;
        this.yOffset = yOffset;
        this.angle = angle;
        this.roll = roll;
        this.length = length;
        this.speed = speed;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.count = count;
    }

    public static VfxSpec parse(JsonObject o) {
        float sx = 0, sy = 0, sz = 0;
        if (o.has("spread")) {
            JsonElement s = o.get("spread");
            if (s.isJsonArray()) {
                JsonArray a = s.getAsJsonArray();
                sx = a.get(0).getAsFloat();
                sy = a.size() > 1 ? a.get(1).getAsFloat() : sx;
                sz = a.size() > 2 ? a.get(2).getAsFloat() : sx;
            } else {
                sx = sy = sz = s.getAsFloat();
            }
        }
        String particle = GsonHelper.getAsString(o, "particle");
        if (particle.equals("dust") || particle.equals("dust_color_transition")) {
            particle = "minecraft:" + particle;
        }
        return new VfxSpec(particle,
                JsonUtil.color(o, "color", 0xFFFFFF), JsonUtil.color(o, "color_to", 0xFFFFFF),
                (float) GsonHelper.getAsDouble(o, "size", 1.0), GsonHelper.getAsString(o, "shape", "point"),
                (float) GsonHelper.getAsDouble(o, "radius", 1.0), (float) GsonHelper.getAsDouble(o, "height", 2.0),
                (float) GsonHelper.getAsDouble(o, "turns", 2.0), (float) GsonHelper.getAsDouble(o, "y_offset", 0.0),
                (float) GsonHelper.getAsDouble(o, "angle", 120.0), (float) GsonHelper.getAsDouble(o, "roll", 0.0), (float) GsonHelper.getAsDouble(o, "length", 8.0),
                (float) GsonHelper.getAsDouble(o, "speed", 0.0), sx, sy, sz,
                Math.max(1, Math.min(400, GsonHelper.getAsInt(o, "count", 10))));
    }

    public static List<VfxSpec> parseList(JsonElement e) {
        List<VfxSpec> list = new ArrayList<>();
        if (e == null || e.isJsonNull()) {
            return list;
        }
        if (e.isJsonArray()) {
            for (JsonElement el : e.getAsJsonArray()) {
                list.add(parse(GsonHelper.convertToJsonObject(el, "vfx")));
            }
        } else {
            list.add(parse(GsonHelper.convertToJsonObject(e, "vfx")));
        }
        return List.copyOf(list);
    }

    public void write(FriendlyByteBuf b) {
        b.writeUtf(particle, 128);
        b.writeInt(color);
        b.writeInt(color2);
        b.writeFloat(size);
        b.writeUtf(shape, 32);
        b.writeFloat(radius);
        b.writeFloat(height);
        b.writeFloat(turns);
        b.writeFloat(yOffset);
        b.writeFloat(angle);
        b.writeFloat(roll);
        b.writeFloat(length);
        b.writeFloat(speed);
        b.writeFloat(sx);
        b.writeFloat(sy);
        b.writeFloat(sz);
        b.writeVarInt(count);
    }

    public static VfxSpec read(FriendlyByteBuf b) {
        return new VfxSpec(b.readUtf(128), b.readInt(), b.readInt(), b.readFloat(), b.readUtf(32),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readFloat(), b.readFloat(), b.readFloat(), Math.max(1, Math.min(400, b.readVarInt())));
    }

    public VfxSpec withLength(float newLength) {
        return new VfxSpec(particle, color, color2, size, shape, radius, height, turns, yOffset, angle, roll, newLength, speed, sx, sy, sz, count);
    }

    /** The particle to spawn, or null if the particle id is unknown (then the effect is silently skipped). */
    public ParticleOptions options() {
        if (!resolved) {
            resolved = true;
            cached = build();
        }
        return cached;
    }

    private ParticleOptions build() {
        ResourceLocation id = ResourceLocation.tryParse(particle);
        if (id == null) {
            return null;
        }
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("dust")) {
            return new DustParticleOptions(toVec(color), size);
        }
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("dust_color_transition")) {
            return new DustColorTransitionOptions(toVec(color), toVec(color2), size);
        }
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(id);
        return type instanceof SimpleParticleType simple ? simple : null;
    }

    private static Vector3f toVec(int rgb) {
        return new Vector3f(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
    }
}
