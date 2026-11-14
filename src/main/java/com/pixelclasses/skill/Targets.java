package com.pixelclasses.skill;

import com.pixelclasses.config.ClassConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Who a skill may hit or help ("enemies", "allies", "self", "all") and how damage/knockback are applied. */
public final class Targets {
    private Targets() {}

    public static boolean isEnemy(ServerPlayer caster, LivingEntity e) {
        if (e == caster || e instanceof ArmorStand || e.isSpectator()) {
            return false;
        }
        if (e instanceof TamableAnimal pet && pet.isOwnedBy(caster)) {
            return false;
        }
        if (e.isAlliedTo(caster)) {
            return false;
        }
        if (e instanceof Player) {
            return ClassConfig.get(ClassConfig.HURT_PLAYERS);
        }
        if (ClassConfig.get(ClassConfig.HURT_PASSIVE_MOBS) || e instanceof Enemy) {
            return true;
        }
        return e instanceof Mob mob && mob.getTarget() == caster;
    }

    public static boolean isAlly(ServerPlayer caster, LivingEntity e) {
        if (e == caster) {
            return true;
        }
        if (e instanceof ArmorStand || e.isSpectator()) {
            return false;
        }
        return e instanceof Player || (e instanceof TamableAnimal pet && pet.isOwnedBy(caster)) || e.isAlliedTo(caster);
    }

    public static boolean matches(ServerPlayer caster, LivingEntity e, String affect) {
        if (!e.isAlive()) {
            return false;
        }
        return switch (affect) {
            case "self" -> e == caster;
            case "enemies" -> isEnemy(caster, e);
            case "allies" -> isAlly(caster, e);
            default -> !(e instanceof ArmorStand) && !e.isSpectator();
        };
    }

    public static List<LivingEntity> find(ServerLevel level, ServerPlayer caster, Vec3 pos, double radius, String affect,
                                          Vec3 dir, double coneDegrees, int maxTargets) {
        if (affect.equals("self")) {
            return List.of(caster);
        }
        AABB box = new AABB(pos, pos).inflate(radius + 1.0);
        double cosLimit = Math.cos(Math.toRadians(coneDegrees / 2.0));
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        boolean useCone = coneDegrees > 0 && flat.lengthSqr() > 1.0E-6;
        Vec3 flatDir = useCone ? flat.normalize() : flat;

        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!matches(caster, e, affect)) {
                return false;
            }
            Vec3 center = e.position().add(0, e.getBbHeight() / 2.0, 0);
            if (center.distanceTo(pos) - e.getBbWidth() / 2.0 > radius) {
                return false;
            }
            if (useCone) {
                Vec3 to = new Vec3(e.getX() - pos.x, 0, e.getZ() - pos.z);
                double len = to.length();
                if (len > 0.8 && to.scale(1.0 / len).dot(flatDir) < cosLimit) {
                    return false;
                }
            }
            return true;
        }));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
        int limit = Math.min(maxTargets > 0 ? maxTargets : Integer.MAX_VALUE, ClassConfig.get(ClassConfig.MAX_TARGETS_PER_ACTION));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    /** damageType: "physical" (like a melee hit), "magic", "projectile". Fire is physical + the ignite option. */
    public static void hurt(ServerLevel level, ServerPlayer caster, LivingEntity target, double amount, String damageType) {
        DamageSource source = switch (damageType) {
            case "magic" -> level.damageSources().indirectMagic(caster, caster);
            case "projectile" -> level.damageSources().thrown(caster, caster);
            default -> level.damageSources().playerAttack(caster);
        };
        target.invulnerableTime = 0;   // let rapid multi-hit skills land every hit
        target.hurt(source, (float) amount);
    }

    /** Pushes the target away from {@code from} (negative strength pulls). */
    public static void knock(LivingEntity target, Vec3 from, double strength) {
        double dx = from.x - target.getX();
        double dz = from.z - target.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            dx = (Math.random() - 0.5) * 0.02;
            dz = (Math.random() - 0.5) * 0.02;
        }
        target.knockback(strength, dx, dz);
        target.hurtMarked = true;
    }
}
