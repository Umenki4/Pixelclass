package com.pixelclasses.skill.action;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pixelclasses.data.JsonUtil;
import com.pixelclasses.network.ModNetwork;
import com.pixelclasses.skill.SkillContext;
import com.pixelclasses.skill.SkillRuntime;
import com.pixelclasses.skill.SoundSpec;
import com.pixelclasses.vfx.VfxSpec;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * One building block of a skill. Every action type supports these common JSON fields:
 *   delay (ticks), repeat {count, interval}, scatter, y_offset, at (context|caster|eye|aim), aim_range,
 *   direction (look|flat|down|up), vfx [..], sound {..}
 */
public abstract class SkillAction {
    private static final Logger LOGGER = LogUtils.getLogger();

    protected int delay;
    protected int repeatCount = 1;
    protected int repeatInterval = 1;
    protected double scatter;
    protected double yOffset;
    protected double aimRange = 24.0;
    protected String at;
    protected String direction = "look";
    protected List<VfxSpec> vfx = List.of();
    protected List<SoundSpec> sounds = List.of();

    final void readCommon(JsonObject o) {
        delay = Math.max(0, GsonHelper.getAsInt(o, "delay", 0));
        if (o.has("repeat")) {
            JsonObject r = GsonHelper.getAsJsonObject(o, "repeat");
            repeatCount = Math.max(1, Math.min(200, GsonHelper.getAsInt(r, "count", 1)));
            repeatInterval = Math.max(1, GsonHelper.getAsInt(r, "interval", 1));
        }
        scatter = GsonHelper.getAsDouble(o, "scatter", 0.0);
        yOffset = GsonHelper.getAsDouble(o, "y_offset", 0.0);
        aimRange = Math.max(1.0, Math.min(128.0, GsonHelper.getAsDouble(o, "aim_range", 24.0)));
        at = o.has("at") ? GsonHelper.getAsString(o, "at") : null;
        direction = GsonHelper.getAsString(o, "direction", "look");
        vfx = VfxSpec.parseList(o.get("vfx"));
        sounds = SoundSpec.parseList(o.get("sound"));
    }

    /** Where this action happens when "at" is not written in the JSON. */
    protected String defaultAt(SkillContext ctx) {
        return "context";
    }

    protected abstract void execute(SkillContext ctx, ServerPlayer caster);

    /** Starts the action (immediately, or after its delay, repeated if configured). */
    public final void schedule(SkillContext ctx) {
        for (int i = 0; i < repeatCount; i++) {
            int when = delay + i * repeatInterval;
            if (when <= 0) {
                runSafely(ctx);
            } else {
                SkillRuntime.later(ctx.casterId, when, () -> runSafely(ctx));
            }
        }
    }

    private void runSafely(SkillContext ctx) {
        try {
            ServerPlayer caster = ctx.caster();
            if (caster == null || !caster.isAlive()) {
                return;
            }
            SkillContext local = ctx.at(resolvePos(ctx, caster), resolveDir(ctx));
            playEffects(local, caster);
            execute(local, caster);
        } catch (Exception ex) {
            LOGGER.error("[PixelClasses] Action '{}' failed: {}", getClass().getSimpleName(), ex.toString());
        }
    }

    private Vec3 resolvePos(SkillContext ctx, ServerPlayer caster) {
        String mode = at != null ? at : defaultAt(ctx);
        Vec3 p = switch (mode) {
            case "caster" -> caster.position();
            case "eye" -> caster.getEyePosition();
            case "aim" -> ctx.aimPoint(caster, aimRange);
            default -> ctx.origin;
        };
        if (scatter > 0) {
            double angle = caster.getRandom().nextDouble() * Math.PI * 2.0;
            double r = scatter * Math.sqrt(caster.getRandom().nextDouble());
            p = p.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
        }
        return p.add(0, yOffset, 0);
    }

    private Vec3 resolveDir(SkillContext ctx) {
        return switch (direction) {
            case "down" -> new Vec3(0, -1, 0);
            case "up" -> new Vec3(0, 1, 0);
            case "flat" -> {
                Vec3 flat = new Vec3(ctx.dir.x, 0, ctx.dir.z);
                yield flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
            }
            default -> ctx.dir;
        };
    }

    private void playEffects(SkillContext local, ServerPlayer caster) {
        if (!vfx.isEmpty()) {
            ModNetwork.sendVfx(caster.serverLevel(), local.origin, local.dir, vfx);
        }
        for (SoundSpec s : sounds) {
            s.play(caster.serverLevel(), local.origin);
        }
    }

    protected static List<VfxSpec> vfxList(JsonObject o, String key) {
        return VfxSpec.parseList(o.get(key));
    }

    protected static JsonObject object(JsonObject o, String key) {
        return GsonHelper.getAsJsonObject(o, key);
    }

    protected static String str(JsonObject o, String key, String def) {
        return GsonHelper.getAsString(o, key, def);
    }

    static List<com.pixelclasses.data.EffectSpec> effects(JsonObject o) {
        List<com.pixelclasses.data.EffectSpec> list = new java.util.ArrayList<>();
        for (var e : JsonUtil.array(o, "effects")) {
            list.add(com.pixelclasses.data.EffectSpec.parse(GsonHelper.convertToJsonObject(e, "effect")));
        }
        return List.copyOf(list);
    }
}
