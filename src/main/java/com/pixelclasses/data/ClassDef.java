package com.pixelclasses.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** One class (Wizard, Archer, ...) loaded from data/<ns>/pixelclasses/classes/<name>.json */
public record ClassDef(ResourceLocation id, LocalizedText name, LocalizedText description, int color, ResourceLocation icon,
                       LocalizedText manaName, int manaColor, double maxMana, double manaRegen,
                       List<AttributeSpec> attributes, List<ResourceLocation> skillOrder) {

    public static ClassDef parse(ResourceLocation id, JsonObject o) {
        JsonObject mana = o.has("mana") ? GsonHelper.getAsJsonObject(o, "mana") : new JsonObject();
        List<AttributeSpec> attrs = new ArrayList<>();
        for (JsonElement e : JsonUtil.array(o, "attributes")) {
            attrs.add(AttributeSpec.parse(GsonHelper.convertToJsonObject(e, "attribute")));
        }
        List<ResourceLocation> order = new ArrayList<>();
        for (JsonElement e : JsonUtil.array(o, "skills")) {
            order.add(JsonUtil.id(e.getAsString(), "skill"));
        }
        ResourceLocation icon = o.has("icon")
                ? JsonUtil.id(GsonHelper.getAsString(o, "icon"), "icon")
                : new ResourceLocation(id.getNamespace(), "textures/class/" + id.getPath() + ".png");
        return new ClassDef(id,
                LocalizedText.parse(o.get("name"), id.getPath()),
                LocalizedText.parse(o.get("description"), ""),
                JsonUtil.color(o, "color", 0xFFFFFF), icon,
                LocalizedText.parse(mana.get("name"), "Mana"),
                JsonUtil.color(mana, "color", 0x5A8CFF),
                GsonHelper.getAsDouble(mana, "max", 100.0),
                GsonHelper.getAsDouble(mana, "regen", 4.0),
                List.copyOf(attrs), List.copyOf(order));
    }
}
