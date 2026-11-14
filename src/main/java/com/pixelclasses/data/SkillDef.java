package com.pixelclasses.data;

import com.google.gson.JsonObject;
import com.pixelclasses.skill.action.Actions;
import com.pixelclasses.skill.action.SkillAction;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** One skill loaded from data/<ns>/pixelclasses/skills/<name>.json */
public record SkillDef(ResourceLocation id, ResourceLocation classId, LocalizedText name, LocalizedText description,
                       ResourceLocation icon, int cooldownTicks, double manaCost,
                       String scalingStat, double scalingPerPoint, List<SkillAction> actions) {

    public static SkillDef parse(ResourceLocation id, JsonObject o) {
        JsonObject scaling = o.has("scaling") ? GsonHelper.getAsJsonObject(o, "scaling") : new JsonObject();
        ResourceLocation icon = o.has("icon")
                ? JsonUtil.id(GsonHelper.getAsString(o, "icon"), "icon")
                : new ResourceLocation(id.getNamespace(), "textures/skill/" + id.getPath() + ".png");
        return new SkillDef(id,
                JsonUtil.id(GsonHelper.getAsString(o, "class"), "class"),
                LocalizedText.parse(o.get("name"), id.getPath()),
                LocalizedText.parse(o.get("description"), ""),
                icon,
                Math.max(1, (int) Math.round(GsonHelper.getAsDouble(o, "cooldown", 5.0) * 20.0)),
                Math.max(0.0, GsonHelper.getAsDouble(o, "mana_cost", 10.0)),
                GsonHelper.getAsString(scaling, "stat", ""),
                GsonHelper.getAsDouble(scaling, "per_point", 0.0),
                Actions.parseList(JsonUtil.array(o, "actions")));
    }
}
