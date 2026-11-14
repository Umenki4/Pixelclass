package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.pixelclasses.data.EffectSpec;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.Targets;
import com.pixelclasses.vfx.VfxSpec;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Hurts / heals / buffs / debuffs everything in a radius around the action position.
 * JSON: radius, damage, damage_type (physical|magic|projectile), knockback (negative pulls), ignite (seconds),
 * heal, effects [..], affect (enemies|allies|self|all), cone (degrees, 0 = full circle), max_targets, hit_vfx [..]
 */
public final class AreaAction extends SkillAction {
    private double radius;
    private double damage;
    private String damageType;
    private double knockback;
    private double ignite;
    private double heal;
    private double cone;
    private int maxTargets;
    private String affect;
    private List<EffectSpec> effects;
    private List<VfxSpec> hitVfx;

    static AreaAction parse(JsonObject o) {
        AreaAction a = new AreaAction();
        a.radius = GsonHelper.getAsDouble(o, "radius", 3.0);
        a.damage = GsonHelper.getAsDouble(o, "damage", 0.0);
        a.damageType = str(o, "damage_type", "physical");
        a.knockback = GsonHelper.getAsDouble(o, "knockback", 0.0);
        a.ignite = GsonHelper.getAsDouble(o, "ignite", 0.0);
        a.heal = GsonHelper.getAsDouble(o, "heal", 0.0);
        a.cone = GsonHelper.getAsDouble(o, "cone", 0.0);
        a.maxTargets = GsonHelper.getAsInt(o, "max_targets", 0);
        a.affect = o.has("affect") ? str(o, "affect", "enemies") : null;
        a.effects = effects(o);
        a.hitVfx = vfxList(o, "hit_vfx");
        return a;
    }

    @Override
    protected void execute(SkillContext ctx, ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        String who = affect != null ? affect : (damage > 0 ? "enemies" : "allies");
        boolean hostile = who.equals("enemies") || who.equals("all");
        List<LivingEntity> targets = Targets.find(level, caster, ctx.origin, radius, who, ctx.dir, cone, maxTargets);
        for (LivingEntity target : targets) {
            if (damage > 0) {
                Targets.hurt(level, caster, target, damage * ctx.power, damageType);
            }
            if (knockback != 0 && hostile && target != caster) {
                Targets.knock(target, ctx.origin, knockback);
            }
            if (ignite > 0 && hostile && target != caster) {
                target.setSecondsOnFire((int) Math.ceil(ignite));
            }
            if (heal > 0) {
                target.heal((float) (heal * ctx.power));
            }
            for (EffectSpec spec : effects) {
                MobEffectInstance instance = spec.create();
                if (instance != null) {
                    target.addEffect(instance, caster);
                }
            }
            if (!hitVfx.isEmpty()) {
                Vec3 p = target.position().add(0, target.getBbHeight() / 2.0, 0);
                ModNetwork.sendVfx(level, p, ctx.dir, hitVfx);
            }
        }
    }
}
