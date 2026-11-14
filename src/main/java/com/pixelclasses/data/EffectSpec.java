package com.pixelclasses.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

/** {"effect": "minecraft:slowness", "duration": 4.0 (seconds), "amplifier": 1, "particles": false} */
public record EffectSpec(ResourceLocation effect, int ticks, int amplifier, boolean particles) {

    public static EffectSpec parse(JsonObject o) {
        ResourceLocation id = JsonUtil.id(GsonHelper.getAsString(o, "effect"), "effect");
        int ticks = (int) Math.round(GsonHelper.getAsDouble(o, "duration", 5.0) * 20.0);
        return new EffectSpec(id, Math.max(1, ticks), Math.max(0, GsonHelper.getAsInt(o, "amplifier", 0)),
                GsonHelper.getAsBoolean(o, "particles", false));
    }

    /** Returns null if the effect does not exist (e.g. a mod is missing). */
    public MobEffectInstance create() {
        MobEffect e = ForgeRegistries.MOB_EFFECTS.getValue(effect);
        return e == null ? null : new MobEffectInstance(e, ticks, amplifier, false, particles);
    }
}
