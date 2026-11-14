package com.pixelclasses.player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Per-player data: chosen class, equipped skills (remembered per class), mana and cooldowns. */
public class ClassData {
    public static final int SLOTS = 4;

    private String classId = "";
    private float mana;
    private final Map<String, String[]> loadouts = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Not saved: last mana value sent to the client. */
    public float lastSentMana = -1.0F;

    public String getClassId() {
        return classId;
    }

    public void setClassId(String id) {
        this.classId = id == null ? "" : id;
    }

    public float getMana() {
        return mana;
    }

    public void setMana(float mana) {
        this.mana = Math.max(0.0F, mana);
    }

    /** The 4 equipped skill ids of a class ("" = empty slot). The returned array can be edited directly. */
    public String[] loadout(String forClass) {
        return loadouts.computeIfAbsent(forClass, k -> {
            String[] a = new String[SLOTS];
            Arrays.fill(a, "");
            return a;
        });
    }

    public long cooldownEnd(String skillId) {
        return cooldowns.getOrDefault(skillId, 0L);
    }

    public void setCooldown(String skillId, long endGameTime) {
        cooldowns.put(skillId, endGameTime);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Class", classId);
        tag.putFloat("Mana", mana);
        CompoundTag lo = new CompoundTag();
        for (Map.Entry<String, String[]> e : loadouts.entrySet()) {
            ListTag list = new ListTag();
            for (String s : e.getValue()) {
                list.add(StringTag.valueOf(s));
            }
            lo.put(e.getKey(), list);
        }
        tag.put("Loadouts", lo);
        CompoundTag cd = new CompoundTag();
        for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
            cd.putLong(e.getKey(), e.getValue());
        }
        tag.put("Cooldowns", cd);
        return tag;
    }

    public void load(CompoundTag tag) {
        classId = tag.getString("Class");
        mana = tag.getFloat("Mana");
        loadouts.clear();
        CompoundTag lo = tag.getCompound("Loadouts");
        for (String key : lo.getAllKeys()) {
            ListTag list = lo.getList(key, Tag.TAG_STRING);
            String[] a = new String[SLOTS];
            Arrays.fill(a, "");
            for (int i = 0; i < SLOTS && i < list.size(); i++) {
                a[i] = list.getString(i);
            }
            loadouts.put(key, a);
        }
        cooldowns.clear();
        CompoundTag cd = tag.getCompound("Cooldowns");
        for (String key : cd.getAllKeys()) {
            cooldowns.put(key, cd.getLong(key));
        }
    }

    public void copyFrom(ClassData other) {
        load(other.save());
    }
}
