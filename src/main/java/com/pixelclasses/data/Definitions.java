package com.pixelclasses.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

/**
 * Holds every loaded class and skill. SERVER is filled by the datapack reload listener,
 * CLIENT is filled from the sync packet (so the UI shows the server's numbers).
 * A broken JSON file is logged and skipped - it can never crash the game.
 */
public final class Definitions {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Definitions SERVER = new Definitions();
    public static final Definitions CLIENT = new Definitions();

    private volatile Map<ResourceLocation, ClassDef> classes = Map.of();
    private volatile Map<ResourceLocation, SkillDef> skills = Map.of();
    private volatile Map<ResourceLocation, String> rawClasses = Map.of();
    private volatile Map<ResourceLocation, String> rawSkills = Map.of();

    public void load(Map<ResourceLocation, JsonElement> classJson, Map<ResourceLocation, JsonElement> skillJson) {
        Map<ResourceLocation, ClassDef> newClasses = new LinkedHashMap<>();
        Map<ResourceLocation, String> newRawClasses = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : classJson.entrySet()) {
            try {
                JsonObject obj = GsonHelper.convertToJsonObject(e.getValue(), "class");
                newClasses.put(e.getKey(), ClassDef.parse(e.getKey(), obj));
                newRawClasses.put(e.getKey(), obj.toString());
            } catch (Exception ex) {
                LOGGER.error("[PixelClasses] Skipping broken class file '{}': {}", e.getKey(), ex.toString());
            }
        }
        Map<ResourceLocation, SkillDef> newSkills = new LinkedHashMap<>();
        Map<ResourceLocation, String> newRawSkills = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : skillJson.entrySet()) {
            try {
                JsonObject obj = GsonHelper.convertToJsonObject(e.getValue(), "skill");
                newSkills.put(e.getKey(), SkillDef.parse(e.getKey(), obj));
                newRawSkills.put(e.getKey(), obj.toString());
            } catch (Exception ex) {
                LOGGER.error("[PixelClasses] Skipping broken skill file '{}': {}", e.getKey(), ex.toString());
            }
        }
        this.classes = newClasses;
        this.skills = newSkills;
        this.rawClasses = newRawClasses;
        this.rawSkills = newRawSkills;
        LOGGER.info("[PixelClasses] Loaded {} classes and {} skills", newClasses.size(), newSkills.size());
    }

    public ClassDef classDef(ResourceLocation id) {
        return id == null ? null : classes.get(id);
    }

    public SkillDef skillDef(ResourceLocation id) {
        return id == null ? null : skills.get(id);
    }

    /** All classes, sorted by their English name. */
    public List<ClassDef> classes() {
        List<ClassDef> list = new ArrayList<>(classes.values());
        list.sort(Comparator.comparing((ClassDef c) -> c.name().english()).thenComparing(c -> c.id().toString()));
        return list;
    }

    /** Skills of one class: the order written in the class file first, then any other skill that names this class. */
    public List<SkillDef> skillsOf(ResourceLocation classId) {
        ClassDef cls = classDef(classId);
        List<SkillDef> result = new ArrayList<>();
        if (cls != null) {
            for (ResourceLocation id : cls.skillOrder()) {
                SkillDef s = skills.get(id);
                if (s != null && s.classId().equals(classId) && !result.contains(s)) {
                    result.add(s);
                }
            }
        }
        List<SkillDef> rest = new ArrayList<>();
        for (SkillDef s : skills.values()) {
            if (s.classId().equals(classId) && !result.contains(s)) {
                rest.add(s);
            }
        }
        rest.sort(Comparator.comparing(s -> s.id().toString()));
        result.addAll(rest);
        return result;
    }

    public Collection<ResourceLocation> classIds() {
        return classes.keySet();
    }

    public Collection<ResourceLocation> skillIds() {
        return skills.keySet();
    }

    public Map<ResourceLocation, String> rawClasses() {
        return rawClasses;
    }

    public Map<ResourceLocation, String> rawSkills() {
        return rawSkills;
    }
}
