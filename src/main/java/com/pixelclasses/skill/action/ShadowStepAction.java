package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.SoundSpec;
import com.pixelclasses.skill.Targets;
import com.pixelclasses.vfx.VfxSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Teleports the caster next to the target the skill locked onto (needs "target" in the skill file), turned to face it.
 * JSON: side ("behind" the target or "front" = on your side of it), behind (distance), range (only used if the skill has
 * no locked target: picks the enemy nearest the crosshair), fallback (no target found: blink forward this far, 0 = stay),
 * path_vfx [..] (drawn as a line from where you left to where you arrive), arrival_vfx [..], arrival_sound {..}
 */
public final class ShadowStepAction extends SkillAction {
    private double range;
    private double behind;
    private double fallback;
    private boolean front;
    private List<VfxSpec> pathVfx;
    private List<VfxSpec> arrivalVfx;
    private List<SoundSpec> arrivalSound;

    static ShadowStepAction parse(JsonObject o) {
        ShadowStepAction a = new ShadowStepAction();
        a.range = Math.max(2.0, Math.min(40.0, GsonHelper.getAsDouble(o, "range", 14.0)));
        a.behind = Math.max(0.8, Math.min(5.0, GsonHelper.getAsDouble(o, "behind", 1.7)));
        a.fallback = Math.max(0.0, Math.min(40.0, GsonHelper.getAsDouble(o, "fallback", 0.0)));
        a.front = "front".equals(GsonHelper.getAsString(o, "side", "behind"));
        a.pathVfx = vfxList(o, "path_vfx");
        a.arrivalVfx = vfxList(o, "arrival_vfx");
        a.arrivalSound = SoundSpec.parseList(o.get("arrival_sound"));
        return a;
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        Vec3 from = caster.position();
        Vec3 arrived = null;
        if (ctx.hasTarget()) {
            LivingEntity locked = ctx.target(level);
            if (locked != null) {
                arrived = stepNextTo(level, caster, locked);
            }
        } else {
            LivingEntity target = pickTarget(level, caster);
            if (target != null) {
                arrived = stepNextTo(level, caster, target);
            } else if (fallback > 0) {
                arrived = BlinkAction.teleportAlongLook(caster, fallback);
            }
        }
        if (arrived == null) {
            return;
        }
        Vec3 delta = arrived.subtract(from);
        double len = delta.length();
        if (!pathVfx.isEmpty() && len > 0.5) {
            List<VfxSpec> line = new ArrayList<>();
            for (VfxSpec v : pathVfx) {
                line.add(v.withLength((float) len));
            }
            ModNetwork.sendVfx(level, from, delta, line);
        }
        if (!arrivalVfx.isEmpty()) {
            ModNetwork.sendVfx(level, arrived.add(0, 0.2, 0), caster.getLookAngle(), arrivalVfx);
        }
        for (SoundSpec s : arrivalSound) {
            s.play(level, arrived);
        }
    }

    /** The enemy closest to the crosshair (and not behind the caster). */
    private LivingEntity pickTarget(ServerLevel level, ServerPlayer caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 eye = caster.getEyePosition();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : Targets.find(level, caster, caster.position(), range, "enemies", look, 0.0, 0)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(eye);
            double dist = to.length();
            if (dist < 0.5) {
                continue;
            }
            double dot = to.scale(1.0 / dist).dot(look);
            if (dot < 0.0) {
                continue;
            }
            double score = dist * (1.6 - dot);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private Vec3 stepNextTo(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        Vec3 start = caster.position();
        Vec3 forward;
        if (front) {
            Vec3 toTarget = new Vec3(target.getX() - start.x, 0, target.getZ() - start.z);
            forward = toTarget.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, target.getYRot()) : toTarget.normalize();
        } else {
            forward = Vec3.directionFromRotation(0.0F, target.getYRot());
        }
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        double[] distances = {behind, behind * 0.6, behind * 1.4};
        double[] sides = {0.0, 1.0, -1.0};
        for (double dist : distances) {
            for (double sideOffset : sides) {
                Vec3 spot = target.position().subtract(forward.scale(dist)).add(side.scale(sideOffset));
                AABB moved = caster.getBoundingBox().move(spot.subtract(start));
                if (level.noCollision(caster, moved)) {
                    float yaw = (float) (Mth.atan2(target.getZ() - spot.z, target.getX() - spot.x) * (180.0 / Math.PI)) - 90.0F;
                    caster.connection.teleport(spot.x, spot.y, spot.z, yaw, 0.0F);
                    caster.fallDistance = 0.0F;
                    caster.setDeltaMovement(Vec3.ZERO);
                    return spot;
                }
            }
        }
        return null;
    }
}
