package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.Targets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Forces nearby hostile mobs to target the caster. JSON: radius */
public final class TauntAction extends SkillAction {
    private double radius;

    static TauntAction parse(JsonObject o) {
        TauntAction a = new TauntAction();
        a.radius = GsonHelper.getAsDouble(o, "radius", 8.0);
        return a;
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        AABB box = new AABB(ctx.origin, ctx.origin).inflate(radius);
        for (Mob mob : caster.serverLevel().getEntitiesOfClass(Mob.class, box,
                m -> m.isAlive() && m.distanceToSqr(ctx.origin) <= radius * radius && Targets.matches(caster, m, "enemies"))) {
            mob.setTarget(caster);
        }
    }
}
