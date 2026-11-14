package com.pixelclasses.config;

import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Global balance knobs (file: saves/<world>/serverconfig/pixelclasses-server.toml, synced to clients on servers).
 * Per-skill and per-class numbers live in the JSON files instead (see README).
 */
public final class ClassConfig {
    private static final Map<ForgeConfigSpec.ConfigValue<?>, Object> DEFAULTS = new HashMap<>();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue OPEN_SELECTION_ON_JOIN;
    public static final ForgeConfigSpec.BooleanValue ALLOW_CLASS_CHANGE;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_MULT;
    public static final ForgeConfigSpec.DoubleValue MANA_COST_MULT;
    public static final ForgeConfigSpec.DoubleValue MANA_REGEN_MULT;
    public static final ForgeConfigSpec.BooleanValue HURT_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue HURT_PASSIVE_MOBS;
    public static final ForgeConfigSpec.IntValue MAX_TASKS_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_TARGETS_PER_ACTION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        OPEN_SELECTION_ON_JOIN = bool(b, "openSelectionOnFirstJoin", "Open the class selection window for players that have no class yet.", true);
        ALLOW_CLASS_CHANGE = bool(b, "allowClassChange", "If false, a player can only pick a class once (operators can still change it with /pixelclasses set).", true);
        HURT_PLAYERS = bool(b, "skillsCanHurtPlayers", "Skills may damage other players (the normal server PvP rule still applies on top of this).", true);
        HURT_PASSIVE_MOBS = bool(b, "skillsCanHurtPassiveMobs", "Skills may damage animals/villagers. If false only hostile mobs (and mobs targeting you) are hit.", false);
        b.pop();

        b.push("balance");
        DAMAGE_MULT = dbl(b, "skillDamageMultiplier", "Multiplies ALL skill damage and healing.", 1.0, 0.0, 100.0);
        COOLDOWN_MULT = dbl(b, "cooldownMultiplier", "Multiplies ALL skill cooldowns (0.5 = twice as fast).", 1.0, 0.0, 100.0);
        MANA_COST_MULT = dbl(b, "manaCostMultiplier", "Multiplies ALL skill mana costs.", 1.0, 0.0, 100.0);
        MANA_REGEN_MULT = dbl(b, "manaRegenMultiplier", "Multiplies mana regeneration of every class.", 1.0, 0.0, 100.0);
        b.pop();

        b.push("safety");
        MAX_TASKS_PER_PLAYER = intVal(b, "maxScheduledEffectsPerPlayer", "Upper limit of delayed/repeating skill effects one player can have at the same time.", 300, 10, 5000);
        MAX_TARGETS_PER_ACTION = intVal(b, "maxTargetsPerAction", "Upper limit of entities one skill action can hit at once.", 40, 1, 500);
        b.pop();
        SPEC = b.build();
    }

    private ClassConfig() {}

    @SuppressWarnings("unchecked")
    public static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (RuntimeException notLoadedYet) {
            return (T) DEFAULTS.get(value);
        }
    }

    private static ForgeConfigSpec.BooleanValue bool(ForgeConfigSpec.Builder b, String path, String comment, boolean def) {
        ForgeConfigSpec.BooleanValue v = b.comment(comment).define(path, def);
        DEFAULTS.put(v, def);
        return v;
    }

    private static ForgeConfigSpec.DoubleValue dbl(ForgeConfigSpec.Builder b, String path, String comment, double def, double min, double max) {
        ForgeConfigSpec.DoubleValue v = b.comment(comment).defineInRange(path, def, min, max);
        DEFAULTS.put(v, def);
        return v;
    }

    private static ForgeConfigSpec.IntValue intVal(ForgeConfigSpec.Builder b, String path, String comment, int def, int min, int max) {
        ForgeConfigSpec.IntValue v = b.comment(comment).defineInRange(path, def, min, max);
        DEFAULTS.put(v, def);
        return v;
    }
}
