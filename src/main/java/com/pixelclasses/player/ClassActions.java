package com.pixelclasses.player;

import com.pixelclasses.config.ClassConfig;
import com.pixelclasses.data.ClassDef;
import com.pixelclasses.data.Definitions;
import com.pixelclasses.data.SkillDef;
import com.pixelclasses.network.ModNetwork;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server-side rules for choosing a class and equipping skills. Every client request is validated here. */
public final class ClassActions {
    private ClassActions() {}

    /** A player picked a class in the window. */
    public static void requestClass(ServerPlayer player, String id) {
        ClassDef cls = Definitions.SERVER.classDef(ResourceLocation.tryParse(id));
        if (cls == null) {
            return;
        }
        boolean hasClass = ClassEffects.current(player) != null;
        if (hasClass && !ClassConfig.get(ClassConfig.ALLOW_CLASS_CHANGE) && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.pixelclasses.change_locked"), true);
            return;
        }
        setClass(player, cls);
    }

    public static void setClass(ServerPlayer player, ClassDef cls) {
        ClassData data = ClassApi.get(player);
        data.setClassId(cls.id().toString());
        String[] loadout = data.loadout(cls.id().toString());
        boolean empty = true;
        for (String s : loadout) {
            if (!s.isEmpty()) {
                empty = false;
            }
        }
        if (empty) {   // first time with this class: equip the first four skills so the player can play right away
            List<SkillDef> skills = Definitions.SERVER.skillsOf(cls.id());
            for (int i = 0; i < loadout.length && i < skills.size(); i++) {
                loadout[i] = skills.get(i).id().toString();
            }
        }
        data.setMana((float) cls.maxMana());
        ClassEffects.apply(player);
        ModNetwork.sendPlayer(player);
    }

    public static void clearClass(ServerPlayer player) {
        ClassData data = ClassApi.get(player);
        data.setClassId("");
        ClassEffects.apply(player);
        ModNetwork.sendPlayer(player);
    }

    /** skillId "" = empty the slot. */
    public static void equip(ServerPlayer player, int slot, String skillId) {
        ClassDef cls = ClassEffects.current(player);
        if (cls == null || slot < 0 || slot >= ClassData.SLOTS) {
            return;
        }
        String[] loadout = ClassApi.get(player).loadout(cls.id().toString());
        if (skillId.isEmpty()) {
            loadout[slot] = "";
        } else {
            SkillDef skill = Definitions.SERVER.skillDef(ResourceLocation.tryParse(skillId));
            if (skill == null || !skill.classId().equals(cls.id())) {
                return;
            }
            for (int i = 0; i < loadout.length; i++) {
                if (loadout[i].equals(skillId)) {
                    loadout[i] = "";
                }
            }
            loadout[slot] = skillId;
        }
        ModNetwork.sendPlayer(player);
    }
}
