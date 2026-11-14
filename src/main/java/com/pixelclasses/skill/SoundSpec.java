package com.pixelclasses.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pixelclasses.data.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** {"id": "pixelclasses:boom", "volume": 1.2, "pitch": 1.0} - any registered sound (vanilla or other mods) works. */
public record SoundSpec(ResourceLocation id, float volume, float pitch) {

    public static SoundSpec parse(JsonObject o) {
        return new SoundSpec(JsonUtil.id(GsonHelper.getAsString(o, "id"), "sound"),
                (float) GsonHelper.getAsDouble(o, "volume", 1.0), (float) GsonHelper.getAsDouble(o, "pitch", 1.0));
    }

    public static List<SoundSpec> parseList(JsonElement e) {
        List<SoundSpec> list = new ArrayList<>();
        if (e == null || e.isJsonNull()) {
            return list;
        }
        if (e.isJsonArray()) {
            for (JsonElement el : e.getAsJsonArray()) {
                list.add(parse(GsonHelper.convertToJsonObject(el, "sound")));
            }
        } else {
            list.add(parse(GsonHelper.convertToJsonObject(e, "sound")));
        }
        return List.copyOf(list);
    }

    public void play(ServerLevel level, Vec3 pos) {
        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (event != null) {
            level.playSound(null, pos.x, pos.y, pos.z, event, SoundSource.PLAYERS, volume, pitch);
        }
    }
}
