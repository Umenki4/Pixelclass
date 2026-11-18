package com.pixelclasses.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.network.S2C;
import com.pixelclasses.vfx.VfxSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Runs on the client only (called through DistExecutor so a dedicated server never loads it). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onDefs(List<S2C.Defs.Entry> entries) {
        Map<ResourceLocation, JsonElement> classes = new HashMap<>();
        Map<ResourceLocation, JsonElement> skills = new HashMap<>();
        for (S2C.Defs.Entry e : entries) {
            ResourceLocation id = ResourceLocation.tryParse(e.id);
            if (id == null) {
                continue;
            }
            try {
                JsonElement json = JsonParser.parseString(e.json);
                (e.kind == 0 ? classes : skills).put(id, json);
            } catch (Exception ignored) {
                // a broken entry is skipped
            }
        }
        Definitions.CLIENT.load(classes, skills);
    }

    public static void onPlayer(String classId, String[] equipped, float mana, float maxMana) {
        ClientData.classId = classId;
        String[] eq = {"", "", "", ""};
        for (int i = 0; i < eq.length && i < equipped.length; i++) {
            eq[i] = equipped[i];
        }
        ClientData.equipped = eq;
        ClientData.mana = mana;
        ClientData.maxMana = maxMana;
    }

    public static void onMana(float mana, float maxMana) {
        ClientData.mana = mana;
        ClientData.maxMana = maxMana;
    }

    public static void onCooldown(String skillId, int ticks) {
        ClientData.setCooldown(skillId, ticks);
    }

    public static void onOpen(int kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(kind == 0 ? new ClassSelectScreen() : new SkillScreen());
    }

    public static void onVfx(Vec3 pos, Vec3 dir, List<VfxSpec> specs, int flightId) {
        VfxRenderer.play(pos, dir, specs, flightId);
    }
}
