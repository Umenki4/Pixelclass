package com.pixelclasses.client;

import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.LocalizedText;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Client-side copy of the local player's class state (filled by packets). */
public final class ClientData {
    public static String classId = "";
    public static String[] equipped = {"", "", "", ""};
    public static float mana;
    public static float maxMana;
    public static int tick;

    /** skill id -> {end tick, total ticks} */
    private static final Map<String, int[]> COOLDOWNS = new HashMap<>();

    private ClientData() {}

    public static ClassDef currentClass() {
        return classId.isEmpty() ? null : Definitions.CLIENT.classDef(ResourceLocation.tryParse(classId));
    }

    public static int cooldownLeft(String skillId) {
        int[] c = COOLDOWNS.get(skillId);
        return c == null ? 0 : Math.max(0, c[0] - tick);
    }

    public static int cooldownTotal(String skillId) {
        int[] c = COOLDOWNS.get(skillId);
        return c == null ? 1 : Math.max(1, c[1]);
    }

    /** "*" clears every cooldown. */
    public static void setCooldown(String skillId, int ticks) {
        if ("*".equals(skillId)) {
            COOLDOWNS.clear();
        } else {
            COOLDOWNS.put(skillId, new int[] {tick + ticks, ticks});
        }
    }

    public static void clear() {
        classId = "";
        equipped = new String[] {"", "", "", ""};
        mana = 0.0F;
        maxMana = 0.0F;
        COOLDOWNS.clear();
    }

    public static String text(LocalizedText t) {
        return t.get(Minecraft.getInstance().options.languageCode);
    }
}
