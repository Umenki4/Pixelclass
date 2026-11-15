package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.SoundSpec;
import com.pixelclasses.vfx.VfxSpec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Teleports the caster forward (stops before walls). JSON: distance, arrival_vfx [..], arrival_sound {..} */
public final class BlinkAction extends SkillAction {
    private double distance;
    private List<VfxSpec> arrivalVfx;
    private List<SoundSpec> arrivalSound;

    static BlinkAction parse(JsonObject o) {
        BlinkAction a = new BlinkAction();
        a.distance = Math.max(1.0, Math.min(64.0, GsonHelper.getAsDouble(o, "distance", 10.0)));
        a.arrivalVfx = vfxList(o, "arrival_vfx");
        a.arrivalSound = SoundSpec.parseList(o.get("arrival_sound"));
        return a;
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        Vec3 arrived = teleportAlongLook(caster, distance);
        if (arrived == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        if (!arrivalVfx.isEmpty()) {
            ModNetwork.sendVfx(level, arrived.add(0, 0.2, 0), caster.getLookAngle(), arrivalVfx);
        }
        for (SoundSpec s : arrivalSound) {
            s.play(level, arrived);
        }
    }

    /** Teleports the caster up to {@code distance} blocks along their look direction. Returns the new position or null. */
    static Vec3 teleportAlongLook(ServerPlayer caster, double distance) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle();
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(look.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 reach = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.5));
        Vec3 feet = reach.subtract(0, caster.getEyeHeight(), 0);
        Vec3 start = caster.position();

        boolean ok = false;
        for (int i = 0; i < 14; i++) {
            AABB moved = caster.getBoundingBox().move(feet.subtract(start));
            if (level.noCollision(caster, moved)) {
                ok = true;
                break;
            }
            Vec3 back = start.subtract(feet);
            feet = back.lengthSqr() < 0.25 ? start : feet.add(back.normalize().scale(0.4));
        }
        if (!ok || feet.distanceToSqr(start) < 0.25) {
            return null;
        }
        caster.connection.teleport(feet.x, feet.y, feet.z, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        caster.setDeltaMovement(Vec3.ZERO);
        return feet;
    }
}
