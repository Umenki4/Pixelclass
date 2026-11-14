package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.pixelclasses.skill.SkillContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

/** Launches the caster along their facing direction. JSON: strength (horizontal), vertical, backward (true = away). */
public final class DashAction extends SkillAction {
    private double strength;
    private double vertical;
    private boolean backward;

    static DashAction parse(JsonObject o) {
        DashAction a = new DashAction();
        a.strength = GsonHelper.getAsDouble(o, "strength", 1.5);
        a.vertical = GsonHelper.getAsDouble(o, "vertical", 0.1);
        a.backward = GsonHelper.getAsBoolean(o, "backward", false);
        return a;
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        Vec3 d = new Vec3(ctx.dir.x, 0, ctx.dir.z);
        d = d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize();
        if (backward) {
            d = d.scale(-1.0);
        }
        caster.setDeltaMovement(d.x * strength, vertical, d.z * strength);
        caster.hurtMarked = true;
        caster.fallDistance = 0.0F;
    }
}
