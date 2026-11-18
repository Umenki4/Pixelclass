package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pixelclasses.data.EffectSpec;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.SkillRuntime;
import com.pixelclasses.skill.Targets;
import com.pixelclasses.vfx.VfxSpec;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * An invisible projectile made of particles (no entity = no render/crash problems).
 * JSON: speed (blocks/tick), range, hit_radius, pierce (extra entities it passes through), ignore_blocks, gravity,
 * spread (degrees), damage, damage_type, knockback, ignite, effects, affect, trail [..] (particles each tick),
 * on_hit [actions] (at every entity hit), on_end [actions] (where it stops: wall, max range or last hit)
 */
public final class ProjectileAction extends SkillAction {
    private static final Logger LOGGER = LogUtils.getLogger();

    private double speed;
    private double range;
    private double hitRadius;
    private int pierce;
    private boolean ignoreBlocks;
    private double gravity;
    private double spread;
    private double damage;
    private String damageType;
    private double knockback;
    private double ignite;
    private String affect;
    private List<EffectSpec> effects;
    private List<VfxSpec> trail;
    private List<VfxSpec> hitVfx;
    private static final AtomicInteger NEXT_FLIGHT = new AtomicInteger();
    private List<SkillAction> onHit;
    private List<SkillAction> onEnd;

    static ProjectileAction parse(JsonObject o) {
        ProjectileAction a = new ProjectileAction();
        a.speed = Math.max(0.1, Math.min(12.0, GsonHelper.getAsDouble(o, "speed", 1.5)));
        a.range = Math.max(1.0, Math.min(128.0, GsonHelper.getAsDouble(o, "range", 30.0)));
        a.hitRadius = Math.max(0.1, GsonHelper.getAsDouble(o, "hit_radius", 0.8));
        a.pierce = Math.max(0, GsonHelper.getAsInt(o, "pierce", 0));
        a.ignoreBlocks = GsonHelper.getAsBoolean(o, "ignore_blocks", false);
        a.gravity = GsonHelper.getAsDouble(o, "gravity", 0.0);
        a.spread = GsonHelper.getAsDouble(o, "spread", 0.0);
        a.damage = GsonHelper.getAsDouble(o, "damage", 0.0);
        a.damageType = str(o, "damage_type", "physical");
        a.knockback = GsonHelper.getAsDouble(o, "knockback", 0.0);
        a.ignite = GsonHelper.getAsDouble(o, "ignite", 0.0);
        a.affect = str(o, "affect", "enemies");
        a.effects = effects(o);
        a.trail = vfxList(o, "trail");
        a.hitVfx = vfxList(o, "hit_vfx");
        a.onHit = Actions.parseList(com.pixelclasses.data.JsonUtil.array(o, "on_hit"));
        a.onEnd = Actions.parseList(com.pixelclasses.data.JsonUtil.array(o, "on_end"));
        return a;
    }

    @Override
    protected String defaultAt(SkillContext ctx) {
        return ctx.root ? "eye" : "context";
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        Vec3 dir = ctx.dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : ctx.dir.normalize();
        if (spread > 0) {
            dir = jitter(dir, spread, caster.getRandom());
        }
        new Flight(ctx, caster.serverLevel(), ctx.origin, dir.scale(speed)).run();
    }

    private static Vec3 jitter(Vec3 dir, double degrees, RandomSource random) {
        double s = Math.tan(Math.toRadians(degrees));
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        double a = (random(random) * 2 - 1) * s;
        double b = (random(random) * 2 - 1) * s;
        return dir.add(side.scale(a)).add(up.scale(b)).normalize();
    }

    private static double random(RandomSource r) {
        return r.nextDouble();
    }

    /** One flying projectile: moves a step each tick until it hits a wall, runs out of range or its last target. */
    private final class Flight implements Runnable {
        private final SkillContext ctx;
        private final ServerLevel level;
        private final Set<Integer> hitIds = new HashSet<>();
        private Vec3 pos;
        private Vec3 vel;
        private double traveled;
        private int pierceLeft = pierce;
        private final int flightId = NEXT_FLIGHT.incrementAndGet();

        Flight(SkillContext ctx, ServerLevel level, Vec3 start, Vec3 velocity) {
            this.ctx = ctx;
            this.level = level;
            this.pos = start;
            this.vel = velocity;
        }

        @Override
        public void run() {
            try {
                ServerPlayer caster = ctx.caster();
                if (caster == null) {
                    return;
                }
                Vec3 from = pos;
                Vec3 next = from.add(vel);

                if (!ignoreBlocks) {
                    BlockHitResult block = level.clip(new ClipContext(from, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
                    if (block.getType() != HitResult.Type.MISS) {
                        finish(block.getLocation(), caster);
                        return;
                    }
                }

                AABB box = new AABB(from, next).inflate(hitRadius);
                List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> !hitIds.contains(e.getId()) && Targets.matches(caster, e, affect));
                found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
                for (LivingEntity e : found) {
                    AABB body = e.getBoundingBox().inflate(hitRadius);
                    Optional<Vec3> clip = body.clip(from, next);
                    if (clip.isEmpty() && !body.contains(from)) {
                        continue;
                    }
                    Vec3 hitPoint = clip.orElse(from);
                    hitIds.add(e.getId());
                    applyHit(caster, e, hitPoint);
                    if (pierceLeft <= 0) {
                        finish(hitPoint, caster);
                        return;
                    }
                    pierceLeft--;
                }

                traveled += vel.length();
                pos = next;
                vel = vel.add(0, -gravity, 0);
                if (!trail.isEmpty()) {
                    ModNetwork.sendVfx(level, pos, vel, trail, flightId);
                }
                if (traveled >= range) {
                    finish(pos, caster);
                    return;
                }
                SkillRuntime.later(ctx.casterId, 1, this);
            } catch (Exception ex) {
                LOGGER.error("[PixelClasses] Projectile failed: {}", ex.toString());
            }
        }

        private void applyHit(ServerPlayer caster, LivingEntity target, Vec3 hitPoint) {
            if (damage > 0) {
                Targets.hurt(level, caster, target, damage * ctx.power, damageType);
            }
            if (knockback != 0) {
                Targets.knock(target, pos.subtract(vel), knockback);
            }
            if (ignite > 0) {
                target.setSecondsOnFire((int) Math.ceil(ignite));
            }
            for (EffectSpec spec : effects) {
                MobEffectInstance instance = spec.create();
                if (instance != null) {
                    target.addEffect(instance, caster);
                }
            }
            if (!hitVfx.isEmpty()) {
                ModNetwork.sendVfx(level, target.position().add(0, target.getBbHeight() / 2.0, 0), vel, hitVfx);
            }
            Vec3 dir = vel.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : vel.normalize();
            for (SkillAction a : onHit) {
                a.schedule(ctx.at(hitPoint, dir));
            }
        }

        private void finish(Vec3 point, ServerPlayer caster) {
            Vec3 dir = vel.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : vel.normalize();
            for (SkillAction a : onEnd) {
                a.schedule(ctx.at(point, dir));
            }
        }
    }
}
