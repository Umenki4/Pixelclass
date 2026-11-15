package com.pixelclasses.skill.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.GsonHelper;

/** Turns the "actions" JSON array into action objects. To add a new action type, add a case here + a class. */
public final class Actions {
    private Actions() {}

    public static SkillAction parse(JsonObject o) {
        String type = GsonHelper.getAsString(o, "type");
        SkillAction action = switch (type) {
            case "area" -> AreaAction.parse(o);
            case "projectile" -> ProjectileAction.parse(o);
            case "dash" -> DashAction.parse(o);
            case "blink" -> BlinkAction.parse(o);
            case "taunt" -> TauntAction.parse(o);
            case "shadow_step" -> ShadowStepAction.parse(o);
            case "vfx" -> new VfxAction();
            default -> throw new JsonSyntaxException("Unknown action type '" + type + "' (area, projectile, dash, blink, shadow_step, taunt, vfx)");
        };
        action.readCommon(o);
        return action;
    }

    public static List<SkillAction> parseList(JsonArray array) {
        List<SkillAction> list = new ArrayList<>();
        for (JsonElement e : array) {
            list.add(parse(GsonHelper.convertToJsonObject(e, "action")));
        }
        return List.copyOf(list);
    }
}
