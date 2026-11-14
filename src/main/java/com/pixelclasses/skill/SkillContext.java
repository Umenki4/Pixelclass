package com.pixelclasses.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** What an action needs to know: who cast, where, which way, and how strong (power multiplier). */
public final class SkillContext {
    /** Values frozen at cast time and shared by all child contexts. */
    private static final class Shared {
        Vec3 eye;
        Vec3 look;
        final Map<Integer, Vec3> aim = new HashMap<>();
    }

    public final MinecraftServer server;
    public final UUID casterId;
    public final Vec3 origin;
    public final Vec3 dir;
    public final boolean root;
    public final double power;
    private final Shared shared;

    private SkillContext(MinecraftServer server, UUID casterId, Vec3 origin, Vec3 dir, boolean root, double power, Shared shared) {
        this.server = server;
        this.casterId = casterId;
        this.origin = origin;
        this.dir = dir;
        this.root = root;
        this.power = power;
        this.shared = shared;
    }

    public static SkillContext create(ServerPlayer caster, double power) {
        Shared s = new Shared();
        s.eye = caster.getEyePosition();
        s.look = caster.getLookAngle();
        return new SkillContext(caster.server, caster.getUUID(), caster.position(), s.look, true, power, s);
    }

    /** A child context at another position/direction (used for projectile hits, explosions, ...). */
    public SkillContext at(Vec3 newOrigin, Vec3 newDir) {
        return new SkillContext(server, casterId, newOrigin, newDir, false, power, shared);
    }

    /** The caster, or null if they logged out. */
    public ServerPlayer caster() {
        return server.getPlayerList().getPlayer(casterId);
    }

    /** The point the caster was aiming at when the skill started (cached so delayed actions stay on target). */
    public Vec3 aimPoint(ServerPlayer caster, double range) {
        int key = (int) Math.round(range * 10.0);
        Vec3 cached = shared.aim.get(key);
        if (cached != null) {
            return cached;
        }
        Vec3 end = shared.eye.add(shared.look.scale(range));
        BlockHitResult hit = caster.level().clip(new ClipContext(shared.eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 point;
        if (hit.getType() != HitResult.Type.MISS) {
            point = hit.getLocation();
        } else {
            // aiming at the sky: drop the point down onto the ground below
            BlockHitResult ground = caster.level().clip(new ClipContext(end, end.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            point = ground.getType() != HitResult.Type.MISS ? ground.getLocation() : end;
        }
        shared.aim.put(key, point);
        return point;
    }
}
